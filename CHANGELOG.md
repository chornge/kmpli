# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed

- `--template` generation worked for no template at all. github.com answers the
  `archive/refs/heads/<branch>.zip` redirect with a `content-security-policy` header of roughly
  9 KB, past the curl engine's per-header line limit, so every run failed with "Header line length
  limit exceeded" while the URLs themselves still returned 200. All five templates now download
  from codeload.github.com, the host that redirect points at, which serves the same archive rooted
  at the same folder with headers under 100 bytes
- The `linux-arm64` binary could not start on Debian. ktor 3.0.1 bundled a static `libcurl.a` built
  with SSH support, so the binary referenced `libssh.so.4` directly, and Debian satisfies libcurl
  with libssh2 and ships no such library. The aarch64 binary wanted 24 shared libraries where
  x86_64 wanted 11, and 13 of those came from a linker closure it should never have carried
- The container recipe under Troubleshooting could not work as written. `install.sh` installs with
  `sudo`, which slim images do not ship, `unzip` was missing from the install list, and without
  `set -e` the trailing `chown` made the whole recipe exit 0 having generated nothing

### Changed

- ktor 3.0.1 to 3.3.3. Its klib bundles libcurl, OpenSSL and their closure and declares its own
  `linkerOpts.linux=-lz`, which removes the `pkg-config --libs --static libcurl` closure, the
  static-`libgcc.a` outline-atomics hack, and eleven of the twelve arm64 cross-compilation
  packages from both workflows. On macOS the binary no longer links Homebrew's libcurl at all
- `unzip` is now documented as a runtime dependency on macOS and Linux. kmpli has always shelled
  out to it to unpack the generated project, and this was never stated

### Added

- `Smoke Test (minimal container)` in `build.yml` for both Linux targets, running the binary in
  `debian:stable-slim` with only the documented prerequisites installed. Runner images carry a
  broad package set, so the other smoke tests can pass with a binary that would not start on a
  user's machine, which is how the `libssh.so.4` gap went unnoticed
- `Verify aarch64 Binary` now fails the build if libssh, LDAP, RTMP or GSSAPI reappear in the
  binary's `DT_NEEDED` entries
- `Template generation still works` in the daily `Upstream Check`, covering every template in
  `Template.kt`. The existing check proved the archives were reachable, which is not the same as
  kmpli being able to consume them, and that gap is what hid the header-limit failure for a week

## [1.6.1] - 2026-10-15

### Added

