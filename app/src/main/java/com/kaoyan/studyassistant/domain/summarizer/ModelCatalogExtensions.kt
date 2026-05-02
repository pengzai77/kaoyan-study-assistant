package com.kaoyan.studyassistant.domain.summarizer

import com.kaoyan.studyassistant.data.remote.ai.OpenAiLikeModelsResponse

internal fun OpenAiLikeModelsResponse?.orEmptyModels() = this?.data.orEmpty()
