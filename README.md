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
- **Staying in the terminal.** An interactive wizard when you want one, flags when you do not.

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

kmpli needs curl and its SSL libraries present at runtime. These are the runtime packages, not
the development ones; the `-dev` packages are only needed to build from source.

```bash
# macOS
brew install curl openssl

# Linux (Debian/Ubuntu) - libcurl comes with curl
sudo apt-get install -y ca-certificates curl

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

- **macOS**: `brew install openssl curl`
- **Linux**: `sudo apt-get install -y curl libcurl4-openssl-dev libssl-dev`
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

This means your system is missing SSL certificates or curl cannot find them.

**Solution:**

1. **Install OpenSSL** (if not already installed):

   ```bash
   # macOS
   brew install openssl curl

   # Linux (Debian/Ubuntu) - ca-certificates is the one that matters here
   sudo apt-get install -y ca-certificates curl

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

3. **Point curl at the CA bundle explicitly.** kmpli does not search for a bundle itself; it leaves
   certificate verification to curl, so tell curl which file to use:

   ```bash
   export CURL_CA_BUNDLE=/path/to/cert.pem
   kmpli --name="CMPProject" --pid="org.cmp.project"
   ```

4. **macOS specific**: the released binary links against Homebrew's curl rather than the system
   one, so make sure that is installed and current:

   ```bash
   brew install curl ca-certificates
   brew upgrade curl ca-certificates
   ```

   Do not try to `brew link openssl`. The formula is named `openssl@3`, and it is keg-only, so
   Homebrew leaves it unlinked deliberately and the link will fail.

#### Other Issues

- **Binary not found**: Make sure you've run the installer and restarted your terminal
- **Permission denied**: On Unix systems, ensure the binary is executable: `chmod +x ~/.kmpli/bin/kmpli-*`
- **Command not found**: Check that the install directory is in your PATH

### License

[Apache-2.0](LICENSE)

### Acknowledgments

- Inspired by a need to create Kotlin/Compose Multiplatform projects without the need for a GUI.
- Thanks to the Kotlin and Compose Multiplatform communities for the support and for the resources.