- Scheduled `Upstream Check` workflow, run daily and on demand, covering the two things kmpli
  does not control: that every template URL in `Template.kt` still resolves, and that
  `--platforms` generation still works against kmp.jetbrains.com. It tests the latest released
  binary rather than a build of `main`, opens an issue on failure, and is deliberately not part
  of `build-gate` so an upstream outage cannot block a merge (#25)
- `kmpli-source.zip` is now listed under Manual Download in the generated release notes. It was
  already attached to every release but undocumented
- Tests that drive the TUI wizard end to end over its numbered-prompt path, covering template
  selection, defaults, declining tests, back navigation, cancelling, and exhausted input (#27)
- `--build-system` for `--platforms` generation, accepting `gradle` (the default) or `kotlin` for
  the Kotlin Toolchain. The wizard offers the same choice when generating from platforms. It has
  no effect with `--template`, since each template id already names its build system (#35)

### Changed

- `TuiWizard` takes an optional `interactive` flag, defaulting to probing the terminal exactly as
  before. The numbered-prompt fallback is real behaviour on terminals without raw mode and was
  otherwise unreachable in a test, because a recording terminal reports raw mode as supported and
  then never emits a key event

### Fixed

- README listed stale `make` targets: `lint` was missing, and the descriptions of `test` and
  `sanity` predated those targets changing
- README told users to install `-dev` packages to run kmpli. The binary needs the runtime
  libraries (`libcurl.so.4`), not the `.so` symlinks that `-dev` provides; on Debian and Ubuntu
  `libcurl` arrives with `curl`. The `-dev` packages remain listed under Building from Source,
  where they are required
- Quick Start sat above the prerequisites section with no pointer to it, so a reader following
  it top to bottom on a fresh machine could hit a TLS error with no explanation

## [1.6.0] - 2026-10-14

### Added

- CI smoke test covering `--platforms` generation against the live service. The existing smoke
  tests only exercised `--template`, which is why the regression below went unnoticed
- Animated demo of the interactive wizard at the top of the README, captured from the real
  binary by `scripts/record-demo.py`, which drives it through a pseudo-terminal so the demo
  can be regenerated whenever the wizard changes

### Fixed

- The `shared-ui-amper` and `native-ui-amper` templates returned `HTTP 404`. JetBrains replaced
  Amper with the Kotlin Toolchain in the Multiplatform Wizard and removed the upstream `amper`
  branches, so both had been failing for every user who selected them (#31)
- `--platforms` generation reported `Extraction produced no recognizable project folder` and exited
  `1` even though the project had been generated correctly. `kmp.jetbrains.com` returns an archive
  already rooted at the project name, so nothing needs renaming, whereas the template repositories
  return `<repo>-<branch>` and do. Treating a missing folder as an error in both cases broke the
  first. Introduced in 1.5.0

### Changed

- Those two templates are renamed to `shared-ui-toolchain` and `native-ui-toolchain` and point at
  the upstream `kotlin-toolchain` branches, matching what the Multiplatform Wizard now offers.
  **The old ids are no longer accepted**, so any script using `--template=shared-ui-amper` or
  `--template=native-ui-amper` needs updating (#31)
- `unzip` now runs quietly, matching the Windows extraction path. Unix users previously saw a full
  file listing that Windows users never did

## [1.5.0] - 2026-10-14

### Added

- Linux ARM64 (aarch64) binaries, cross-compiled from the x86_64 Linux runner because
  Kotlin/Native publishes no linux-aarch64 host compiler (#19)
- Homebrew formula now selects between Linux ARM64 and x64 builds
- CI smoke tests that run the built binary on every platform, covering `--version`, `--help`
  and a live template generation. This is the first coverage of the real network, TLS,
  unzip/tar and placeholder-rewriting path, which the unit tests mock out
- The aarch64 smoke test runs on real ARM hardware, since that binary is cross-compiled and
  cannot execute on its build host
- detekt static analysis, run by `make lint` and by CI. Existing findings are recorded in
  `config/detekt-baseline.xml` so they do not block the build, while new ones fail it (#24)

### Fixed

- The release workflow could publish a corrupted release when triggered by `workflow_dispatch`
  from a branch. The tag check was skipped rather than failed, so the job succeeded and the
  release was named after the branch ref, which then propagated into the Homebrew formula and
  Scoop manifest via the auto-merged manifest PR. It now fails closed on any non-tag ref
- Template extraction on Windows failed whenever Git for Windows was installed, because `tar`
  on PATH resolves to GNU tar, which cannot read zip archives. The Windows-bundled bsdtar is
  now invoked by absolute path, with a PowerShell `Expand-Archive` fallback
- A failed extraction reported `Project generation complete!` and exited `0`. Extraction
  failures, an unrecognized project folder and an empty result now report an error and exit `1`

### Changed

- Launcher resolves `aarch64`/`arm64` on Linux to `kmpli-linux-arm64` instead of reporting the
  architecture as unsupported
- Linux CI runners pinned to `ubuntu-24.04` rather than `ubuntu-latest`, ahead of the
  `ubuntu-latest` migration to Ubuntu 26
- Gradle and Kotlin/Native caches are keyed per build target, so the two Linux targets no
  longer share one cache entry
- `make sanity` now runs lint before tests and build
- `Kmpli.parse()` decomposed from 79 lines into `printHelp`, `resolveProject` and a handler per
  generation mode. Name defaulting, package ID derivation and validation were previously
  duplicated verbatim in the template and platform branches and now share one function (#26)
- Invalid user input raises `InvalidInputException` rather than `IllegalStateException`, so a
  user mistake can no longer be confused with a violated internal invariant. Input errors now
  also suggest `kmpli --help`, which environment and IO failures deliberately do not (#28)

## [1.4.0] - 2026-10-14

### Added

- `--version` / `-v` flag reporting the version declared by `appVersion` in `gradle/libs.versions.toml`
- Release job that refuses to publish a tag disagreeing with `appVersion`
- Unit tests for the TUI wizard's selection labels and the wizard-to-CLI argument contract (new `nativeTest` source set)

### Fixed

- Invalid input now prints the error and exits `1` instead of aborting with a Kotlin stack trace and `SIGABRT`
- Spurious `Unknown argument` when a value was passed as `--name Foo` rather than `--name=Foo`
- `--name` help text omitted spaces, which project names may contain
- `--pid` help text omitted the 100-character limit
- `--name` error message listed `-` as allowed without stating that the first character must be a letter or digit
- `--pid` error message described the rule as a valid Java package name, when lowercase segments are required
- `--platforms` error message rendered with no value when an entry was empty (e.g. `android,,ios`)
- Homebrew command in generated release notes omitted the tap URL, so `brew tap chornge/kmpli` resolved to a nonexistent repository
- Generated release notes included the previous release's `Update package manifests` commit
- Launcher on Linux ARM64 tried to download a binary that is never published, and now reports the architecture as unsupported (#19)
- Makefile `install` on Windows used cmd.exe syntax while every other target requires a POSIX shell
- Makefile `uninstall` left the launcher, batch wrapper and PATH entry created by `install.ps1` in place
- Makefile `sanity` prerequisites could run out of order under `make -j`

### Changed

- Flag names must match exactly, so a typo such as `--nameXYZ=Foo` is reported rather than accepted as `--name`
- `--platforms` entries must be fully formed, so `ios(swiftui` and `android!!!` are rejected rather than silently using the default UI
- TUI template selection resolves ids by exact match against the template list instead of splitting the label on a separator
- `make test` runs `./gradlew allTests`, matching the test job in CI
- Makefile rewritten with a shared Unix branch, self-documenting `help`, and a guard for unsupported hosts
- README documents supported platforms, uninstalling, the Makefile targets, exit codes and the flag constraints

## [1.3.0] - 2026-01-13

### Added

- Interactive TUI wizard when running `kmpli` with no arguments
- Arrow-key navigation on supported terminals, numbered prompts as fallback
- Circular navigation (wrap-around) for arrow keys in selection lists
- Back navigation throughout the wizard with state persistence
- Cursor position preservation in platform toggle selection
- Vim-style j/k keys for navigation
- Styled interface with color palette
- UI framework selection for iOS (Compose/SwiftUI) and Web (Compose/React) in TUI
- Default hints for project name (CMPProject) and package ID (org.example.app)

### Fixed

- Memory leaks in native platform operations (proper try-finally for nativeHeap allocations)
- Inconsistent default package ID generation between template and platform modes
- Invalid package ID generation when project name starts with a digit
- Overly broad exception catching in file operations (now catches only IOException)
- ZIP extraction failing when project name contains spaces

### Changed

- Default package ID now sanitizes special characters and handles edge cases

## [1.2.11] - 2026-01-02

### Changed

- Release workflow uses `--admin` merge to bypass required checks for manifest-only PRs

## [1.2.10] - 2026-01-02

### Changed

- Branch protection rules updated to allow automated PR auto-merge

## [1.2.9] - 2026-01-02

### Changed

- Release workflow now creates PR for package manifest updates (Homebrew/Scoop) with auto-merge enabled
- Package manifest PR branches auto-delete after merge
- Makefile `sanity` target (clean, test, build)

## [1.2.7] - 2026-01-02

### Added

- Package directory renaming to match `--pid` parameter (e.g., `com/jetbrains/kmpapp` → `io/example/myapp`)
- Support for library template folder detection during extraction

### Fixed

- Library template (`--template="library"`) now correctly renames extracted folder to project name
- Native UI templates now use correct package ID for replacements
- Each template now has its own `oldPackageId` for accurate placeholder replacement

### Changed

- Templates now store their default package ID for proper placeholder replacement
- CI/CD uses `macos-15-intel` runner for macOS x64 builds (replacing retired `macos-13`)

## [1.2.6] - 2025-12-16

### Added

- Homebrew formula for macOS/Linux installation (`brew tap chornge/kmpli`)
- Scoop manifest for Windows installation (`scoop bucket add kmpli`)
- Auto-update of package manifests on release
- Input validation for project names and package IDs
- Path traversal prevention in file operations
- HTTP response validation (status codes)
- Package ID length validation (max 100 chars)
- Tag format validation in PowerShell launcher

### Changed

- CI/CD workflow Tests (only runs on Linux)
- CI/CD workflow Builds (skipped for documentation-only changes)
- Windows ZIP extraction uses `tar -xf` instead of `unzip`

## [1.2.2] - 2025-11-15

### Changed

- Maintenance release with minor updates

## [1.2.1] - 2025-11-15

### Changed

- Version bump and stability improvements

## [1.2.0] - 2025-11-15

### Added

- Major feature release

### Changed

- Updated dependencies

## [1.1.3] - 2025-11-15

### Changed

- Cleaned up CI/CD workflows
- Optimized for smaller compressed file size

## [1.1.1] - 2025-11-15

### Changed

- Version bump

## [1.1.0] - 2025-11-15

### Added

- Initial stable release with core functionality

### Features

- Generate Kotlin Multiplatform projects from CLI
- Support for Android, iOS, Desktop, Web, and Server targets
- Template-based generation (shared-ui, native-ui, library)
- Amper build system templates
- Platform-based generation via JetBrains KMP API
- Cross-platform binaries (macOS, Linux, Windows)
- Smart launcher with auto-download of binaries

## [0.2.5] - 2025-11-15

### Changed

- Pre-release improvements

## [0.2.4] - 2025-11-15

### Added

- Initial pre-release
