package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeResponseFormat

object AiRequestPolicy {
    private val jsonModeUnstableTokens = listOf(
        "reasoning",
        "thinking",
        "reasoner",
        "qwq",
        "qvq",
        "deepseek-r1",
        "o1",
        "o3"
    )

    fun responseFormatOrNull(settings: AppPreferences.AiSettings): OpenAiLikeResponseFormat? {
        return if (shouldForceJsonMode(settings.provider, settings.model, settings.jsonMode)) {
            OpenAiLikeResponseFormat(type = "json_object")
        } else {
            null
        }
    }

    fun shouldForceJsonMode(
        provider: AiProvider,
        model: String,
        userSetting: Boolean
    ): Boolean {
        if (!userSetting) return false

        val normalizedModel = model.lowercase().trim()
        if (normalizedModel.isBlank()) return false
        if (jsonModeUnstableTokens.any { token -> normalizedModel.contains(token) }) {
            return false
        }

        if (provider == AiProvider.MIMO && normalizedModel.contains("preview-thinking")) {
            return false
        }

        return true
    }

    fun shouldRetryWithoutJsonMode(errorMessage: String, usedJsonMode: Boolean): Boolean {
        if (!usedJsonMode) return false
        val normalized = errorMessage.lowercase()
        return listOf(
            "response_format",
            "json_object",
            "json schema",
            "schema",
            "not support",
            "unsupported",
            "invalid parameter",
            "invalid request",
            "unrecognized",
            // MiniMax 特定错误提示
            "does not support response_format",
            "tool_choice"
        ).any { normalized.contains(it) }
    }
}
