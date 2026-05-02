package com.kaoyan.studyassistant.data.remote.ai

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface OpenAiLikeApiService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Body request: OpenAiLikeChatRequest
    ): Response<OpenAiLikeChatResponse>

    @GET("models")
    suspend fun listModels(): Response<OpenAiLikeModelsResponse>
}
