![CI/CD](https://github.com/chornge/kmpli/actions/workflows/build.yml/badge.svg?branch=main)

# Kmpli

Pronounced `"Comply"`

A command-line interface (CLI) designed to streamline the creation of new Kotlin/Compose Multiplatform
projects. It offers a flexible way to generate customized project configurations and directory structures, including
configuring with Amper, and test setups.

### Requirements

Kotlin or Compose Multiplatform development environment set up (JDK-17 and Gradle).

### Installation

Clone repository & build project using Gradle:

```
git clone https://github.com/chornge/kmpli.git
cd kmpli
chmod +x kmpli gradlew
./gradlew clean build
```

### Usage

To generate Android & IOS (Compose UI) targets with tests (default behavior on kmp.jetbrains.com), run:

```
./kmpli
```

#### Usage Options

```
  --name TEXT       Project name (optional)
  --pid TEXT        Project ID (optional)
  --template TEXT   Multiplatform template
                    shared-ui → Shared UI App (Compose)
                    native-ui → Native UI App (Compose + SwiftUI) 
                    library → Bare-bones Multiplatform Library
                    shared-ui-amper → Shared UI App (configured with Amper)
                    native-ui-amper → Native UI App (configured with Amper)
  --platforms TEXT  Target platform(s)
                    android,ios(swiftui),web(react),desktop,server
  --include-tests   Include Tests (optional)
  -h, --help        Show this message and exit
```

To generate all the platforms (IOS with SwiftUI and Web with React/TS), with a name, project ID and with tests, run:

```
./kmpli --name="CMPProject" --pid="io.chornge.cmpproject" --platforms="android,ios(swiftui),desktop,web(react),server" --include-tests
```

To generate only IOS (with Compose UI), Desktop, Web (with Compose UI) & Server. Exclude tests:

```
./kmpli --name="CMPProject" --pid="io.chornge.cmpproject" --platforms="ios,desktop,web,server"
```

To generate a template project. Shared UI or Native UI or Barebones KMP library. Pick one:

```
./kmpli --template="shared-ui"
./kmpli --template="native-ui"
./kmpli --template="library"
```

To generate a template configured with Amper (only available with Shared UI or Native UI). Pick one:

```
./kmpli --template="shared-ui-amper"
./kmpli --template="native-ui-amper"
```

### License

[Apache-2.0](LICENSE)

### Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

### Acknowledgments

- Inspired by a need to create Kotlin/Compose Multiplatform projects without the need for a GUI.
- Thanks to the Kotlin and Compose Multiplatform communities for the support and for the resources.