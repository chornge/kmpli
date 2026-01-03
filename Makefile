.PHONY: build test clean release install uninstall sanity help

# Detect OS
UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)

# Determine platform-specific tasks and paths
ifeq ($(UNAME_S),Darwin)
    ifeq ($(UNAME_M),arm64)
        TEST_TASK := macosArm64Test
        RELEASE_TASK := linkReleaseExecutableMacosArm64
        PLATFORM_DIR := macosArm64
        BINARY_EXT := .kexe
    else
        TEST_TASK := macosX64Test
        RELEASE_TASK := linkReleaseExecutableMacosX64
        PLATFORM_DIR := macosX64
        BINARY_EXT := .kexe
    endif
    INSTALL_DIR := /usr/local/bin
else ifeq ($(UNAME_S),Linux)
    ifeq ($(UNAME_M),aarch64)
        TEST_TASK := linuxArm64Test
        RELEASE_TASK := linkReleaseExecutableLinuxArm64
        PLATFORM_DIR := linuxArm64
        BINARY_EXT := .kexe
    else
        TEST_TASK := linuxX64Test
        RELEASE_TASK := linkReleaseExecutableLinuxX64
        PLATFORM_DIR := linuxX64
        BINARY_EXT := .kexe
    endif
    INSTALL_DIR := /usr/local/bin
else
    # Windows (MSYS/MinGW/Cygwin)
    TEST_TASK := mingwX64Test
    RELEASE_TASK := linkReleaseExecutableMingwX64
    PLATFORM_DIR := mingwX64
    BINARY_EXT := .exe
    INSTALL_DIR := $(USERPROFILE)/bin
endif

BINARY_PATH := composeApp/build/bin/$(PLATFORM_DIR)/releaseExecutable/kmpli$(BINARY_EXT)

# Default target
help:
	@echo "Usage: make [target]"
	@echo ""
	@echo "Targets:"
	@echo "  build      Build native binaries for current platform"
	@echo "  test       Run unit tests"
	@echo "  clean      Clean build artifacts"
	@echo "  release    Build release binary for current platform"
	@echo "  install    Build and install kmpli to $(INSTALL_DIR)"
	@echo "  uninstall  Remove kmpli from $(INSTALL_DIR)"
	@echo "  sanity     Run clean, test, and build"
	@echo "  help       Show this help message"
	@echo ""
	@echo "Detected: $(UNAME_S) $(UNAME_M)"

build:
	./gradlew build

test:
	./gradlew $(TEST_TASK)

clean:
	./gradlew clean

release:
	./gradlew $(RELEASE_TASK)

sanity: clean test build

install: release
ifeq ($(UNAME_S),Darwin)
	@mkdir -p $(INSTALL_DIR)
	cp $(BINARY_PATH) $(INSTALL_DIR)/kmpli
	chmod +x $(INSTALL_DIR)/kmpli
	@echo "Installed kmpli to $(INSTALL_DIR)/kmpli"
else ifeq ($(UNAME_S),Linux)
	@mkdir -p $(INSTALL_DIR)
	cp $(BINARY_PATH) $(INSTALL_DIR)/kmpli
	chmod +x $(INSTALL_DIR)/kmpli
	@echo "Installed kmpli to $(INSTALL_DIR)/kmpli"
else
	@if not exist "$(INSTALL_DIR)" mkdir "$(INSTALL_DIR)"
	copy "$(subst /,\,$(BINARY_PATH))" "$(INSTALL_DIR)\kmpli.exe"
	@echo Installed kmpli to $(INSTALL_DIR)\kmpli.exe
	@echo Make sure $(INSTALL_DIR) is in your PATH
endif

uninstall:
ifeq ($(UNAME_S),Darwin)
	@found=0; \
	if command -v brew >/dev/null 2>&1 && brew list kmpli >/dev/null 2>&1; then \
		echo "Uninstalling via Homebrew..."; \
		brew uninstall kmpli; \
		found=1; \
	fi; \
	if [ -f $(INSTALL_DIR)/kmpli ]; then \
		rm -f $(INSTALL_DIR)/kmpli; \
		echo "Removed kmpli from $(INSTALL_DIR)"; \
		found=1; \
	fi; \
	if [ -d ~/.kmpli/bin ]; then \
		rm -rf ~/.kmpli/bin; \
		echo "Removed cached binaries from ~/.kmpli/bin"; \
		found=1; \
	fi; \
	if [ $$found -eq 0 ]; then \
		echo "kmpli not found"; \
	fi
else ifeq ($(UNAME_S),Linux)
	@found=0; \
	if command -v brew >/dev/null 2>&1 && brew list kmpli >/dev/null 2>&1; then \
		echo "Uninstalling via Homebrew..."; \
		brew uninstall kmpli; \
		found=1; \
	fi; \
	if [ -f $(INSTALL_DIR)/kmpli ]; then \
		rm -f $(INSTALL_DIR)/kmpli; \
		echo "Removed kmpli from $(INSTALL_DIR)"; \
		found=1; \
	fi; \
	if [ -d ~/.kmpli/bin ]; then \
		rm -rf ~/.kmpli/bin; \
		echo "Removed cached binaries from ~/.kmpli/bin"; \
		found=1; \
	fi; \
	if [ $$found -eq 0 ]; then \
		echo "kmpli not found"; \
	fi
else
	@powershell -Command "$$found = $$false; if (Get-Command scoop -ErrorAction SilentlyContinue) { $$list = scoop list 2>$$null | Select-String 'kmpli'; if ($$list) { Write-Host 'Uninstalling via Scoop...'; scoop uninstall kmpli; $$found = $$true } } if (Test-Path '$(INSTALL_DIR)\kmpli.exe') { Remove-Item '$(INSTALL_DIR)\kmpli.exe'; Write-Host 'Removed kmpli from $(INSTALL_DIR)'; $$found = $$true } if (Test-Path \"$$env:USERPROFILE\.kmpli\bin\") { Remove-Item -Recurse -Force \"$$env:USERPROFILE\.kmpli\bin\"; Write-Host 'Removed cached binaries from ~/.kmpli/bin'; $$found = $$true } if (-not $$found) { Write-Host 'kmpli not found' }"
endif
