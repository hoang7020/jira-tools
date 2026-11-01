package com.jiratools.data

import com.jiratools.domain.JiraError
import com.jiratools.domain.QueryPage
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JiraApiClientSearchTest {
    @Test
    fun trangDauGuiDayDuThamSoVaKhongGuiToken() =
        runTest {
            var body: String? = null
            val engine =
                MockEngine { request ->
                    body = request.body.asText()
                    respond(content = PAGE_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }

            val result = client(engine).searchJql(jql = BASE_JQL)

            assertIs<ApiResult.Success<*>>(result)
            val payload = Json.parseToJsonElement(requireNotNull(body)).jsonObject
            assertEquals(BASE_JQL, payload["jql"]?.jsonPrimitive?.content)
            assertEquals("100", payload["maxResults"]?.jsonPrimitive?.content)
            val fields = payload["fields"].toString()
            listOf("summary", "status", "assignee", "priority", "updated", "timetracking", "duedate").forEach { field ->
                assertTrue(fields.contains("\"$field\""), "thiếu field $field trong $fields")
            }
            assertNull(payload["nextPageToken"])
        }

    @Test
    fun trangSauGuiNextPageToken() =
        runTest {
            var body: String? = null
            val engine =
                MockEngine { request ->
                    body = request.body.asText()
                    respond(content = PAGE_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }

            val result = client(engine).searchJql(jql = BASE_JQL, nextPageToken = "token-2")

            assertIs<ApiResult.Success<*>>(result)
            val payload = Json.parseToJsonElement(requireNotNull(body)).jsonObject
            assertEquals("token-2", payload["nextPageToken"]?.jsonPrimitive?.content)
        }

    @Test
    fun phanHoiDuocAnhXaThanhQueryPage() =
        runTest {
            val engine = MockEngine { respond(content = PAGE_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS) }

            val result = client(engine).searchJql(jql = BASE_JQL)

            val page = assertIs<ApiResult.Success<QueryPage>>(result).value
            assertEquals(listOf("HSP-1"), page.tickets.map { it.key })
            assertEquals("token-2", page.nextPageToken)
            assertEquals(false, page.isLast)
        }

    @Test
    fun isLastTrueThiKetThucPhanTrang() =
        runTest {
            val engine =
                MockEngine { respond(content = LAST_PAGE_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS) }

            val result = client(engine).searchJql(jql = BASE_JQL, nextPageToken = "token-2")

            val page = assertIs<ApiResult.Success<QueryPage>>(result).value
            assertTrue(page.isLast)
            assertNull(page.nextPageToken)
        }

    @Test
    fun loi400TraVeInvalidJqlKemThongDiepCuaJira() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content =
                        INVALID_JQL_BODY,
                        status = HttpStatusCode.BadRequest,
                        headers = JSON_HEADERS,
                    )
                }

            val result = client(engine).searchJql(jql = "project = ABC abc")

            val error = assertIs<ApiResult.Failure>(result).error
            val invalidJql = assertIs<JiraError.InvalidJql>(error)
            assertTrue(invalidJql.message.contains("Expecting operator"), invalidJql.message)
        }

    @Test
    fun loi401TraVeAuthFailed() =
        runTest {
            val engine =
                MockEngine { respond(content = "{}", status = HttpStatusCode.Unauthorized, headers = JSON_HEADERS) }

            val result = client(engine).searchJql(jql = BASE_JQL)

            assertEquals(JiraError.AuthFailed, assertIs<ApiResult.Failure>(result).error)
        }

    @Test
    fun loi403TraVePermissionDenied() =
        runTest {
            val engine =
                MockEngine { respond(content = "{}", status = HttpStatusCode.Forbidden, headers = JSON_HEADERS) }

            val result = client(engine).searchJql(jql = BASE_JQL)

            assertEquals(JiraError.PermissionDenied, assertIs<ApiResult.Failure>(result).error)
        }

    @Test
    fun loi429TraVeRateLimited() =
        runTest {
            val engine =
                MockEngine {
                    respond(
                        content = "{}",
                        status = HttpStatusCode.TooManyRequests,
                        headers =
                            headersOf(
                                HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                                HttpHeaders.RetryAfter to listOf("30"),
                            ),
                    )
                }

            val result = client(engine).searchJql(jql = BASE_JQL)

            val error = assertIs<ApiResult.Failure>(result).error
            assertEquals(JiraError.RateLimited(retryAfterSeconds = 30), error)
        }

    @Test
    fun requestGuiHeaderXacThucBasic() =
        runTest {
            var authorization: String? = null
            val engine =
                MockEngine { request ->
                    authorization = request.headers[HttpHeaders.Authorization]
                    respond(content = PAGE_BODY, status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }

            client(engine).searchJql(jql = BASE_JQL)

            assertTrue(
                requireNotNull(authorization).startsWith("Basic "),
                "header Authorization phải là Basic, nhận được $authorization",
            )
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
            credentials = { JiraCredentials(baseUrl = BASE_URL, email = "dev@congty.vn", token = "api-token-bi-mat") },
        )

    private companion object {
        val INVALID_JQL_BODY =
            """
            {"errorMessages":["Error in the JQL Query: Expecting operator but got 'abc'."],"errors":{}}
            """.trimIndent()

        const val BASE_URL = "https://congty.atlassian.net"
        const val BASE_JQL = "project = HSP ORDER BY updated DESC"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

        val PAGE_BODY =
            """
            {
              "isLast": false,
              "nextPageToken": "token-2",
              "issues": [
                {
                  "id": "10001",
                  "key": "HSP-1",
                  "fields": {
                    "summary": "Sai số lượng hàng tồn",
                    "status": {"id": "10000", "name": "In Progress"},
                    "assignee": {"displayName": "Nguyen Van A"},
                    "priority": {"id": "2", "name": "High"},
                    "updated": "2026-10-30T08:15:00.000+0700"
                  }
                }
              ]
            }
            """.trimIndent()

        val LAST_PAGE_BODY =
            """
            {
              "isLast": true,
              "issues": [
                {
                  "id": "10002",
                  "key": "HSP-2",
                  "fields": {
                    "summary": "Không tạo được hoá đơn",
                    "status": {"id": "10001", "name": "Done"},
                    "assignee": null,
                    "priority": null,
                    "updated": "2026-10-29T17:00:00.000+0700"
                  }
                }
              ]
            }
            """.trimIndent()
    }
}
