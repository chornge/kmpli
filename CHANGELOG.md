# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
