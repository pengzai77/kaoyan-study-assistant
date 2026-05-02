package com.kaoyan.studyassistant.data.remote.ai

import com.google.gson.annotations.SerializedName

data class OpenAiLikeChatRequest(
    val model: String,
    val messages: List<OpenAiLikeMessage>,
    val temperature: Double = 0.2,
    @SerializedName("response_format")
    val responseFormat: OpenAiLikeResponseFormat? = null,
    val stream: Boolean = false,
    @SerializedName("extra_body")
    val extraBody: Map<String, @JvmSuppressWildcards Any>? = null
)

data class OpenAiLikeMessage(
    val role: String,
    val content: String
)

data class OpenAiLikeResponseFormat(
    val type: String
)

data class OpenAiLikeChatResponse(
    val choices: List<OpenAiLikeChoice> = emptyList(),
    val error: OpenAiLikeError? = null
)

data class OpenAiLikeChoice(
    val message: OpenAiLikeMessageResponse? = null
)

data class OpenAiLikeMessageResponse(
    val role: String? = null,
    val content: String? = null,
    /**
     * MiniMax 专有字段：独立的思考过程内容。
     * 当模型开启 thinking 模式时，思考过程会放在此字段，
     * 最终回答放在 content 字段。
     * 其他模型此字段为 null，不影响正常解析。
     */
    @SerializedName("thinking_content")
    val thinkingContent: String? = null
)

data class OpenAiLikeModelsResponse(
    val data: List<OpenAiLikeModelDto> = emptyList()
)

data class OpenAiLikeModelDto(
    val id: String,
    @SerializedName("owned_by")
    val ownedBy: String? = null
)

data class OpenAiLikeErrorEnvelope(
    val error: OpenAiLikeError? = null
)

data class OpenAiLikeError(
    val message: String? = null,
    val type: String? = null
)
