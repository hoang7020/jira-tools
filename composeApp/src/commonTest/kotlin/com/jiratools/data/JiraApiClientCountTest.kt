package com.jiratools.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JiraApiClientCountTest {
    @Test
    fun demThanhCongTraVeSoLuong() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content = """{"count":153}""",
                        status = HttpStatusCode.OK,
                        headers = JSON_HEADERS,
                    )
                }

            assertEquals(153, client(engine).approximateCount(jql = BASE_JQL))
        }

    @Test
    fun loiHttpTraVeNull() =
        runTest {
            val engine = MockEngine { respondError(status = HttpStatusCode.Forbidden) }

            assertNull(client(engine).approximateCount(jql = BASE_JQL))
        }

    @Test
    fun loiMangTraVeNull() =
        runTest {
            val engine = MockEngine { throw HttpRequestTimeoutException("hết thời gian chờ") }

            assertNull(client(engine).approximateCount(jql = BASE_JQL))
        }

    private fun client(engine: MockEngine): JiraApiClient =
        JiraApiClient(
            client = HttpClientFactory.create(engine),
            credentials = { JiraCredentials(baseUrl = BASE_URL, email = "dev@congty.vn", token = "api-token-bi-mat") },
        )

    private companion object {
        const val BASE_URL = "https://congty.atlassian.net"
        const val BASE_JQL = "project = HSP ORDER BY updated DESC"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
