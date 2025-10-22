![CI/CD](https://github.com/chornge/kmpli/actions/workflows/build.yml/badge.svg?branch=main)

# Kmpli

Pronounced `"Comply"`

A command-line interface (CLI) designed to streamline the creation of new Kotlin/Compose Multiplatform
projects. It offers a flexible way to generate customized project configurations and directory structures, including
configuring with Amper, and test setups.

### Requirements

Kotlin or Compose Multiplatform dev environment (IntelliJ, Android Studio) with JDK-17+, Gradle, Git.

If on:

• macOS (homebrew): `brew install openssl curl`

• linux: `sudo apt-get install -y libcurl4-openssl-dev libssl-dev build-essential pkg-config`

• windows (chocolatey): `choco install -y curl libssh2`

### Installation

```
git clone https://github.com/chornge/kmpli
cd kmpli
chmod +x gradlew
./gradlew build
```

Mac/Linux:

```bash
chmod +x install.sh
bash install.sh
```

Windows (Powershell - restart powershell after running):

```
powershell -ExecutionPolicy Bypass -File install.ps1
```

### Usage

To generate Android & IOS (Compose UI) targets (similar to kmp.jetbrains.com), run:

```bash
kmpli
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
kmpli --template="shared-ui"
kmpli --template="native-ui"
kmpli --template="library"
```

To generate a template configured with Amper (only available with Shared UI or Native UI). Pick one:

```
kmpli --template="shared-ui-amper"
kmpli --template="native-ui-amper"
```

### License

[Apache-2.0](LICENSE)

### Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

### Acknowledgments

- Inspired by a need to create Kotlin/Compose Multiplatform projects without the need for a GUI.
- Thanks to the Kotlin and Compose Multiplatform communities for the support and for the resources.