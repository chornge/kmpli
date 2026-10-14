# =============================================================================
# Kmpli - build, test, and install automation
#
# Requirements:
#   * A POSIX shell. On Windows, run make from Git Bash, MSYS2, or Cygwin --
#     every target shells out to ./gradlew, and the Windows-only file
#     operations below are delegated to PowerShell rather than cmd.exe.
#   * A JDK; Gradle itself comes from the wrapper checked into this repo.
#
# Any variable below can be overridden per invocation, e.g.
#   make install INSTALL_DIR=/opt/bin
# =============================================================================

# -----------------------------------------------------------------------------
# Host detection
# -----------------------------------------------------------------------------
UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)

# KN_TARGET is the Kotlin/Native target name for this host. composeApp/build.gradle.kts
# declares exactly one target -- the host's -- via `when (HostManager.host)`, and the
# Kotlin Multiplatform plugin then generates per-target Gradle task names from it:
#
#   KN_TARGET = macosArm64  ->  link task    linkReleaseExecutableMacosArm64
#                           ->  output dir   composeApp/build/bin/macosArm64/releaseExecutable/
#
# So BINARY_PATH is derived below, while RELEASE_TASK is spelled out per host because
# the link task capitalizes the target name and Make has no way to change case without
# another shell invocation.
#
# The `test` target deliberately uses the aggregate `allTests` rather than a per-target
# task, so it matches .github/workflows/build.yml exactly and keeps working if more than
# one target is ever declared (see issue #19).
#
# BINARY_EXT is Kotlin/Native's executable suffix: .kexe on Apple/Linux, .exe on Windows.
# WINDOWS is a flag the install/uninstall recipes branch on.

ifeq ($(UNAME_S),Darwin)
    ifneq (,$(filter arm64 aarch64,$(UNAME_M)))
        KN_TARGET    := macosArm64
        RELEASE_TASK := linkReleaseExecutableMacosArm64
    else ifneq (,$(filter x86_64,$(UNAME_M)))
        KN_TARGET    := macosX64
        RELEASE_TASK := linkReleaseExecutableMacosX64
    endif
    BINARY_EXT  := .kexe
    INSTALL_DIR := /usr/local/bin

else ifeq ($(UNAME_S),Linux)
    ifneq (,$(filter aarch64 arm64,$(UNAME_M)))
        KN_TARGET    := linuxArm64
        RELEASE_TASK := linkReleaseExecutableLinuxArm64
    else ifneq (,$(filter x86_64,$(UNAME_M)))
        KN_TARGET    := linuxX64
        RELEASE_TASK := linkReleaseExecutableLinuxX64
    endif
    BINARY_EXT  := .kexe
    INSTALL_DIR := /usr/local/bin

else
    # Windows under Git Bash / MSYS2 / Cygwin, where uname -s reports
    # MINGW64_NT-*, MSYS_NT-* or CYGWIN_NT-*. Only mingwX64 is supported.
    ifneq (,$(filter x86_64,$(UNAME_M)))
        KN_TARGET    := mingwX64
        RELEASE_TASK := linkReleaseExecutableMingwX64
    endif
    BINARY_EXT  := .exe
    INSTALL_DIR := $(USERPROFILE)/bin
    WINDOWS     := 1
endif

BINARY_PATH := composeApp/build/bin/$(KN_TARGET)/releaseExecutable/kmpli$(BINARY_EXT)

# PowerShell accepts forward slashes, but normalize for legible output.
INSTALL_DIR_WIN := $(subst /,\,$(INSTALL_DIR))
BINARY_PATH_WIN := $(subst /,\,$(BINARY_PATH))

# -----------------------------------------------------------------------------
# Targets
# -----------------------------------------------------------------------------
.PHONY: help build test clean release sanity install uninstall platform-check
.DEFAULT_GOAL := help

help: ## Show this help message
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@awk 'BEGIN { FS = ":.*##" } /^[a-zA-Z][a-zA-Z0-9_-]*:.*##/ { printf "  \033[36m%-11s\033[0m %s\n", $$1, $$2 }' $(MAKEFILE_LIST)
	@echo ""
	@echo "Host:        $(UNAME_S) $(UNAME_M)"
	@echo "KN target:   $(if $(KN_TARGET),$(KN_TARGET),(unsupported))"
	@echo "Install dir: $(INSTALL_DIR)"

# Guard for every target that needs a Kotlin/Native host target. Kept as a
# prerequisite rather than a parse-time $(error) so `make help` still reports
# what it detected on an unsupported machine.
platform-check:
ifeq ($(KN_TARGET),)
	@echo "Error: unsupported host: $(UNAME_S) $(UNAME_M)" >&2; \
	 echo "       Supported: macOS and Linux on arm64/x86_64, Windows on x86_64." >&2; \
	 exit 1
endif
ifdef WINDOWS
	@[ -n "$(USERPROFILE)" ] || { echo "Error: USERPROFILE is unset; cannot resolve $(INSTALL_DIR)." >&2; exit 1; }
endif

