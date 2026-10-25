![CI/CD](https://github.com/chornge/kmpli/actions/workflows/build.yml/badge.svg?branch=main)
![Downloads](https://img.shields.io/github/downloads/chornge/kmpli/total)

# Kmpli

Pronounced `"Comply"`

Generate Kotlin and Compose Multiplatform projects from your terminal.

A single native binary: no IDE, no browser, no JVM.

<p align="center">
  <img src="docs/demo.gif" alt="The kmpli interactive wizard generating a project" width="813">
</p>

### Why a CLI

[kmp.jetbrains.com](https://kmp.jetbrains.com) is the usual way to start a Kotlin Multiplatform
project, and it works well for a person clicking through it once. kmpli covers what a web form
cannot:

- **Scripting and CI.** Generate a project inside a pipeline, a Makefile, or a setup script.
- **Reproducible scaffolding.** The same flags produce the same project every time, so a team can
  commit the command instead of documenting a sequence of clicks.
- **Staying in the terminal.** An interactive wizard is also available.

### Quick Start

kmpli needs curl and its SSL libraries at runtime. Most systems already have them; see
[Prerequisites](#installation) if the steps below fail with a TLS error.

```bash
# macOS/Linux
curl -fsSL https://raw.githubusercontent.com/chornge/kmpli/main/install.sh | bash

kmpli
```

Running `kmpli` with no arguments opens the wizard shown above. Or skip it entirely:

```bash
kmpli --name="MyApp" --pid="com.acme.myapp" --platforms="android,ios(swiftui),desktop"
kmpli --name="MyLib" --pid="com.acme.mylib" --template="library"
```

### Supported Platforms

| OS | Architecture | Status |
| --- | --- | --- |
| macOS | arm64 (Apple Silicon) | Supported |
| macOS | x86_64 (Intel) | Supported |
| Linux | x86_64 | Supported |
| Linux | aarch64 / arm64 | Supported |
| Windows | x86_64 | Supported |

Each release publishes a binary for every supported row. The Linux aarch64 binary is cross-compiled
from an x86_64 host, because Kotlin/Native publishes no linux-aarch64 host compiler.

### Installation

**Prerequisites (Required)**

kmpli links curl and its TLS library statically, so neither has to be installed to run it. What
it does need is the pieces it reaches for at runtime:

- **A certificate store on Linux.** `ca-certificates` provides it. Without one, every download
  fails verification, because the bundled TLS library has no trust store of its own. macOS and
  Windows read theirs from the OS and need nothing.
- **`unzip` on macOS and Linux**, to unpack the generated project. macOS ships it and most Linux
  installs have it, but minimal container images usually do not. Windows uses the bundled
  `tar.exe`, or PowerShell as a fallback.
- **`curl`, for the installer only.** The launcher script downloads the binary with it. macOS and
  Windows 10+ ship curl already.

```bash
# macOS - nothing required; curl and unzip are already present

# Linux (Debian/Ubuntu)
sudo apt-get install -y ca-certificates unzip curl

# Windows - nothing required; curl ships with Windows 10 and later
```

**macOS/Linux (Homebrew):**

```bash
brew tap chornge/kmpli https://github.com/chornge/kmpli
brew install kmpli
```

**Windows (Scoop):**

```powershell
scoop bucket add kmpli https://github.com/chornge/kmpli
scoop install kmpli
```

**Quick Install (curl):**

```bash
# macOS/Linux
curl -fsSL https://raw.githubusercontent.com/chornge/kmpli/main/install.sh | bash

# Windows (PowerShell)
irm https://raw.githubusercontent.com/chornge/kmpli/main/install.ps1 | iex
```

**Alternative Install (git):**

Clone the repository and run the installer:

```bash
git clone https://github.com/chornge/kmpli
cd kmpli
```

macOS/Linux:

```bash
bash install.sh
```

Windows (PowerShell):

```powershell
powershell -ExecutionPolicy Bypass -File install.ps1
```

The installer sets up a launcher that automatically downloads the appropriate binary for your platform on first run. No
build required!

### Uninstalling

From a clone of this repository:

```bash
make uninstall
```

That removes kmpli however it was installed: the Homebrew formula or Scoop package if either is
present, the binary in the install directory, and the launcher and cached binaries under `~/.kmpli`.
On Windows it also removes the `%USERPROFILE%\.kmpli` entry that `install.ps1` added to your PATH.

Or remove it by hand, using whichever applies:

```bash
brew uninstall kmpli          # Homebrew
scoop uninstall kmpli         # Scoop
rm -f /usr/local/bin/kmpli    # curl or git installer (macOS/Linux)
rm -rf ~/.kmpli               # launcher and cached binaries
```

### Building from Source (Optional)

If you prefer to build from source or need to contribute:

**Requirements:** JDK-17+, Git, and the platform-specific dependencies below. Gradle itself comes
from the wrapper checked into this repository, so there is no need to install it.

- **macOS**: nothing beyond the above. TLS resolves against `Security.framework` from the SDK.
- **Linux**: `sudo apt-get install -y zlib1g-dev`, which supplies the `libz.so` symlink the link
  step resolves `-lz` against. libcurl and OpenSSL are no longer needed: ktor's klib bundles
  prebuilt static copies and the build no longer links the system ones.
- **Windows**: `choco install -y curl openssl.light`

**Build:**

```bash
chmod +x gradlew
./gradlew build
```

A `Makefile` wraps the common tasks for whichever platform you are on. Run `make help` to list them:

| Target | What it does |
| --- | --- |
| `make build` | Assemble and check the host target, running its tests too |
| `make test` | Run the unit tests (`./gradlew allTests`, exactly as CI does) |
| `make lint` | Run detekt static analysis |
| `make release` | Link the optimized release binary |
| `make install` | Build the release binary and copy it to the install directory |
| `make uninstall` | Remove kmpli installed by any supported method |
| `make clean` | Delete Gradle build outputs |
| `make sanity` | Run clean, lint, test, then build, in that order |

Then run the installer as shown above. The launcher prefers a locally built binary over downloading
one, but it looks for that binary at a path relative to the current directory, so you only get the
local build when you run `kmpli` from the root of this repository.

### Usage

#### Interactive Mode (Recommended)

Simply run `kmpli` with no arguments to launch the interactive wizard:

```bash
kmpli
```

The wizard guides you through project setup with:

- Template or custom platform selection
- Project name and package ID configuration
- Test inclusion options
- Summary and confirmation before generation

On supported terminals, use arrow keys to navigate. Otherwise, enter numbered choices.

#### Command-Line Mode

For scripting or CI/CD pipelines, use command-line flags:

```bash
kmpli --name="CMPProject" --pid="org.cmp.project"
```

#### Usage Options

```
  --name TEXT       Project name. Optional, defaults to "CMPProject".
                    1 to 50 characters of Latin letters, digits, spaces,
                    underscores and hyphens, starting with a letter or digit.
  --pid TEXT        Package ID. Optional, defaults to "org.cmp.<name>" using the
                    name lowercased with everything but letters and digits
                    removed (or "project" if that result is empty or begins
                    with a digit).
                    Must be dot-separated lowercase segments of letters, digits
                    and '_', each starting with a letter, at most 100 chars.
  --template TEXT   Multiplatform template
                    shared-ui -> Shared UI App (Compose)
                    native-ui -> Native UI App (Compose + SwiftUI)
                    library -> Bare-bones Multiplatform Library
                    shared-ui-toolchain -> Shared UI App (Kotlin Toolchain)
                    native-ui-toolchain -> Native UI App (Kotlin Toolchain)
  --platforms TEXT  Target platform(s). Optional, defaults to
                    "android(compose),ios(compose)". Comma-separated, each entry
                    written as NAME or NAME(UI):
                      android   compose (default)
                      ios       compose (default), swiftui
                      desktop   compose (default)
                      web       compose (default), react
                      server    always uses the ktor engine; any UI is ignored
  --build-system    Build system for --platforms generation.
                    gradle (default) or kotlin for the Kotlin Toolchain.
                    Ignored with --template, since each template id already
                    names its build system.
  --include-tests   Include sample tests (optional)
  --help, -h        Show this help message
  --version, -v     Show the installed kmpli version
```

Values may be given as either `--flag=value` or `--flag value`. Flag names must match exactly, so a
typo such as `--nameXYZ=Foo` is reported rather than silently accepted.

**Exit codes:** `0` on success, `1` on invalid input or a failed generation. Worth relying on when
calling kmpli from a script or CI job.

To generate all the platforms (IOS with SwiftUI and Web with React/TS), with a name, project ID and with tests, run:

```bash
kmpli --name="CMPProject" --pid="org.cmp.project" --platforms="android,ios(swiftui),desktop,web(react),server" --include-tests
```

To generate only IOS (with Compose UI), Desktop, Web (with Compose UI) & Server. Exclude tests:

```bash
kmpli --name="CMPProject" --pid="org.cmp.project" --platforms="ios,desktop,web,server"
```

To generate a template project. Shared UI or Native UI or Barebones KMP library. Pick one:

```
kmpli --name="CMPProject" --pid="org.cmp.project" --template="shared-ui"
kmpli --name="CMPProject" --pid="org.cmp.project" --template="native-ui"
kmpli --name="CMPProject" --pid="org.cmp.project" --template="library"
```

To generate platforms with the Kotlin Toolchain instead of Gradle:

```bash
kmpli --name="CMPProject" --pid="org.cmp.project" --platforms="android,ios" --build-system="kotlin"
```

To generate a template configured with the Kotlin Toolchain rather than Gradle (available for
Shared UI and Native UI). Pick one:

```
kmpli --name="CMPProject" --pid="org.cmp.project" --template="shared-ui-toolchain"
kmpli --name="CMPProject" --pid="org.cmp.project" --template="native-ui-toolchain"
```

### Troubleshooting

#### SSL/TLS Certificate Errors

If you encounter an error like:

```
TLS verification failed for request
SSL peer certificate or SSH remote key was not OK
```

This means kmpli could not find a certificate store to verify against. Almost always Linux, where
the bundled TLS library has no trust store of its own.

**Solution:**

1. **Install a certificate store:**

   ```bash
   # Linux (Debian/Ubuntu)
   sudo apt-get install -y ca-certificates

   # Linux (Fedora/RHEL)
   sudo dnf install -y ca-certificates
   ```

   macOS and Windows read the OS trust store, so there is nothing to install there.

2. **Check that a bundle exists where kmpli looks:**

   ```bash
   ls -la /etc/ssl/certs/ca-certificates.crt   # Debian, Ubuntu
   ls -la /etc/pki/tls/certs/ca-bundle.crt     # Fedora, RHEL
   ```

3. **Name your bundle explicitly** if it lives somewhere else. kmpli checks the usual paths on
   Linux, but an explicit setting always wins:

   ```bash
   export CURL_CA_BUNDLE=/path/to/cert.pem
   kmpli --name="CMPProject" --pid="org.cmp.project"
   ```

4. **Which store each platform uses.** macOS verifies through `Security.framework` and Windows
   through Schannel, both of which read the OS trust store, so a failure on either is more
   likely a proxy or antivirus intercepting TLS than a missing certificate. Linux is the case
   worth checking: kmpli looks for `/etc/ssl/certs/ca-certificates.crt`,
   `/etc/pki/tls/certs/ca-bundle.crt`, `/etc/ssl/ca-bundle.pem`, `/etc/pki/tls/cacert.pem` and
   `/etc/ssl/cert.pem`, in that order, then falls back to the `/etc/ssl/certs` directory. If your
   store is somewhere else, name it with `CURL_CA_BUNDLE` or `SSL_CERT_FILE`.

   Versions up to v1.6.1 linked Homebrew's curl on macOS and needed `brew install curl`. Later
   builds bundle their own, so that is no longer required.

#### Linux aarch64: `libssh.so.4` not found (v1.6.1 and earlier)

On a clean Debian aarch64 system the arm64 binary refuses to start:

```
kmpli: error while loading shared libraries: libssh.so.4: cannot open shared object file
```

Either upgrade kmpli, or install libssh:

```sh
sudo apt-get install -y libssh-4
```

Only the aarch64 binary was affected, and only up to v1.6.1. Those builds linked a libcurl built
with SSH support, so the binary asked for `libssh.so.4` directly. Debian satisfies libcurl with
libssh2 and ships no `libssh.so.4`, while Ubuntu builds libcurl against libssh and normally
already has it, which is why the same binary ran on one and not the other. Later builds bundle
libcurl without SSH support and no longer reference it.

#### Alpine, or any other musl-based distribution

kmpli's Linux binaries are linked against glibc, so they do not run on musl **out of the box**.
Building from source does not change that: Kotlin/Native links against its own bundled glibc
sysroot, so a binary compiled on Alpine would still ask for glibc. Alpine's compatibility layer
covers the gap, though, and that is the short answer below.

**What it looks like.** The download succeeds, then the binary fails to start with a misleading
message along the lines of:

```
sh: kmpli: not found
```

The file is there. What is missing is the dynamic loader it asks for. Confirm it with:

```sh
ls /lib64/ld-linux-x86-64.so.2
# No such file or directory  ->  this is the problem
```

**What to do.** Install Alpine's glibc compatibility layer and run kmpli directly:

```sh
apk add gcompat libgcc ca-certificates
kmpli --name="MyApp" --pid="com.acme.myapp" --platforms="android,ios"
```

Three packages, each for one reason. `gcompat` provides the loader and the glibc shim, `libgcc`
provides `libgcc_s.so.1`, and `ca-certificates` provides the trust store kmpli verifies against.
Nothing else is needed: zlib and a BusyBox `unzip` are already in the base image. Verified on
`alpine:latest` for both `--template` and `--platforms` generation, on x86_64 and aarch64 alike,
and covered by CI on every push.

One version note. Binaries up to v1.6.1 do not run on aarch64 musl at all: musl's loader
rejects them with `Not a valid dynamic program`. Later builds link far less, needing nothing
beyond zlib and glibc where v1.6.1 pulled in libcurl's whole dependency closure, and they load
fine. x86_64 was never affected.

`gcompat` is a compatibility shim rather than real glibc. If you would rather not depend on one,
running kmpli inside a glibc container works too, and writes the project into the current
directory:

```sh
docker run --rm -v "$PWD:/out" -w /out debian:stable-slim sh -c '
  set -e
  apt-get update -qq && apt-get install -y -qq curl ca-certificates unzip >/dev/null
  curl -fsSL https://raw.githubusercontent.com/chornge/kmpli/main/kmpli -o /usr/local/bin/kmpli
  chmod +x /usr/local/bin/kmpli
  kmpli --name="MyApp" --pid="com.acme.myapp" --platforms="android,ios"
  chown -R '"$(id -u):$(id -g)"' /out
'
```

The final `chown` matters. Everything inside the container runs as root, so without it the
generated project would be owned by root on your host. The `$(id -u)` and `$(id -g)` expand on
your machine, not inside the container.

Three other details are easy to get wrong. `unzip` has to be in the install list, because kmpli
shells out to it. The launcher is fetched directly rather than piped through `install.sh`,
because that script installs with `sudo`, which slim images do not ship. And `set -e` is what
makes a failure visible at all: without it the trailing `chown` succeeds regardless, so the
container exits 0 having generated nothing.

On aarch64, add `libssh-4` to the install list if you are running v1.6.1 or earlier, for the
reason given in the `libssh.so.4` section above.

#### Other Issues

- **Binary not found**: Make sure you've run the installer and restarted your terminal
- **Permission denied**: On Unix systems, ensure the binary is executable: `chmod +x ~/.kmpli/bin/kmpli-*`
- **Command not found**: Check that the install directory is in your PATH

### License

[Apache-2.0](LICENSE)

### Acknowledgments

- Inspired by a need to create Kotlin/Compose Multiplatform projects without the need for a GUI.
- Thanks to the Kotlin and Compose Multiplatform communities for the support and for the resources.
