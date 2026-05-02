package com.kaoyan.studyassistant.domain.summarizer

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSummaryJsonParser @Inject constructor() {

    fun parse(rawContent: String): AiSummaryOutput {
        val jsonText = extractJsonObjectText(stripCodeFence(rawContent))
        val root = try {
            JsonParser.parseString(jsonText)
        } catch (e: Exception) {
            throw IllegalStateException("非法 JSON：${e.message}", e)
        }

        if (!root.isJsonObject) {
            throw IllegalStateException("JSON 顶层结构不是对象")
        }

        val obj = root.asJsonObject
        val summary = obj.readString("summary").normalizeText(maxLength = 220)
        if (summary.isBlank()) {
            throw IllegalStateException("缺少 summary")
        }

        return AiSummaryOutput(
            summary = summary,
            strengths = obj.readStringList("strengths"),
            problems = obj.readStringList("problems"),
            suggestions = obj.readStringList("suggestions"),
            tomorrowFocus = obj.readStringList("tomorrowFocus"),
            confidence = obj.readString("confidence").ifBlank { "中" }.normalizeConfidence(),
            tone = obj.readString("tone").ifBlank { "克制鼓励" }.normalizeText(maxLength = 16)
        )
    }

    fun stripCodeFence(rawContent: String): String {
        // 先清洗思考过程标签（<think>...</think> 等），再处理代码围栏
        val cleaned = AiResponseCleaner.clean(rawContent)
        val trimmed = cleaned.trim()
        if (!trimmed.startsWith("```")) return trimmed

        return trimmed
            .replace(Regex("^```(?:json|JSON)?\\s*"), "")
            .replace(Regex("\\s*```$"), "")
            .trim()
    }

    private fun extractJsonObjectText(content: String): String {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            return trimmed
        }

        val startIndex = trimmed.indexOf('{')
        val endIndex = trimmed.lastIndexOf('}')
        if (startIndex >= 0 && endIndex > startIndex) {
            return trimmed.substring(startIndex, endIndex + 1)
        }

        throw IllegalStateException("返回内容不是 JSON 对象")
    }

    private fun JsonObject.readString(fieldName: String): String {
        val value = get(fieldName) ?: return ""
        return when {
            value.isJsonNull -> ""
            value.isJsonPrimitive -> runCatching { value.asString }.getOrDefault(value.toString())
            else -> value.toString()
        }.trim()
    }

    private fun JsonObject.readStringList(fieldName: String): List<String> {
        val value = get(fieldName) ?: return emptyList()
        val items = when {
            value.isJsonNull -> emptyList()
            value.isJsonArray -> value.asJsonArray.mapNotNull { it.asReadableTextOrNull() }
            value.isJsonPrimitive -> splitCompositeItem(value.asString)
            else -> emptyList()
        }

        return items
            .map { it.normalizeText(maxLength = 36) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(3)
    }

    private fun JsonElement.asReadableTextOrNull(): String? {
        return when {
            isJsonNull -> null
            isJsonPrimitive -> runCatching { asString }.getOrNull()
            else -> null
        }
    }

    private fun splitCompositeItem(item: String): List<String> {
        return item
            .split(Regex("[；;\\n]"))
            .map { it.trim().trim('、', '-', '•', ' ') }
            .filter { it.isNotBlank() }
    }

    private fun String.normalizeText(maxLength: Int): String {
        val normalized = replace(Regex("\\s+"), " ")
            .replace("```", "")
            .trim()
            .trim('，', '。', '；', ';', ':', '：', ' ')
        return if (normalized.length <= maxLength) normalized else normalized.take(maxLength).trim()
    }

    private fun String.normalizeConfidence(): String {
        return when {
            contains("高") -> "高"
            contains("低") -> "低"
            else -> "中"
        }
    }
}
