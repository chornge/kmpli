# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- Linux ARM64 (aarch64) binaries, cross-compiled from the x86_64 Linux runner because
  Kotlin/Native publishes no linux-aarch64 host compiler (#19)
- Homebrew formula now selects between Linux ARM64 and x64 builds
- CI smoke tests that run the built binary on every platform, covering `--version`, `--help`
  and a live template generation. This is the first coverage of the real network, TLS,
  unzip/tar and placeholder-rewriting path, which the unit tests mock out
- The aarch64 smoke test runs on real ARM hardware, since that binary is cross-compiled and
  cannot execute on its build host

### Changed

- Launcher resolves `aarch64`/`arm64` on Linux to `kmpli-linux-arm64` instead of reporting the
  architecture as unsupported
- Linux CI runners pinned to `ubuntu-24.04` rather than `ubuntu-latest`, ahead of the
  `ubuntu-latest` migration to Ubuntu 26
- Gradle and Kotlin/Native caches are keyed per build target, so the two Linux targets no
  longer share one cache entry

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
