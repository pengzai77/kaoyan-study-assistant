package com.kaoyan.studyassistant.domain.summarizer

enum class AiModelSource {
    REMOTE,
    PRESET
}

data class AiModelOption(
    val id: String,
    val label: String,
    val provider: AiProvider,
    val isRecommended: Boolean = false,
    val isLatest: Boolean = false,
    val isPreview: Boolean = false,
    val source: AiModelSource = AiModelSource.PRESET
) {
    val displayLabel: String
        get() {
            val tags = buildList {
                if (isRecommended) add("推荐")
                if (isLatest) add("Latest")
                if (isPreview) add("预览")
                if (source == AiModelSource.PRESET) add("预设")
            }
            return if (tags.isEmpty()) label else "$label  ·  ${tags.joinToString(" · ")}"
        }
}

data class ModelCatalogResult(
    val provider: AiProvider,
    val options: List<AiModelOption>,
    val message: String? = null,
    val errorMessage: String? = null,
    val usedFallback: Boolean = false
)
