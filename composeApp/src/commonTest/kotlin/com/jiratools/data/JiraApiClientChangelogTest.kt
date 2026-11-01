package com.jiratools.data

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
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JiraApiClientChangelogTest {
    @Test
    fun sendsStatusFilterAndFollowsEveryPage() =
        runTest {
            val requests = mutableListOf<String>()
            val engine =
                MockEngine { request ->
                    requests += request.body.asText()
                    val token = if (requests.size == 1) ""","nextPageToken":"next"""" else ""
                    respond(
                        content = """{"issueChangeLogs":[]$token}""",
                        status = HttpStatusCode.OK,
                        headers = JSON_HEADERS,
                    )
                }

            val result = client(engine).bulkStatusChangelogs(listOf("101", "102"))

            assertIs<ApiResult.Success<List<IssueChangeLogDto>>>(result)
            assertEquals(2, requests.size)
            val first = Json.parseToJsonElement(requests.first()).jsonObject
            assertEquals("""["101","102"]""", first["issueIdsOrKeys"].toString())
            assertEquals("""["status"]""", first["fieldIds"].toString())
            assertEquals(
                "next",
                Json
                    .parseToJsonElement(requests[1])
                    .jsonObject["nextPageToken"]
                    ?.jsonPrimitive
                    ?.content,
            )
            assertTrue(requests.all { it.isNotBlank() })
        }

    @Test
    fun splitsIssueIdsIntoBatchesOfOneThousand() =
        runTest {
            val issueCounts = mutableListOf<Int>()
            val engine =
                MockEngine { request ->
                    val payload = Json.parseToJsonElement(request.body.asText()).jsonObject
                    issueCounts += payload["issueIdsOrKeys"]!!.jsonArray.size
                    respond(content = """{"issueChangeLogs":[]}""", status = HttpStatusCode.OK, headers = JSON_HEADERS)
                }

            val result = client(engine).bulkStatusChangelogs((1..1001).map(Int::toString))

            assertIs<ApiResult.Success<List<IssueChangeLogDto>>>(result)
            assertEquals(listOf(1000, 1), issueCounts)
        }

    private fun client(engine: MockEngine) =
        JiraApiClient(
            client = HttpClientFactory.create(engine),
            credentials = { JiraCredentials("https://example.atlassian.net", "user", "secret") },
        )

    private fun OutgoingContent.asText(): String =
        when (this) {
            is TextContent -> text
            is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
            else -> ""
        }

    private companion object {
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }
}
