sealed class ProjectTemplate(
    val id: String,
    val description: String,
    val url: String
) {
    object SharedUi : ProjectTemplate(
        "shared-ui", "Shared UI App (Compose)",
        "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/main.zip"
    )

    object NativeUi : ProjectTemplate(
        "native-ui", "Native UI App (Compose + SwiftUI)",
        "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/main.zip"
    )

    object Library : ProjectTemplate(
        id = "library",
        description = "Bare-bones Multiplatform Library",
        url = "https://github.com/Kotlin/multiplatform-library-template/archive/refs/heads/main.zip"
    )

    object SharedUiAmper : ProjectTemplate(
        id = "shared-ui-amper",
        description = "Shared UI App (configured with Amper)",
        url = "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/amper.zip"
    )

    object NativeUiAmper : ProjectTemplate(
        id = "native-ui-amper",
        description = "Native UI App (configured with Amper)",
        url = "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/amper.zip"
    )

    companion object {
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
