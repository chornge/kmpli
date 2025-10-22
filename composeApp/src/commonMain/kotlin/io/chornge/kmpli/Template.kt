sealed class Template(
    val id: String,
    val description: String,
    val url: String
) {
    object SharedUi : Template(
        "shared-ui", "Shared UI App (Compose)",
        "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/main.zip"
    )

    object NativeUi : Template(
        "native-ui", "Native UI App (Compose + SwiftUI)",
        "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/main.zip"
    )

    object Library : Template(
        id = "library",
        description = "Bare-bones Multiplatform Library",
        url = "https://github.com/Kotlin/multiplatform-library-template/archive/refs/heads/main.zip"
    )

    object SharedUiAmper : Template(
        id = "shared-ui-amper",
        description = "Shared UI App (configured with Amper)",
        url = "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/amper.zip"
    )

    object NativeUiAmper : Template(
        id = "native-ui-amper",
        description = "Native UI App (configured with Amper)",
        url = "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/amper.zip"
    )

    companion object Companion {
        fun fromId(id: String) = listOf(SharedUi, NativeUi, Library, SharedUiAmper, NativeUiAmper).find {
            it.id.equals(
                id,
                ignoreCase = true
            )
        }

        fun availableTemplates() =
            listOf(SharedUi, NativeUi, Library, SharedUiAmper, NativeUiAmper).joinToString(", ") { it.id }
    }
}
