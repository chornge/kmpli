# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
