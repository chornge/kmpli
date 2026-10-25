# Cross-compiling a Kotlin/Native CLI to Linux ARM64

[kmpli](https://github.com/chornge/kmpli) is a Kotlin/Native CLI that ships prebuilt binaries for
macOS, Linux and Windows. Adding `linux_arm64` looked like a one-line change to the build file. It
took six CI rounds and turned up five separate root causes, none of which are documented anywhere
obvious.

This writes them all down.

> **Status note.** This describes the ktor 3.0.1 era, when ktor's `linux_arm64` klib bundled a
> static `libcurl.a` built with SSH, LDAP and RTMP support and nothing else. Layers 3 and 5, the
> `pkg-config --libs --static libcurl` closure and the static-`libgcc.a` hack, have since been
> removed from the build. ktor 3.3.3 bundles libcurl, OpenSSL and their closure, and declares
> `linkerOpts.linux=-lz` in its own klib manifest, so the cross-link now needs nothing but
> `zlib1g-dev:arm64`. The debugging narrative is kept because the techniques still apply, but do
> not copy the build snippets as current configuration. One conclusion below turned out to be
> wrong in a way that shipped a broken binary; see the end of "One last check worth doing".

## The thing that stops you immediately

The obvious plan is to build aarch64 on an aarch64 machine. GitHub even provides free arm64 Linux
runners now (`ubuntu-24.04-arm`), so this should be a matrix entry and nothing more.

It is not, because **Kotlin/Native publishes no `linux-aarch64` host compiler**. Every host
distribution on Maven Central, for 2.2.20 through the current 2.4.x:

```
kotlin-native-prebuilt-<ver>-linux-x86_64.tar.gz
kotlin-native-prebuilt-<ver>-macos-aarch64.tar.gz
kotlin-native-prebuilt-<ver>-macos-x86_64.tar.gz
kotlin-native-prebuilt-<ver>-windows-x86_64.zip
```

There is no `linux-aarch64`. An arm64 Linux runner can run a Kotlin/Native binary but cannot build
one, and upgrading Kotlin does not change that.

So aarch64 has to be cross-compiled from x86_64.

## The good news, buried in konan.properties

Cross-compiling to `linux_arm64` is a first-class, supported combination. It is right there in the
compiler's own `konan.properties`:

```properties
targetToolchain.linux_x64-linux_arm64 = $gccToolchain.linux_arm64/aarch64-unknown-linux-gnu
toolchainDependency.linux_arm64 = aarch64-unknown-linux-gnu-gcc-8.3.0-glibc-2.25-kernel-4.9-2
```

Declare the target and Gradle downloads the aarch64 cross-toolchain and sysroot automatically. No
manual toolchain setup at all. Compilation worked on the very first attempt.

Only the link step failed. Repeatedly. Note those two version numbers though, **GCC 8.3.0** and
**glibc 2.25**, because they are layers 4 and 5.

## Layer 1: arm64 packages live on a different host

The project uses `ktor-client-curl`, so the link needs libcurl built for aarch64. On Ubuntu that
means dpkg multiarch, plus a detail that bites later: Ubuntu serves arm64 from `ports.ubuntu.com`,
not `archive.ubuntu.com`. Adding the architecture without adding those sources makes `apt-get
update` fail with 404s.

The subtle part is that existing sources must first be pinned to amd64, or apt tries to fetch
arm64 indexes from the amd64 mirror:

```bash
sudo sed -i 's/^Types: deb$/Types: deb\nArchitectures: amd64/' /etc/apt/sources.list.d/ubuntu.sources
sudo dpkg --add-architecture arm64
```

Then add the arm64 sources. **Mirror every suite the amd64 side has**, not just `main` and
`-updates`. Packages like `libcurl4t64` are `Multi-Arch: same`, which requires identical versions
across architectures. This bit me weeks later, when `noble-security` published
`8.5.0-2ubuntu10.16` while my sources only reached `noble-updates` at `10.15`:

```
libcurl4-openssl-dev:arm64 : Depends: libcurl4t64:arm64 (= 8.5.0-2ubuntu10.15)
                             but it is not going to be installed
```

Any suite visible to amd64 but not to arm64 guarantees eventual drift.

## Layer 2: dpkg refuses the dev package

```
dpkg: error processing archive libcurl4-openssl-dev_..._arm64.deb (--unpack):
 trying to overwrite shared '/usr/bin/curl-config'
```

Both architectures' dev packages ship `/usr/bin/curl-config`, with per-architecture contents, so
dpkg will not install the second one. Since the job only ever builds aarch64, letting the arm64
copy win is fine:

```bash
sudo apt-get install -y -o Dpkg::Options::="--force-overwrite" libcurl4-openssl-dev:arm64 ...
```

Install the `-dev` packages, not only the runtime ones. The linker resolves `-lfoo` against the
unversioned `libfoo.so` symlink, which only `-dev` provides. Runtime packages ship just
`libfoo.so.N`.

## Layer 3: ktor force-includes a static libcurl

With libcurl present, the link still failed, on symbols that have nothing to do with HTTP:

```
ld.lld: error: undefined symbol: ssh_scp_free
>>> referenced by libcurl_la-libssh.o:(myssh_statemach_act) in archive /tmp/included.../libcurl.a

ld.lld: error: undefined symbol: DES_set_odd_parity
>>> referenced by libcurl_la-curl_ntlm_core.o:(setup_des_key) in archive /tmp/included.../libcurl.a
```

Read the `in archive` part. ktor's cinterop klib ships a **prebuilt static `libcurl.a`** and hands
it to the linker as an explicit input, ahead of anything you pass. Archive members are pulled to
resolve undefined symbols, so the static copy wins over the shared library every time.

A static libcurl needs its entire dependency closure named on the link line, and Ubuntu builds
libcurl with SFTP, RTMP, LDAP, GSSAPI and NTLM support compiled in. Rather than guess that list,
let pkg-config state it:

```bash
export PKG_CONFIG_LIBDIR=/usr/lib/aarch64-linux-gnu/pkgconfig:/usr/share/pkgconfig
pkg-config --libs --static libcurl
```

Use `PKG_CONFIG_LIBDIR`, not only `PKG_CONFIG_PATH`. Otherwise the amd64 `.pc` files remain on the
search path and you can silently get an x86_64 link closure.

## Layer 4: glibc 2.25 against glibc 2.39

```
ld.lld: error: undefined reference: pthread_detach@GLIBC_2.34
>>> referenced by /usr/lib/aarch64-linux-gnu/libcurl.so (disallowed by --no-allow-shlib-undefined)
ld.lld: error: undefined reference: stat@GLIBC_2.33
>>> referenced by /usr/lib/aarch64-linux-gnu/libssl.so (disallowed by --no-allow-shlib-undefined)
```

Kotlin/Native's aarch64 sysroot is **glibc 2.25**, deliberately old so output runs on older
distributions. Ubuntu 24.04's arm64 libraries are built against **glibc 2.39** and reference
versioned symbols the sysroot does not contain. By default the linker verifies a shared library's
own undefined symbols and refuses.

```kotlin
linkerOpts("--allow-shlib-undefined")
```

This is a real suppression, not a tidy fix. Those symbols are resolved by the actual glibc on the
user's machine at runtime, which is exactly why the only meaningful validation is running the
binary on real hardware.

Worth noting the asymmetry: `linux_x64` never hits this, because on a matching host the linker
finds the system glibc rather than the sysroot's.

## Layer 5: outline atomics, and a library that cannot possibly help

One symbol left:

```
ld.lld: error: undefined symbol: __aarch64_swp4_acq
>>> referenced by libcurl_la-easy.o:(curl_global_init) in archive /tmp/included.../libcurl.a
```

`__aarch64_swp4_acq` is a GCC **outline atomics** helper. Ubuntu's libcurl was built by GCC 14
with them enabled. Kotlin's aarch64 toolchain is **GCC 8.3.0**, which predates the feature
entirely.

The obvious fix, `-lgcc_s`, does nothing. Unpacking the actual `.deb` files shows why:

```
libgcc_s.so.1   ->   0 __aarch64_* symbols
libgcc.a        ->  40 __aarch64_swp* symbols, including __aarch64_swp4_acq
```

Outline atomics live only in the **static** `libgcc.a`. The shared library exports none of them,
so `-lgcc_s` could never have worked.

So install `libgcc-14-dev:arm64` and use `-lgcc`. That also fails, identically, and this part cost
the most time: **Kotlin ships its own `libgcc.a`**, from GCC 8.3.0, and its directory sits ahead
of yours on the search path.

```
~/.konan/dependencies/aarch64-unknown-linux-gnu-gcc-8.3.0-.../lib/gcc/.../8.3.0/libgcc.a
  __aarch64_swp4_acq occurrences: 0
```

`-lgcc` silently resolved to the wrong archive. The fix is to pass Ubuntu's archive by **absolute
path**, which bypasses `-l` resolution entirely:

```bash
libgcc_a=$(find /usr/lib/gcc/aarch64-linux-gnu -name libgcc.a | head -1)
strings "$libgcc_a" | grep -q '__aarch64_swp4_acq' \
  || { echo "::error::$libgcc_a has no outline-atomics helpers"; exit 1; }
opts="$opts $libgcc_a"
```

That guard matters. Two separate attempts failed with an identical error message while pointing at
archives that did not contain the symbol. Asserting on the archive turns a confusing linker error
into a clear one.

## Validating it, which a green build does not do

Layer 4 suppresses a genuine mismatch, so a successful link proves very little. The binary has to
actually run on aarch64.

The runners that cannot build can still run. So: cross-build on `ubuntu-24.04`, upload the
artifact, download it on `ubuntu-24.04-arm`, and exercise it for real.

```
+ uname -m
aarch64
+ ./kmpli.kexe --version
kmpli v1.5.0
+ ./kmpli.kexe --name=SmokeTest --pid=org.smoke.test --template=library
Downloading from: https://github.com/Kotlin/multiplatform-library-template/...
Downloaded 266222 bytes
Project generation complete!
```

A live HTTPS download through the libcurl we fought with, on real ARM hardware. That is the
evidence `--allow-shlib-undefined` was benign.

## One last check worth doing

The aarch64 binary needs 24 shared libraries where x64 needs 11, because `pkg-config --static` put
libcurl's private dependencies on our link line as direct `DT_NEEDED` entries rather than leaving
them to `libcurl.so.4`.

That looked like a problem until I read libcurl's own packaging:

```
libcurl4t64 Depends: libbrotli1, libgssapi-krb5-2, libidn2-0, libldap2, libnghttp2-14,
                     libpsl5t64, librtmp1, libssh-4, libssl3t64, libzstd1, zlib1g
```

Every extra library is already a dependency of libcurl itself. Anything with libcurl installed has
all of them. The difference is cosmetic.

**That conclusion was wrong, and it is why a broken binary shipped.** The `Depends` line above is
Ubuntu's `libcurl4t64`. Debian builds libcurl against **libssh2** instead, so a Debian aarch64
system with libcurl fully installed still has no `libssh.so.4`, and the binary does not start:

```
kmpli: error while loading shared libraries: libssh.so.4: cannot open shared object file
```

Nothing caught it, because the only machine that ever ran the aarch64 binary was GitHub's
`ubuntu-24.04-arm` runner, which has Ubuntu's closure preinstalled.

Two lessons. Borrowing a distribution's dependency list only tells you about that distribution.
And "it runs on the CI runner" is a weaker claim than "it runs on a clean system", because runner
images are too well equipped to prove anything about a user's machine. The check that would have
caught this is running the binary inside `debian:stable-slim` with only the documented
prerequisites installed, which `build.yml` now does for both Linux targets.

## Takeaways

- **Kotlin/Native has no linux-aarch64 host.** Cross-compile from x86_64, and no Kotlin upgrade
  changes that.
- **`konan.properties` is the map.** `targetToolchain`, `toolchainDependency` and
  `linkerKonanFlags` tell you exactly what the compiler will and will not do for you.
- **Check which file a `-l` flag actually resolved to.** Two of the five layers were the linker
  quietly choosing a different library than intended. An absolute path plus an assertion on the
  archive's contents is worth more than another flag.
- **Inspect the `.deb`.** `libgcc_s.so.1` versus `libgcc.a` was only answerable by unpacking them
  and counting symbols. Reasoning about it would have stayed wrong indefinitely.
- **Cross-compiled output needs an execution test, not a build test.** The whole thing rests on a
  suppressed glibc mismatch. Only running it on the real architecture tells you anything.
