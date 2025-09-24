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

The default targets are Android & IOS (Compose UI framework) including tests.
To generate a new project, navigate to the directory where you want to create the project and run:

```
./kmpli
```

#### Usage Options

```
 -n     --name TEXT     Project name (default: KotlinProject)
 -p     --pid TEXT      Project ID (default: org.example.project)
 -a     --android       Include Android (default: true)
 -i     --ios           Include iOS (default: true)
 -iu    --ios-ui TEXT    iOS UI framework (default: compose)
 -d     --desktop       Include Desktop (default: false)
 -w     --web           Include Web (default: false)
 -wu    --web-ui TEXT    Web UI framework (default: compose)
 -s     --server        Include Server (default: false)
 -t     --tests         Include Tests (default: false)
 -h,    --help          Show this message and exit
```

Examples:

```
./kmpli --name "CMPProject" --pid "io.chornge.cmpproject" --android --ios --ios-ui "swiftui" --desktop --web --web-ui "react" --server --tests
```

OR using short options (exact same as above):

```
./kmpli -n "CMPProject" -p "io.chornge.cmpproject" -a -i -iu "swiftui" -d -w -wu "react" -s -t
```

Generate only IOS (with Compose UI), Desktop, Web (with Compose UI) & Server. Exclude tests:

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