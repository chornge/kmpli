![CI/CD](https://github.com/chornge/kmpli/actions/workflows/build.yml/badge.svg?branch=main)

# Kmpli

Pronounced `"Comply"`

A command-line interface (CLI) designed to streamline the creation of new Kotlin/Compose Multiplatform
projects. It offers a flexible way to generate customized project configurations and directory structures, including
test setups.

### Requirements

Kotlin or Compose Multiplatform development environment set up (JDK-17 and Gradle).

### Installation

Clone repository & build project using Gradle:

```
git clone https://github.com/chornge/kmpli.git
cd kmpli
chmod +x kmpli
gradle clean build installDist
```

### Usage

To generate Android & IOS (Compose UI) targets with tests (default behavior on kmp.jetbrains.com), run:

```
./kmpli
```

#### Usage Options

```
  --name TEXT       Project name
  --pid TEXT        Project ID
  --template TEXT   Multiplatform template
                    shared-ui → Shared UI App (Compose)
                    native-ui → Native UI App (Compose + SwiftUI) 
                    library → Bare-bones Multiplatform Library
                    shared-ui-amper → Shared UI App (configured with Amper)
                    native-ui-amper → Native UI App (configured with Amper)
  --platforms TEXT  Target platform(s)
                    android,ios(swiftui),web(react),desktop,server
  --include-tests   Include Tests (false if a platform is specified)
  -h, --help        Show this message and exit
```

Examples (platforms or template):

```
./kmpli --name="CMPProject" --pid="io.chornge.cmpproject" --platforms="android,ios(swiftui),desktop,web(react),server" --include-tests
```

Generate only IOS (with Compose UI), Desktop, Web (with Compose UI) & Server. Exclude tests:

```
./kmpli --name="CMPProject" --pid="io.chornge.cmpproject" --platforms="ios,desktop,web,server"
```

Generate a template gallery project - Barebones KMP library or Shared UI or Native UI:

```
./kmpli --template="shared-ui"
./kmpli --template="native-ui"
./kmpli --template="library" --name="LibraryProject" --pid="io.chornge.libraryproject"
```

Generate a template with the Amper build system:

```
./kmpli --template="shared-ui-amper"
./kmpli --template="native-ui-amper" --name="NativeUIAmper" --pid="io.chornge.nativeuiamper"
```

### License

[Apache 2.0](LICENSE)

### Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

### Acknowledgments

- Inspired by a need to create Kotlin/Compose Multiplatform projects without the need for a GUI.
- Thanks to the Kotlin and Compose Multiplatform communities for the support and for the resources.