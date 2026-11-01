package com.jiratools.data

import com.jiratools.domain.JiraError
import com.jiratools.domain.QueryPage
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.encodeURLPathPart
import io.ktor.http.isSuccess
import io.ktor.util.encodeBase64
import kotlinx.datetime.LocalDate
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationStrategy
import kotlin.coroutines.cancellation.CancellationException

/** Thông tin xác thực Jira Cloud; token chỉ nằm trong bộ nhớ, không bao giờ ghi ra đĩa. */
data class JiraCredentials(
    val baseUrl: String,
    val email: String,
    val token: String,
)

/** Kết quả một lời gọi API: thành công kèm dữ liệu, hoặc thất bại kèm [JiraError] đã phân loại. */
sealed interface ApiResult<out T> {
    data class Success<T>(
        val value: T,
    ) : ApiResult<T>

    data class Failure(
        val error: JiraError,
    ) : ApiResult<Nothing>
}

/**
 * Lớp gọi Jira Cloud REST API v3.
 *
 * Mọi lỗi HTTP và lỗi mạng đều được chuyển thành [JiraError] qua [JiraErrorMapper], nên tầng
 * trên không bao giờ phải xử lý ngoại lệ của Ktor (Nguyên tắc I và V).
 */
class JiraApiClient(
    private val client: HttpClient,
    private val credentials: suspend () -> JiraCredentials? = { null },
) {
    suspend fun myself(explicit: JiraCredentials? = null): ApiResult<MyselfDto> {
        val current = explicit ?: credentials()
        if (current == null) {
            return ApiResult.Failure(JiraError.Unexpected(detail = "Chưa có thông tin đăng nhập Jira."))
        }
        return request(current, MYSELF_PATH, MyselfDto.serializer())
    }

    /** Cập nhật đúng field Due Date; endpoint PUT thành công không bắt buộc trả body. */
    suspend fun updateDueDate(
        issueIdOrKey: String,
        dueDate: String,
        explicit: JiraCredentials? = null,
    ): ApiResult<Unit> {
        val current = explicit ?: credentials()
        if (current == null) {
            return ApiResult.Failure(JiraError.Unexpected(detail = "Chưa có thông tin đăng nhập Jira."))
        }
        val issueIdentity = issueIdOrKey.trim()
        if (issueIdentity.isEmpty()) {
            return ApiResult.Failure(JiraError.Unexpected(detail = "Thiếu mã issue Jira để cập nhật Due Date."))
        }
        val validDueDate =
            try {
                LocalDate.parse(dueDate).toString().takeIf { it == dueDate }
            } catch (_: IllegalArgumentException) {
                null
            }
        if (validDueDate == null) {
            return ApiResult.Failure(JiraError.Unexpected(detail = "Due Date phải có định dạng yyyy-MM-dd hợp lệ."))
        }
        val response =
            try {
                client.put(url(current.baseUrl, "$ISSUE_PATH/${issueIdentity.encodeURLPathPart()}")) {
                    header(HttpHeaders.Authorization, current.basicAuthHeader())
                    val payload =
                        IssueDueDateUpdateRequest(
                            fields = IssueDueDateFields(dueDate = validDueDate),
                        )
                    val json = HttpClientFactory.json.encodeToString(IssueDueDateUpdateRequest.serializer(), payload)
                    setBody(TextContent(json, ContentType.Application.Json))
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                return ApiResult.Failure(JiraErrorMapper.fromThrowable(failure))
            }
        if (response.status.isSuccess()) return ApiResult.Success(Unit)
        val text =
            try {
                response.bodyAsText()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                return ApiResult.Failure(JiraErrorMapper.fromThrowable(failure))
            }
        return ApiResult.Failure(
            JiraErrorMapper.fromStatus(
                status = response.status.value,
                body = toErrorCollection(text),
                retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.toIntOrNull(),
            ),
        )
    }

    /**
     * Chạy JQL và trả về một trang ticket.
     *
     * `nextPageToken` chỉ được gửi khi tải thêm, đúng như hợp đồng `POST /rest/api/3/search/jql`.
     */
    suspend fun searchJql(
        jql: String,
        nextPageToken: String? = null,
        explicit: JiraCredentials? = null,
    ): ApiResult<QueryPage> {
        val current = explicit ?: credentials()
        if (current == null) {
            return ApiResult.Failure(JiraError.Unexpected(detail = "Chưa có thông tin đăng nhập Jira."))
        }
        val body =
            SearchJqlRequest(
                jql = jql,
                fields = TicketMapper.requestFields,
                maxResults = PAGE_SIZE,
                nextPageToken = nextPageToken,
            )
        return requestPost(
            credentials = current,
            path = SEARCH_PATH,
            payload = body,
            payloadSerializer = SearchJqlRequest.serializer(),
            deserializer = SearchJqlResponse.serializer(),
        ).let { page -> mapPage(page) }
    }

    suspend fun bulkStatusChangelogs(issueIds: List<String>): ApiResult<List<IssueChangeLogDto>> {
        val current =
            credentials()
                ?: return ApiResult.Failure(JiraError.Unexpected(detail = "Chưa có thông tin đăng nhập Jira."))
        val histories = mutableListOf<IssueChangeLogDto>()
        issueIds
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .toList()
            .chunked(CHANGELOG_ISSUE_BATCH_SIZE)
            .forEach { batch ->
                var nextPageToken: String? = null
                val seenTokens = mutableSetOf<String>()
                do {
                    val result =
                        requestPost(
                            credentials = current,
                            path = CHANGELOG_BULK_PATH,
                            payload =
                                BulkChangelogRequest(
                                    issueIdsOrKeys = batch,
                                    fieldIds = listOf("status"),
                                    maxResults = CHANGELOG_PAGE_SIZE,
                                    nextPageToken = nextPageToken,
                                ),
                            payloadSerializer = BulkChangelogRequest.serializer(),
                            deserializer = BulkChangelogResponse.serializer(),
                        )
                    when (result) {
                        is ApiResult.Failure -> return result
                        is ApiResult.Success -> {
                            histories += result.value.issueChangeLogs
                            nextPageToken = result.value.nextPageToken?.takeIf(String::isNotBlank)
                            if (nextPageToken != null && !seenTokens.add(nextPageToken)) {
                                return ApiResult.Failure(
                                    JiraError.Unexpected(detail = "Jira trả token phân trang changelog bị lặp."),
                                )
                            }
                        }
                    }
                } while (nextPageToken != null)
            }
        return ApiResult.Success(histories)
    }

    /** Tổng số ticket gần đúng; mọi lỗi đều trả `null` để không chặn việc hiển thị bảng (FR-007). */
    suspend fun approximateCount(
        jql: String,
        explicit: JiraCredentials? = null,
    ): Int? {
        val current = explicit ?: credentials() ?: return null
        val result =
            requestPost(
                credentials = current,
                path = COUNT_PATH,
                payload = ApproximateCountRequest(jql),
                payloadSerializer = ApproximateCountRequest.serializer(),
                deserializer = ApproximateCountResponse.serializer(),
            )
        return (result as? ApiResult.Success)?.value?.count
    }

    private fun mapPage(result: ApiResult<SearchJqlResponse>): ApiResult<QueryPage> =
        when (result) {
            is ApiResult.Success -> ApiResult.Success(TicketMapper.toPage(result.value))
            is ApiResult.Failure -> result
        }

    private suspend fun <T, B> requestPost(
        credentials: JiraCredentials,
        path: String,
        payload: B,
        payloadSerializer: SerializationStrategy<B>,
        deserializer: DeserializationStrategy<T>,
    ): ApiResult<T> {
        val response =
            try {
                client.post(url(credentials.baseUrl, path)) {
                    header(HttpHeaders.Authorization, credentials.basicAuthHeader())
                    val json = HttpClientFactory.json.encodeToString(payloadSerializer, payload)
                    setBody(TextContent(json, ContentType.Application.Json))
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                return ApiResult.Failure(JiraErrorMapper.fromThrowable(failure))
            }
        return decode(response, deserializer)
    }

    private suspend fun <T> request(
        credentials: JiraCredentials,
        path: String,
        deserializer: DeserializationStrategy<T>,
    ): ApiResult<T> {
        val response =
            try {
                client.get(url(credentials.baseUrl, path)) {
                    header(HttpHeaders.Authorization, credentials.basicAuthHeader())
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                return ApiResult.Failure(JiraErrorMapper.fromThrowable(failure))
            }
        return decode(response, deserializer)
    }

    private suspend fun <T> decode(
        response: HttpResponse,
        deserializer: DeserializationStrategy<T>,
    ): ApiResult<T> {
        val text =
            try {
                response.bodyAsText()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (failure: Throwable) {
                return ApiResult.Failure(JiraErrorMapper.fromThrowable(failure))
            }
        if (!response.status.isSuccess()) {
            return ApiResult.Failure(
                JiraErrorMapper.fromStatus(
                    status = response.status.value,
                    body = toErrorCollection(text),
                    retryAfterSeconds = response.headers[HttpHeaders.RetryAfter]?.toIntOrNull(),
                ),
            )
        }
        val value = runCatching { HttpClientFactory.json.decodeFromString(deserializer, text) }.getOrNull()
        if (value == null) {
            return ApiResult.Failure(
                JiraError.Unexpected(response.status.value, "Jira trả về dữ liệu không đọc được."),
            )
        }
        return ApiResult.Success(value)
    }

    private fun toErrorCollection(text: String): ErrorCollectionDto? {
        if (text.isBlank()) return null
        val body =
            runCatching {
                HttpClientFactory.json.decodeFromString(ErrorCollectionDto.serializer(), text)
            }.getOrNull()
        return body
    }

    private fun url(
        baseUrl: String,
        path: String,
    ): String = "${baseUrl.trimEnd('/')}$path"

    private fun JiraCredentials.basicAuthHeader(): String {
        val encoded = "$email:$token".encodeBase64()
        return "Basic $encoded"
    }

    private companion object {
        const val MYSELF_PATH = "/rest/api/3/myself"
        const val SEARCH_PATH = "/rest/api/3/search/jql"
        const val ISSUE_PATH = "/rest/api/3/issue"
        const val COUNT_PATH = "/rest/api/3/search/approximate-count"
        const val CHANGELOG_BULK_PATH = "/rest/api/3/changelog/bulkfetch"
        const val PAGE_SIZE = 100
        const val CHANGELOG_ISSUE_BATCH_SIZE = 1000
        const val CHANGELOG_PAGE_SIZE = 100
    }
}