build: platform-check ## Assemble and check the host target (runs its tests too)
	./gradlew build

test: platform-check ## Run the unit tests (./gradlew allTests, exactly as CI does)
	./gradlew allTests

clean: ## Delete Gradle build outputs
	./gradlew clean

release: platform-check ## Link the optimized release binary for the host target
	./gradlew $(RELEASE_TASK)

# Separate recipe lines rather than `sanity: clean test build`: prerequisites are
# unordered, so `make -j sanity` could otherwise run clean alongside test or build.
sanity: ## Run clean, then test, then build -- strictly in that order
	@$(MAKE) clean
	@$(MAKE) test
	@$(MAKE) build

install: release ## Build the release binary and copy it to INSTALL_DIR
ifdef WINDOWS
	@powershell -NoProfile -ExecutionPolicy Bypass -Command " \
	  New-Item -ItemType Directory -Force -Path '$(INSTALL_DIR_WIN)' | Out-Null; \
	  Copy-Item -Force '$(BINARY_PATH_WIN)' '$(INSTALL_DIR_WIN)\kmpli.exe'; \
	  Write-Host 'Installed kmpli to $(INSTALL_DIR_WIN)\kmpli.exe'; \
	  Write-Host 'Make sure $(INSTALL_DIR_WIN) is in your PATH'; \
	"
else
	@mkdir -p "$(INSTALL_DIR)"
	install -m 755 "$(BINARY_PATH)" "$(INSTALL_DIR)/kmpli"
	@echo "Installed kmpli to $(INSTALL_DIR)/kmpli"
endif

# Covers every way kmpli can land on a machine:
#   1. a package manager      -- Homebrew (Formula/kmpli.rb) or Scoop (bucket/kmpli.json)
#   2. INSTALL_DIR            -- `make install`, and install.sh's launcher on Unix
#   3. ~/.kmpli               -- install.ps1's launcher plus the binaries the
#                                launcher caches in ~/.kmpli/bin at first run
#   4. the user PATH entry    -- only install.ps1 adds one
# Chocolatey is deliberately absent: kmpli is not published there (the `choco`
# lines in README.md and the CI workflows install curl/openssl prerequisites).
uninstall: ## Remove kmpli installed by any supported method
ifdef WINDOWS
	@powershell -NoProfile -ExecutionPolicy Bypass -Command " \
	  \$$found = \$$false; \
	  \$$kmpliHome = \"\$$env:USERPROFILE\.kmpli\"; \
	  if (Get-Command scoop -ErrorAction SilentlyContinue) { \
	    \$$installed = scoop list 2>\$$null | Select-String 'kmpli'; \
	    if (\$$installed) { \
	      Write-Host 'Uninstalling via Scoop...'; \
	      scoop uninstall kmpli; \
	      \$$found = \$$true; \
	    } \
	  } \
	  if (Test-Path '$(INSTALL_DIR_WIN)\kmpli.exe') { \
	    Remove-Item -Force '$(INSTALL_DIR_WIN)\kmpli.exe'; \
	    Write-Host 'Removed $(INSTALL_DIR_WIN)\kmpli.exe'; \
	    \$$found = \$$true; \
	  } \
	  if (Test-Path \$$kmpliHome) { \
	    Remove-Item -Recurse -Force \$$kmpliHome; \
	    Write-Host \"Removed launcher and cached binaries from \$$kmpliHome\"; \
	    \$$found = \$$true; \
	  } \
	  \$$userPath = [Environment]::GetEnvironmentVariable('Path', 'User'); \
	  if (\$$userPath -and ((\$$userPath -split ';') -contains \$$kmpliHome)) { \
	    \$$kept = ((\$$userPath -split ';') | Where-Object { \$$_ -and \$$_ -ne \$$kmpliHome }) -join ';'; \
	    [Environment]::SetEnvironmentVariable('Path', \$$kept, 'User'); \
	    Write-Host \"Removed \$$kmpliHome from the user PATH (restart your terminal)\"; \
	    \$$found = \$$true; \
	  } \
	  if (-not \$$found) { Write-Host 'kmpli not found' }; \
	"
else
	@found=0; \
	kmpli_home="$${HOME:?HOME is unset; refusing to guess ~/.kmpli}/.kmpli"; \
	if command -v brew >/dev/null 2>&1 && brew list kmpli >/dev/null 2>&1; then \
	    echo "Uninstalling via Homebrew..."; \
	    brew uninstall kmpli; \
	    found=1; \
	fi; \
	if [ -f "$(INSTALL_DIR)/kmpli" ]; then \
	    rm -f "$(INSTALL_DIR)/kmpli"; \
	    echo "Removed $(INSTALL_DIR)/kmpli"; \
	    found=1; \
	fi; \
	if [ -d "$$kmpli_home" ]; then \
	    rm -rf "$$kmpli_home"; \
	    echo "Removed cached binaries from $$kmpli_home"; \
	    found=1; \
	fi; \
	if [ $$found -eq 0 ]; then \
	    echo "kmpli not found"; \
	fi
endif
