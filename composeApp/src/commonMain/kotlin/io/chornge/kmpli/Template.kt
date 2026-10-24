// Template archives are fetched from codeload.github.com rather than the more familiar
// github.com/<owner>/<repo>/archive/refs/heads/<branch>.zip form. The github.com URL answers
// with a redirect carrying a content-security-policy header around 9 KB, which is larger than
// the curl engine's per-header line limit, so every --template run failed with "Header line
// length limit exceeded". codeload is the host that redirect points at; it serves the same
// archive, rooted at the same "<repo>-<branch>" folder, with headers under 100 bytes.
sealed class Template(
    val id: String,
    val description: String,
    val url: String,
    val oldPackageId: String
) {
    object SharedUi : Template(
        "shared-ui", "Shared UI App (Compose)",
        "https://codeload.github.com/Kotlin/KMP-App-Template/zip/refs/heads/main",
        "com.jetbrains.kmpapp"
    )

    object NativeUi : Template(
        "native-ui", "Native UI App (Compose + SwiftUI)",
        "https://codeload.github.com/Kotlin/KMP-App-Template-Native/zip/refs/heads/main",
        "com.jetbrains.kmpapp"
    )

    object Library : Template(
        id = "library",
        description = "Bare-bones Multiplatform Library",
        url = "https://codeload.github.com/Kotlin/multiplatform-library-template/zip/refs/heads/main",
        oldPackageId = "io.github.kotlin.fibonacci"
    )

    // JetBrains replaced Amper with the Kotlin Toolchain in the Multiplatform Wizard, and the
    // upstream "amper" branches were removed. The declarative module.yaml/project.yaml layout
    // carried over, so these are the direct successors.
    object SharedUiToolchain : Template(
        id = "shared-ui-toolchain",
        description = "Shared UI App (configured with Kotlin Toolchain)",
        url = "https://codeload.github.com/Kotlin/KMP-App-Template/zip/refs/heads/kotlin-toolchain",
        oldPackageId = "com.jetbrains.kmpapp"
    )

    object NativeUiToolchain : Template(
        id = "native-ui-toolchain",
        description = "Native UI App (configured with Kotlin Toolchain)",
        url = "https://codeload.github.com/Kotlin/KMP-App-Template-Native/zip/refs/heads/kotlin-toolchain",
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
