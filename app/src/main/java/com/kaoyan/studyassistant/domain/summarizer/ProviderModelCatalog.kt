package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.datastore.AppPreferences

interface ProviderModelCatalog {
    val provider: AiProvider
    suspend fun load(settings: AppPreferences.AiSettings): ModelCatalogResult
    fun presetOptions(): List<AiModelOption>
}
