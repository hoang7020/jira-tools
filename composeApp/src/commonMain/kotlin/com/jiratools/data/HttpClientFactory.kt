package com.jiratools.data

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/** Tạo [HttpClient] dùng chung cho ứng dụng; nhận engine tuỳ biến để kiểm thử bằng MockEngine. */
object HttpClientFactory {
    private const val REQUEST_TIMEOUT_MILLIS = 30_000L
    private const val CONNECT_TIMEOUT_MILLIS = 15_000L
    private const val SOCKET_TIMEOUT_MILLIS = 30_000L

    val json: Json =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    fun create(engine: HttpClientEngine? = null): HttpClient =
        if (engine == null) {
            HttpClient(configure())
        } else {
            HttpClient(engine, configure())
        }

    private fun configure(): HttpClientConfig<*>.() -> Unit =
        {
            // Ứng dụng tự đọc mã trạng thái để hiển thị thông báo tiếng Việt thay vì để Ktor ném lỗi.
            expectSuccess = false
            install(ContentNegotiation) {
                json(json)
            }
            install(HttpTimeout) {
                requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
                socketTimeoutMillis = SOCKET_TIMEOUT_MILLIS
            }
            defaultRequest {
                header(HttpHeaders.Accept, "application/json")
            }
        }
}
