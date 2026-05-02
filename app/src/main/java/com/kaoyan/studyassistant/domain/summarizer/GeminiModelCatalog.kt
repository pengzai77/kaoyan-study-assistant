package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.datastore.AppPreferences
import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeApiFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiModelCatalog @Inject constructor(
    private val apiFactory: OpenAiLikeApiFactory
) : ProviderModelCatalog {
    override val provider: AiProvider = AiProvider.MIMO

    override suspend fun load(settings: AppPreferences.AiSettings): ModelCatalogResult {
        if (settings.apiKey.isBlank()) {
            return ModelCatalogResult(
                provider = provider,
                options = presetOptions(),
                message = "未填写 API Key，先显示小米 Mimo 预设模型。你也可以手动输入模型名。",
                usedFallback = true
            )
        }

        if (settings.baseUrl.isBlank()) {
            return ModelCatalogResult(
                provider = provider,
                options = presetOptions(),
                message = "未填写小米 Mimo 接口地址，先显示预设模型。",
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
                error("小米 Mimo 接口没有返回可用模型列表")
            }

            ModelCatalogResult(
                provider = provider,
                options = models.map { modelId ->
                    AiModelOption(
                        id = modelId,
                        label = modelId,
                        provider = provider,
                        isRecommended = modelId.contains("mimo", ignoreCase = true),
                        isLatest = modelId.contains("latest", ignoreCase = true),
                        isPreview = modelId.contains("preview", ignoreCase = true),
                        source = AiModelSource.REMOTE
                    )
                },
                message = "已从小米 Mimo 接口拉取模型列表。"
            )
        }.getOrElse { error ->
            ModelCatalogResult(
                provider = provider,
                options = presetOptions(),
                message = "小米 Mimo 模型获取失败，已回退到预设列表。你仍然可以手动输入模型名。",
                errorMessage = error.message,
                usedFallback = true
            )
        }
    }

    override fun presetOptions(): List<AiModelOption> {
        return emptyList()
    }
}
