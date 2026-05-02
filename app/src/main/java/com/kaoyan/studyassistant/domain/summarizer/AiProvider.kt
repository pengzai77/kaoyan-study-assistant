package com.kaoyan.studyassistant.domain.summarizer

enum class AiProvider(
    val displayName: String,
    val defaultBaseUrl: String,
    val defaultModel: String
) {
    MIMO(
        displayName = "小米 Mimo",
        defaultBaseUrl = "https://api.xiaomimimo.com/v1",
        defaultModel = ""
    ),
    MINIMAX(
        displayName = "MiniMax",
        defaultBaseUrl = "https://api.minimaxi.com/v1",
        defaultModel = "MiniMax-M2.5"
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        defaultBaseUrl = "https://api.deepseek.com",
        defaultModel = "deepseek-v4-flash"
    );

    companion object {
        fun fromStorageValue(value: String?): AiProvider {
            return when (value) {
                "QWEN" -> MINIMAX
                "GEMINI" -> MIMO
                "CUSTOM" -> DEEPSEEK
                else -> entries.firstOrNull { it.name == value } ?: MIMO
            }
        }
    }
}
