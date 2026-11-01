package com.jiratools.ui.query

import com.jiratools.data.ApiResult
import com.jiratools.data.AppConfigStore
import com.jiratools.data.JiraApiClient
import com.jiratools.domain.ColumnKey
import com.jiratools.domain.JiraError
import com.jiratools.domain.JqlOrderBy
import com.jiratools.domain.QueryPage
import com.jiratools.domain.QueryState
import com.jiratools.domain.SortState
import com.jiratools.domain.Ticket
import com.jiratools.domain.UserPreferences
import com.jiratools.platform.ioDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException

/**
 * Điều phối truy vấn JQL và phân trang.
 *
 * Toàn bộ trạng thái hiển thị nằm trong [state] và [jql]; Compose chỉ đọc lại qua `StateFlow`
 * (Nguyên tắc III).
 */
class QueryViewModel(
    private val apiClient: JiraApiClient,
    private val configStore: AppConfigStore,
    private val dispatcher: CoroutineDispatcher = ioDispatcher,
) {
    private val mutableState = MutableStateFlow<QueryState>(QueryState.Idle)
    private val mutableJql = MutableStateFlow("")
    private val mutableLoadingMore = MutableStateFlow(false)
    private val mutableVisibleColumns = MutableStateFlow(ColumnKey.all())
    private val mutableSort = MutableStateFlow(SortState())

    val state: StateFlow<QueryState> = mutableState.asStateFlow()
    val jql: StateFlow<String> = mutableJql.asStateFlow()
    val loadingMore: StateFlow<Boolean> = mutableLoadingMore.asStateFlow()
    val visibleColumns: StateFlow<Set<ColumnKey>> = mutableVisibleColumns.asStateFlow()
    val sort: StateFlow<SortState> = mutableSort.asStateFlow()

    private var lastPageToken: String? = null

    /** Khôi phục truy vấn, cột hiển thị và sắp xếp đã lưu (FR-011, FR-012). */
    suspend fun loadSaved() {
        val preferences = withContext(dispatcher) { configStore.load() }
        preferences.lastJql?.let { mutableJql.value = it }
        val storedColumns = preferences.visibleColumns.intersect(ColumnKey.all())
        mutableVisibleColumns.value = if (storedColumns.isEmpty()) setOf(ColumnKey.KEY) else storedColumns
        preferences.sort?.let { mutableSort.value = it }
    }

    /**
     * Bật/tắt một cột; cột `Mã` luôn hiển thị nên không thể tắt (FR-011).
     */
    suspend fun toggleColumn(column: ColumnKey) {
        if (column == ColumnKey.KEY) return
        val current = mutableVisibleColumns.value
        val updated = if (column in current) current - column else current + column
        if (updated.isEmpty()) return
        mutableVisibleColumns.value = updated
        persistPreferences { it.copy(visibleColumns = updated) }
    }

    /**
     * Sắp xếp theo cột: bấm lại cùng cột thì đảo chiều, cột mới thì bắt đầu từ tăng dần (FR-012).
     *
     * Truy vấn được chạy lại với `ORDER BY` đã viết vì thứ tự do Jira quyết định trên toàn bộ
     * kết quả, không chỉ trên trang đang hiển thị.
     */
    suspend fun sortBy(column: ColumnKey) {
        val current = mutableSort.value
        val updated =
            if (current.column == column) {
                SortState(column = column, ascending = !current.ascending)
            } else {
                SortState(column = column, ascending = true)
            }
        mutableSort.value = updated
        persistPreferences { it.copy(sort = updated) }
        val jql = mutableJql.value
        if (jql.isNotBlank()) {
            run(jql = JqlOrderBy.apply(jql = jql, column = updated.column, ascending = updated.ascending))
        }
    }

    /** Cập nhật nội dung ô nhập JQL mà không chạy truy vấn. */
    fun updateJql(value: String) {
        mutableJql.value = value
    }

    /** Chạy truy vấn mới; JQL rỗng bị chặn trước khi gọi mạng (FR-009). */
    suspend fun run(jql: String) {
        mutableJql.value = jql
        if (jql.isBlank()) {
            mutableState.value = QueryState.Error(JiraError.InvalidJql(EMPTY_QUERY_MESSAGE), jql)
            return
        }
        persistLastJql(jql)
        mutableState.value = QueryState.Loading
        lastPageToken = null
        when (val result = callSearch(jql, nextPageToken = null)) {
            is ApiResult.Success -> {
                val page = result.value
                if (page.tickets.isEmpty()) {
                    mutableState.value = QueryState.Empty
                    return
                }
                lastPageToken = page.nextPageToken
                mutableState.value = QueryState.Success(page.tickets, hasMore = !page.isLast)
                refreshApproximateCount(jql)
            }
            is ApiResult.Failure -> mutableState.value = QueryState.Error(result.error, jql)
        }
    }

    /** Chạy lại đúng truy vấn đang hiển thị. */
    suspend fun retry() {
        run(mutableJql.value)
    }

    /** Tải trang tiếp theo và nối vào danh sách hiện có, loại bỏ ticket trùng mã (FR-007). */
    suspend fun loadMore() {
        val current = mutableState.value as? QueryState.Success ?: return
        val token = lastPageToken ?: return
        if (!current.hasMore || mutableLoadingMore.value) return

        mutableLoadingMore.value = true
        try {
            when (val result = callSearch(mutableJql.value, nextPageToken = token)) {
                is ApiResult.Success -> {
                    val page = result.value
                    lastPageToken = page.nextPageToken
                    val merged = merge(current.tickets, page.tickets)
                    mutableState.value = current.copy(tickets = merged, hasMore = !page.isLast)
                }
                is ApiResult.Failure -> mutableState.value = QueryState.Error(result.error, mutableJql.value)
            }
        } finally {
            mutableLoadingMore.value = false
        }
    }

    private suspend fun callSearch(
        jql: String,
        nextPageToken: String?,
    ): ApiResult<QueryPage> =
        try {
            withContext(dispatcher) { apiClient.searchJql(jql = jql, nextPageToken = nextPageToken) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (failure: Throwable) {
            ApiResult.Failure(JiraError.Unexpected(detail = failure.message))
        }

    /** Số lượng ước tính chỉ để hiển thị thêm, nên không được chặn hay làm hỏng kết quả (FR-007). */
    private suspend fun refreshApproximateCount(jql: String) {
        val count =
            try {
                withContext(dispatcher) { apiClient.approximateCount(jql) }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                null
            } ?: return
        val current = mutableState.value as? QueryState.Success ?: return
        mutableState.value = current.copy(approximateTotal = count)
    }

    private suspend fun persistLastJql(jql: String) {
        persistPreferences { it.copy(lastJql = jql) }
    }

    /** Ghi tuỳ chọn người dùng; lỗi ghi không được làm hỏng trải nghiệm truy vấn. */
    private suspend fun persistPreferences(update: (UserPreferences) -> UserPreferences) {
        try {
            withContext(dispatcher) {
                configStore.save(update(configStore.load()))
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Throwable) {
            // Không lưu được tuỳ chọn không phải lỗi chặn người dùng.
        }
    }

    private fun merge(
        existing: List<Ticket>,
        incoming: List<Ticket>,
    ): List<Ticket> {
        val seen = existing.mapTo(mutableSetOf()) { it.key }
        val appended = incoming.filter { seen.add(it.key) }
        return existing + appended
    }

    private companion object {
        const val EMPTY_QUERY_MESSAGE = "Vui lòng nhập truy vấn JQL."
    }
}
