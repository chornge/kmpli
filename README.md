![CI/CD](https://github.com/chornge/kmpli/actions/workflows/build.yml/badge.svg?branch=main)

# Kmpli

Pronounced `"Comply"`

A command-line interface (CLI) designed to streamline the creation of new Kotlin/Compose Multiplatform
projects. It offers a flexible way to generate customized project configurations and directory structures, including
test setups.

### Requirements

Kotlin and Compose Multiplatform development environment set up (JDK-17 and Gradle).

### Installation

Clone repository & build project using gradle:

```
git clone https://github.com/chornge/kmpli.git
cd kmpli
gradle build installDist
```

### Usage

```
./kmpli
```

#### Usage Options

The default targets are Android & IOS (Compose UI framework) excluding tests.

```
 -n     --name TEXT     Project name (default: KotlinProject)
 -p     --pid TEXT      Project ID (default: org.example.project)
 -a     --android       Include Android (default: true)
 -i     --ios           Include iOS (default: true)
 -iu    --iosui TEXT    iOS UI framework (default: compose)
 -d     --desktop       Include Desktop (default: false)
 -w     --web           Include Web (default: false)
 -wu    --webui TEXT    Web UI framework (default: compose)
 -s     --server        Include Server (default: false)
 -t     --tests         Include Tests (default: false)
 -h,    --help          Show this message and exit
```

Include all targets (IOS with SwiftUI, Web with React). Include tests:

```
./kmpli --name “CMPProject" --pid “io.chornge.cmpproject" --android --ios "swiftui" --desktop --web "react" --server --tests
```

OR using short options (exact same as above):

```
./kmpli -n "CMPProject" -p "io.chornge.cmpproject" -a -i "swiftui" -d -w "react" -t
```

Include only IOS (with Compose UI), Desktop, Web (with Compose UI) & Server. Exclude tests:

```
./kmpli --name "CMPProject" --pid "io.chornge.cmpproject" --ios --desktop --web --server
```

### License

[MIT](LICENSE)

### Contributing

Contributions are welcome! Please fork the repository and create a pull request with your changes.

### Acknowledgments

- Inspired by the need for a streamlined way to create Kotlin/Compose Multiplatform projects.
- Thanks to the Kotlin and Compose Multiplatform communities for their support and resources.