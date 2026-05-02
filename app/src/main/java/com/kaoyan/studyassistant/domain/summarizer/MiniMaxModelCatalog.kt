package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.datastore.AppPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * MiniMax 模型目录。
 *
 * MiniMax 国内兼容端点使用 https://api.minimaxi.com/v1
 * 当前稳定做法是直接使用预设模型列表，避免依赖未开放或不稳定的 /models 接口。
 */
@Singleton
class MiniMaxModelCatalog @Inject constructor() : ProviderModelCatalog {

    override val provider: AiProvider = AiProvider.MINIMAX

    override suspend fun load(settings: AppPreferences.AiSettings): ModelCatalogResult {
        return ModelCatalogResult(
            provider = provider,
            options = presetOptions(),
            message = "MiniMax 模型列表已就绪，推荐使用 MiniMax-M2.5 或 MiniMax-M2.7。",
            usedFallback = false
        )
    }

    override fun presetOptions(): List<AiModelOption> = listOf(
        preset(id = "MiniMax-M2.7", label = "MiniMax-M2.7（旗舰）", recommended = true, latest = true),
        preset(id = "MiniMax-M2.7-highspeed", label = "MiniMax-M2.7-highspeed（高速旗舰）", latest = true),
        preset(id = "MiniMax-M2.5", label = "MiniMax-M2.5（均衡推荐）", recommended = true),
        preset(id = "MiniMax-M2.5-highspeed", label = "MiniMax-M2.5-highspeed（高速均衡）"),
        preset(id = "MiniMax-M2.1", label = "MiniMax-M2.1（经济）"),
        preset(id = "M2-her", label = "M2-her（角色对话专用）")
    )

    private fun preset(
        id: String,
        label: String,
        recommended: Boolean = false,
        latest: Boolean = false
    ) = AiModelOption(
        id = id,
        label = label,
        provider = provider,
        isRecommended = recommended,
        isLatest = latest,
        isPreview = false,
        source = AiModelSource.PRESET
    )
}
