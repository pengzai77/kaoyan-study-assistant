package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeApiFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomModelCatalog @Inject constructor(
    private val apiFactory: OpenAiLikeApiFactory
) : ProviderModelCatalog {
    override val provider: AiProvider = AiProvider.DEEPSEEK

    override suspend fun load(settings: AppPreferences.AiSettings): ModelCatalogResult {
        if (settings.apiKey.isBlank()) {
            return ModelCatalogResult(
                provider = provider,
                options = presetOptions(),
                message = "未填写 API Key，先显示 DeepSeek 预设模型。仍可手动输入模型名。",
                usedFallback = true
            )
        }

        return runCatching {
            val service = apiFactory.createService(settings.baseUrl, settings.apiKey, settings.timeoutSeconds)
            val response = service.listModels()
            if (!response.isSuccessful) {
                error("HTTP ${response.code()} ${response.message()}")
            }
            val models = response.body().orEmptyModels()
                .map { it.id.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()

            if (models.isEmpty()) {
                error("DeepSeek 接口没有返回模型列表")
            }

            ModelCatalogResult(
                provider = provider,
                options = models.map { modelId ->
                    AiModelOption(
                        id = modelId,
                        label = modelId,
                        provider = provider,
                        isRecommended = modelId.contains("deepseek", ignoreCase = true) &&
                            (modelId.contains("flash", ignoreCase = true) || modelId.contains("chat", ignoreCase = true)),
                        isLatest = modelId.contains("latest", ignoreCase = true),
                        isPreview = modelId.contains("preview", ignoreCase = true) ||
                            modelId.contains("reasoner", ignoreCase = true),
                        source = AiModelSource.REMOTE
                    )
                },
                message = "已从 DeepSeek 接口拉取模型列表。"
            )
        }.getOrElse { error ->
            ModelCatalogResult(
                provider = provider,
                options = presetOptions(),
                message = "DeepSeek 接口未提供模型列表，已回退到预设模型，你仍可手动输入模型名。",
                errorMessage = error.message,
                usedFallback = true
            )
        }
    }

    override fun presetOptions(): List<AiModelOption> {
        return listOf(
            preset("deepseek-v4-flash", label = "deepseek-v4-flash（推荐）", recommended = true, latest = true),
            preset("deepseek-v4-pro", label = "deepseek-v4-pro（高质量）", latest = true, preview = true),
            preset("deepseek-chat", label = "deepseek-chat（兼容旧版）"),
            preset("deepseek-reasoner", label = "deepseek-reasoner（推理旧版）", preview = true)
        )
    }

    private fun preset(
        id: String,
        label: String = id,
        recommended: Boolean = false,
        latest: Boolean = false,
        preview: Boolean = false
    ) = AiModelOption(
        id = id,
        label = label,
        provider = provider,
        isRecommended = recommended,
        isLatest = latest,
        isPreview = preview,
        source = AiModelSource.PRESET
    )
}
