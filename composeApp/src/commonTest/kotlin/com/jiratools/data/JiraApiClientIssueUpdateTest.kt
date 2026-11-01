package com.jiratools.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JiraApiClientIssueUpdateTest {
    @Test
    fun updatesIssueByIdWithOnlyDueDateAndAcceptsNoContent() =
        runTest {
            var requestPath: String? = null
            var requestMethod: HttpMethod? = null
            var requestBody: String? = null
            var authorization: String? = null
            val engine =
                MockEngine { request ->
                    requestPath = request.url.encodedPath
                    requestMethod = request.method
                    requestBody = request.body.asText()
                    authorization = request.headers[HttpHeaders.Authorization]
                    respond(content = "", status = HttpStatusCode.NoContent)
                }

            val result = client(engine).updateDueDate(issueIdOrKey = "10001", dueDate = "2026-11-20")

            assertIs<ApiResult.Success<Unit>>(result)
            assertEquals("/rest/api/3/issue/10001", requestPath)
            assertEquals(HttpMethod.Put, requestMethod)
            val payload = Json.parseToJsonElement(requireNotNull(requestBody)).jsonObject
            assertEquals(setOf("fields"), payload.keys)
            val fields = payload.getValue("fields").jsonObject
            assertEquals(setOf("duedate"), fields.keys)
            assertEquals("2026-11-20", fields.getValue("duedate").jsonPrimitive.content)
            assertTrue(requireNotNull(authorization).startsWith("Basic "))
        }

    @Test
    fun issueKeyCanBeUsedWhenIdIsUnavailable() =
        runTest {
            var requestPath: String? = null
            val engine =
                MockEngine { request ->
                    requestPath = request.url.encodedPath
                    respond(content = "", status = HttpStatusCode.NoContent)
                }

            client(engine).updateDueDate(issueIdOrKey = "HSP-1", dueDate = "2026-11-20")

            assertEquals("/rest/api/3/issue/HSP-1", requestPath)
        }

    @Test
    fun httpErrorsAndNetworkFailuresAreReturnedAsFailures() =
        runTest {
            val forbiddenEngine =
                MockEngine {
                    respond(content = "{}", status = HttpStatusCode.Forbidden, headers = JSON_HEADERS)
                }
            val networkEngine = MockEngine { throw IllegalStateException("network unavailable") }

            assertIs<ApiResult.Failure>(
                client(forbiddenEngine).updateDueDate(issueIdOrKey = "10001", dueDate = "2026-11-20"),
            )
            assertIs<ApiResult.Failure>(
                client(networkEngine).updateDueDate(issueIdOrKey = "10001", dueDate = "2026-11-20"),
            )
        }

    @Test
    fun invalidIssueIdentityOrDateDoesNotSendRequest() =
        runTest {
            var requestsMade = 0
            val engine =
                MockEngine {
                    requestsMade += 1
                    respond(content = "", status = HttpStatusCode.NoContent)
                }
            val api = client(engine)

            assertIs<ApiResult.Failure>(api.updateDueDate(issueIdOrKey = " ", dueDate = "2026-11-20"))
            assertIs<ApiResult.Failure>(api.updateDueDate(issueIdOrKey = "10001", dueDate = "2026-02-30"))

            assertEquals(0, requestsMade)
        }

    private fun OutgoingContent.asText(): String =
        when (this) {
            is TextContent -> text
            is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            else -> ""
        }

    private fun client(engine: MockEngine): JiraApiClient =
        JiraApiClient(
            client = HttpClientFactory.create(engine),
            credentials = { JiraCredentials(baseUrl = BASE_URL, email = "dev@example.com", token = "test-token") },
        )

    private companion object {
        const val BASE_URL = "https://example.atlassian.net"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
