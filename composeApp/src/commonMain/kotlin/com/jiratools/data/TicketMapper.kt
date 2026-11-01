package com.jiratools.data

import com.jiratools.domain.QueryPage
import com.jiratools.domain.Ticket
import kotlin.time.Instant

/**
 * Chuyển DTO của Jira thành mô hình miền [Ticket].
 *
 * Bộ chuyển này chịu toàn bộ khác biệt giữa JSON của Jira và mô hình của ứng dụng: trường thiếu,
 * giá trị null và định dạng thời gian. Nhờ vậy phần còn lại của ứng dụng không cần biết `fields`
 * có thể vắng mặt (Nguyên tắc I).
 */
object TicketMapper {
    /** Trả `null` khi issue thiếu `key`, vì ticket không có mã thì không hiển thị được (FR-006). */
    fun toTicket(dto: IssueDto): Ticket? {
        val key = dto.key?.trim()
        if (key.isNullOrEmpty()) return null
        val fields = dto.fields
        val parentKey =
            fields
                ?.parent
                ?.key
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        return Ticket(
            key = key,
            summary = fields?.summary.orEmpty(),
            status = fields?.status?.name.orEmpty(),
            assignee = fields?.assignee?.displayName?.takeIf { it.isNotBlank() },
            priority = fields?.priority?.name?.takeIf { it.isNotBlank() },
            updated = formatUpdated(fields?.updated),
            issueId = dto.id,
            statusCategoryKey = fields?.status?.statusCategory?.key,
            updatedAtMillis = parseUpdatedAtMillis(fields?.updated),
            originalExpectedTime = fields?.timetracking?.originalEstimate,
            dueDate = fields?.dueDate,
            parentKey = parentKey,
            hasKnownSubtasks = fields?.subtasks.orEmpty().isNotEmpty(),
            isSubtask = fields?.issuetype?.subtask == true,
            isParent = fields?.subtasks.orEmpty().isNotEmpty(),
        )
    }

    /** Ánh xạ cả trang kết quả, bỏ qua issue hỏng thay vì làm hỏng toàn bộ trang. */
    fun toPage(
        dto: SearchJqlResponse,
        approximateTotal: Int? = null,
    ): QueryPage {
        val ticketsWithoutChildParents = dto.issues.mapNotNull(::toTicket)
        val resultKeys = ticketsWithoutChildParents.mapTo(mutableSetOf()) { it.key }
        val knownParentKeys =
            ticketsWithoutChildParents
                .mapNotNull { ticket -> ticket.parentKey?.takeIf { resultKeys.contains(it) } }
                .toSet()
        val tickets =
            ticketsWithoutChildParents.map { ticket ->
                ticket.copy(isParent = ticket.isParent || ticket.key in knownParentKeys)
            }
        val isLast = dto.isLast ?: dto.nextPageToken.isNullOrBlank()
        return QueryPage(
            tickets = tickets,
            nextPageToken = if (isLast) null else dto.nextPageToken,
            isLast = isLast,
            approximateTotal = approximateTotal,
        )
    }

    /**
     * Jira trả `2026-10-30T08:15:00.000+0700`; hiển thị gọn thành `2026-10-30 08:15`.
     * Chuỗi lạ được giữ nguyên để không làm mất dữ liệu của người dùng.
     */
    fun formatUpdated(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        if (raw.length < 16 || raw[4] != '-' || raw[7] != '-' || raw[10] != 'T') return raw
        return raw.substring(0, 10) + " " + raw.substring(11, 16)
    }

    /** Parses Jira's `+HHMM` offset form without changing the displayed Updated value. */
    fun parseUpdatedAtMillis(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        val normalized =
            JIRA_OFFSET_PATTERN.replace(raw) { match ->
                "${match.groupValues[1]}:${match.groupValues[2]}"
            }
        return try {
            Instant.parse(normalized).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    /** Các trường Jira cần yêu cầu cho mỗi ticket trong bảng. */
    val requestFields: List<String> =
        listOf(
            "summary",
            "status",
            "assignee",
            "priority",
            "updated",
            "timetracking",
            "duedate",
            "parent",
            "issuetype",
            "subtasks",
        )

    private val JIRA_OFFSET_PATTERN = Regex("([+-]\\d{2})(\\d{2})$")
}
