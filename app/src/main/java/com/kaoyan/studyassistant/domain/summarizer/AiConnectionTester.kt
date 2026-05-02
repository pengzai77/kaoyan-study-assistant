package com.kaoyan.studyassistant.domain.summarizer

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeApiFactory
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeChatRequest
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeErrorEnvelope
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeMessage
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

data class AiConnectionTestResult(
    val summaryCapabilityReady: Boolean,
    val message: String
)

private data class ProbeExecution(
    val content: String,
    val usedJsonMode: Boolean
)

@Singleton
class AiConnectionTester @Inject constructor(
    private val apiFactory: OpenAiLikeApiFactory,
    private val gson: Gson,
    private val jsonParser: AiSummaryJsonParser
) {
    suspend fun test(settings: AppPreferences.AiSettings): AiConnectionTestResult {
        require(settings.apiKey.isNotBlank()) { "请先填写 API Key" }
        require(settings.baseUrl.isNotBlank()) { "请先填写 Base URL" }
        require(settings.model.isNotBlank()) { "请先填写模型名" }

        val connectionUsedJsonMode = runConnectionProbe(settings)

        return try {
            val summaryUsedJsonMode = runSummaryProbe(settings)
            val message = buildString {
                append("基础连接成功，总结能力可用")
                if (!connectionUsedJsonMode || !summaryUsedJsonMode) {
                    append("（已自动关闭严格 JSON mode 以兼容当前模型）")
                }
            }
            AiConnectionTestResult(summaryCapabilityReady = true, message = message)
        } catch (e: Exception) {
            val reason = e.message?.trim().orEmpty().ifBlank { "未知错误" }.take(180)
            AiConnectionTestResult(
                summaryCapabilityReady = false,
                message = "基础连接成功，但总结能力测试失败：$reason"
            )
        }
    }

    private suspend fun runConnectionProbe(settings: AppPreferences.AiSettings): Boolean {
        val responseFormatEnabled = AiRequestPolicy.responseFormatOrNull(settings) != null
        val probe = executeChatRequest(
            settings = settings,
            systemPrompt = "你是连接测试助手，只返回合法 JSON。",
            userPrompt = """请只返回 {"status":"ok","model":"${settings.model}"}""",
            responseFormatEnabled = responseFormatEnabled,
            allowJsonModeRetry = true
        )

        val json = jsonParser.stripCodeFence(probe.content)
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrElse {
            throw IllegalStateException("基础连接测试返回的不是合法 JSON：${it.message}", it)
        }
        val status = root.get("status")?.asString?.trim().orEmpty()
        if (status.lowercase() != "ok") {
            throw IllegalStateException("基础连接测试返回了非预期结果：$json")
        }
        return probe.usedJsonMode
    }

    private suspend fun runSummaryProbe(settings: AppPreferences.AiSettings): Boolean {
        val responseFormatEnabled = AiRequestPolicy.responseFormatOrNull(settings) != null
        val probe = executeChatRequest(
            settings = settings,
            systemPrompt = """
                你是一位考研学习教练。
                只输出一个合法 JSON 对象，不要输出 Markdown，不要输出额外解释。
                JSON 结构固定为：
                {
                  "summary": "字符串",
                  "problems": ["字符串"],
                  "tomorrowFocus": ["字符串"]
                }
                summary 必须非空，problems 和 tomorrowFocus 也必须各至少 1 项。
            """.trimIndent(),
            userPrompt = """
                请根据下面的简短样本输出总结 JSON：
                - 范围：近7天
                - 总学习时长：18小时
                - 覆盖天数：6天
                - 高投入科目：数学、英语
                - 高频问题：拖延、做题慢、复盘断层
                - 规则判断：投入不低，但节奏波动较大，最拖后腿的是复盘跟进不稳定。
            """.trimIndent(),
            responseFormatEnabled = responseFormatEnabled,
            allowJsonModeRetry = true
        )

        val parsed = jsonParser.parse(probe.content)
        if (parsed.problems.isEmpty() || parsed.tomorrowFocus.isEmpty()) {
            throw IllegalStateException("返回 JSON 缺少 problems 或 tomorrowFocus")
        }
        return probe.usedJsonMode
    }

    private suspend fun executeChatRequest(
        settings: AppPreferences.AiSettings,
        systemPrompt: String,
        userPrompt: String,
        responseFormatEnabled: Boolean,
        allowJsonModeRetry: Boolean
    ): ProbeExecution {
        val response = apiFactory
            .createService(settings.baseUrl, settings.apiKey, settings.timeoutSeconds)
            .createChatCompletion(
                OpenAiLikeChatRequest(
                    model = settings.model,
                    messages = listOf(
                        OpenAiLikeMessage(role = "system", content = systemPrompt),
                        OpenAiLikeMessage(role = "user", content = userPrompt)
                    ),
                    temperature = 0.1,
                    responseFormat = if (responseFormatEnabled) AiRequestPolicy.responseFormatOrNull(settings) else null,
                    stream = false
                )
            )

        if (!response.isSuccessful) {
            val errorMessage = extractErrorMessage(response)
            if (allowJsonModeRetry && AiRequestPolicy.shouldRetryWithoutJsonMode(errorMessage, responseFormatEnabled)) {
                return executeChatRequest(
                    settings = settings,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    responseFormatEnabled = false,
                    allowJsonModeRetry = false
                )
            }
            throw IllegalStateException(errorMessage)
        }

        val content = response.body()
            ?.choices
            ?.firstOrNull()
            ?.message
            ?.content
            ?.trim()
            .orEmpty()

        if (content.isBlank()) {
            throw IllegalStateException("测试请求没有返回内容")
        }

        return ProbeExecution(content = content, usedJsonMode = responseFormatEnabled)
    }

    /**
     * 安全提取错误信息。
     *
     * 当服务端返回 HTML 错误页（如 404 页面）时，Gson 会抛出 MalformedJsonException。
     * 此方法通过 runCatching 捕获所有解析异常，回退到原始文本截断显示，
     * 确保错误信息始终以可读字符串形式呈现，而不是崩溃。
     */
    private fun extractErrorMessage(response: Response<*>): String {
        val rawBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")

        // 尝试将 errorBody 解析为 OpenAI 标准错误格式
        val parsedMessage = runCatching {
            gson.fromJson(rawBody, OpenAiLikeErrorEnvelope::class.java)?.error?.message
        }.getOrNull()

        if (!parsedMessage.isNullOrBlank()) {
            return "HTTP ${response.code()} - $parsedMessage"
        }

        // 如果是 HTML 页面（404 页面等），只截取前 120 字符避免刷屏
        val bodySnippet = rawBody
            .replace(Regex("<[^>]+>"), "") // 去掉 HTML 标签
            .replace(Regex("\\s+"), " ")   // 合并空白
            .trim()
            .take(120)

        return if (bodySnippet.isNotBlank()) {
            "HTTP ${response.code()} ${response.message()} - $bodySnippet"
        } else {
            "HTTP ${response.code()} ${response.message()}"
        }
    }
}
