package com.kaoyan.studyassistant.data.remote.ai

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OpenAiLikeApiFactory @Inject constructor(
    private val gson: Gson
) {
    /**
     * 使用 lenient Gson 解析器，防止服务端返回 HTML 错误页或非标准 JSON 时
     * 抛出 MalformedJsonException。
     * lenient 模式下，Gson 会尽力解析，解析失败时返回 null 而不是抛异常。
     */
    private val lenientGson: Gson = GsonBuilder()
        .setLenient()
        .create()

    fun createService(
        baseUrl: String,
        apiKey: String,
        timeoutSeconds: Int
    ): OpenAiLikeApiService {
        return createRetrofit(
            baseUrl = baseUrl,
            timeoutSeconds = timeoutSeconds,
            apiKey = apiKey
        ).create(OpenAiLikeApiService::class.java)
    }

    fun createRetrofit(
        baseUrl: String,
        timeoutSeconds: Int,
        apiKey: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(createClient(timeoutSeconds, apiKey, extraHeaders))
            // 使用 lenient Gson，防止服务端返回非标准 JSON 时崩溃
            .addConverterFactory(GsonConverterFactory.create(lenientGson))
            .build()
    }

    fun createClient(
        timeoutSeconds: Int,
        apiKey: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds.toLong(), TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val builder = chain.request()
                    .newBuilder()
                    .header("Accept", "application/json")

                if (chain.request().body != null) {
                    builder.header("Content-Type", "application/json")
                }
                if (!apiKey.isNullOrBlank()) {
                    builder.header("Authorization", "Bearer $apiKey")
                }
                extraHeaders.forEach { (key, value) ->
                    builder.header(key, value)
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    fun normalizeBaseUrl(baseUrl: String): String {
        val normalized = baseUrl.trim().let { if (it.endsWith("/")) it else "$it/" }
        return normalized.toHttpUrlOrNull()?.toString()
            ?: throw IllegalArgumentException("Base URL 格式不正确：$baseUrl")
    }

    fun resolveUrl(
        baseUrl: String,
        relativePath: String,
        queryParams: Map<String, String> = emptyMap()
    ): HttpUrl {
        val base = normalizeBaseUrl(baseUrl).toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Base URL 格式不正确：$baseUrl")
        val cleanedPath = relativePath.trimStart('/')
        val builder = base.newBuilder().addEncodedPathSegments(cleanedPath)
        queryParams.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return builder.build()
    }

    fun newRequest(url: HttpUrl): Request = Request.Builder().url(url).build()
}
