sealed class Template(
    val id: String,
    val description: String,
    val url: String,
    val oldPackageId: String
) {
    object SharedUi : Template(
        "shared-ui", "Shared UI App (Compose)",
        "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/main.zip",
        "com.jetbrains.kmpapp"
    )

    object NativeUi : Template(
        "native-ui", "Native UI App (Compose + SwiftUI)",
        "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/main.zip",
        "com.jetbrains.kmpapp"
    )

    object Library : Template(
        id = "library",
        description = "Bare-bones Multiplatform Library",
        url = "https://github.com/Kotlin/multiplatform-library-template/archive/refs/heads/main.zip",
        oldPackageId = "io.github.kotlin.fibonacci"
    )

    // JetBrains replaced Amper with the Kotlin Toolchain in the Multiplatform Wizard, and the
    // upstream "amper" branches were removed. The declarative module.yaml/project.yaml layout
    // carried over, so these are the direct successors.
    object SharedUiToolchain : Template(
        id = "shared-ui-toolchain",
        description = "Shared UI App (configured with Kotlin Toolchain)",
        url = "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/kotlin-toolchain.zip",
        oldPackageId = "com.jetbrains.kmpapp"
    )

    object NativeUiToolchain : Template(
        id = "native-ui-toolchain",
        description = "Native UI App (configured with Kotlin Toolchain)",
        url = "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/kotlin-toolchain.zip",
        oldPackageId = "com.jetbrains.kmpapp"
    )

    companion object Companion {
        fun fromId(id: String) = listOf(SharedUi, NativeUi, Library, SharedUiToolchain, NativeUiToolchain).find {
            it.id.equals(
                id,
                ignoreCase = true
            )
        }

        fun availableTemplates() =
            listOf(SharedUi, NativeUi, Library, SharedUiToolchain, NativeUiToolchain).joinToString(", ") { it.id }
    }
}
