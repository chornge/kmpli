![CI/CD](https://github.com/chornge/kmpli/actions/workflows/build.yml/badge.svg?branch=main)
![Downloads](https://img.shields.io/github/downloads/chornge/kmpli/total)

# Kmpli

Pronounced `"Comply"`

A command-line interface (CLI) designed to streamline the creation of new Kotlin/Compose Multiplatform
projects. It offers a flexible way to generate customized project configurations and directory structures, including
configuring with Amper, and test setups.

### Installation

**Prerequisites (Required)**

Before installing kmpli, you **must** install OpenSSL and curl for SSL/TLS support:

```bash
# macOS
brew install curl openssl

# Linux (Debian/Ubuntu)
sudo apt-get install -y ca-certificates curl libcurl4-openssl-dev libssl-dev

# Windows (Scoop)
scoop install curl openssl

# Windows (Chocolatey)
choco install -y curl openssl.light
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

### Building from Source (Optional)

If you prefer to build from source or need to contribute:

**Requirements:** JDK-17+, Gradle, Git, and platform-specific dependencies:

- **macOS**: `brew install openssl curl`
- **Linux**: `sudo apt-get install -y curl libcurl4-openssl-dev libssl-dev`
- **Windows**: `choco install -y curl openssl.light`

**Build:**

```bash
chmod +x gradlew
./gradlew build
```

Then run the installer as shown above. The launcher will use your locally built binary instead of downloading.

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
  --name TEXT       Project name (optional)
  --pid TEXT        Package ID (optional)
  --template TEXT   Multiplatform template
                    shared-ui -> Shared UI App (Compose)
                    native-ui -> Native UI App (Compose + SwiftUI)
                    library -> Bare-bones Multiplatform Library
                    shared-ui-amper -> Shared UI App (configured with Amper)
                    native-ui-amper -> Native UI App (configured with Amper)
  --platforms TEXT  Target platform(s)
                    android,ios(swiftui),web(react),desktop,server
  --include-tests   Include sample tests (optional)
  --help, -h        Show this help message
```

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

To generate a template configured with Amper (only available with Shared UI or Native UI). Pick one:

```
kmpli --name="CMPProject" --pid="org.cmp.project" --template="shared-ui-amper"
kmpli --name="CMPProject" --pid="org.cmp.project" --template="native-ui-amper"
```

### Troubleshooting

#### SSL/TLS Certificate Errors

If you encounter an error like:

```
TLS verification failed for request
SSL peer certificate or SSH remote key was not OK
```

This means your system is missing SSL certificates or curl cannot find them.

**Solution:**

1. **Install OpenSSL** (if not already installed):

   ```bash
   # macOS
   brew install openssl curl

   # Linux (Debian/Ubuntu)
   sudo apt-get install -y ca-certificates curl libcurl4-openssl-dev libssl-dev

   # Windows (Scoop)
   scoop install curl openssl

   # Windows (Chocolatey - alternative)
   choco install -y curl openssl.light
   ```

2. **Verify installation**:

   ```bash
   # macOS - check if cert.pem exists
   ls -la /etc/ssl/cert.pem
   ls -la /opt/homebrew/etc/openssl/cert.pem  # Apple Silicon
   ls -la /usr/local/etc/openssl/cert.pem     # Intel
   ```

3. **Manual CA bundle path** (if auto-detection fails):

   ```bash
   # Set environment variable before running kmpli
   export CURL_CA_BUNDLE=/path/to/cert.pem
   kmpli --name="CMPProject" --pid="org.cmp.project"
   ```

4. **macOS specific**: If you recently installed/updated Homebrew, you may need to link openssl:
   ```bash
   brew link openssl
   # or for specific version
   brew link openssl@3
   ```

#### Other Issues

- **Binary not found**: Make sure you've run the installer and restarted your terminal
- **Permission denied**: On Unix systems, ensure the binary is executable: `chmod +x ~/.kmpli/bin/kmpli-*`
- **Command not found**: Check that the install directory is in your PATH

### License

[Apache-2.0](LICENSE)

### Acknowledgments

- Inspired by a need to create Kotlin/Compose Multiplatform projects without the need for a GUI.
- Thanks to the Kotlin and Compose Multiplatform communities for the support and for the resources.
