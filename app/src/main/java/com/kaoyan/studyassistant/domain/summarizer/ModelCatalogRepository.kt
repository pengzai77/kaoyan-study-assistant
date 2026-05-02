package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.datastore.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModelCatalogRepository @Inject constructor(
    mimoModelCatalog: GeminiModelCatalog,
    miniMaxModelCatalog: MiniMaxModelCatalog,
    customModelCatalog: CustomModelCatalog
) {
    private val catalogs: Map<AiProvider, ProviderModelCatalog> = mapOf(
        AiProvider.MIMO to mimoModelCatalog,
        AiProvider.MINIMAX to miniMaxModelCatalog,
        AiProvider.DEEPSEEK to customModelCatalog
    )

    private val cache = mutableMapOf<AiProvider, CacheEntry>()

    suspend fun load(settings: AppPreferences.AiSettings, forceRefresh: Boolean = false): ModelCatalogResult {
        val signature = "${settings.baseUrl}|${settings.apiKey}|${settings.provider}"
        if (!forceRefresh) {
            cache[settings.provider]?.takeIf { it.signature == signature }?.let { return it.result }
        }

        val catalog = catalogs.getValue(settings.provider)
        val loaded = catalog.load(settings)
        val result = loaded.copy(options = catalogScoped(settings.provider, loaded.options))
        cache[settings.provider] = CacheEntry(signature = signature, result = result)
        return result
    }

    fun presetOptions(provider: AiProvider): List<AiModelOption> {
        return catalogs.getValue(provider).presetOptions()
    }

    fun invalidate(provider: AiProvider) {
        cache.remove(provider)
    }

    private fun catalogScoped(provider: AiProvider, options: List<AiModelOption>): List<AiModelOption> {
        return options.filter { it.provider == provider }
    }

    private data class CacheEntry(
        val signature: String,
        val result: ModelCatalogResult
    )
}
