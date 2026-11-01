package com.jiratools.data

import com.jiratools.domain.ColumnKey
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class TicketMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun assigneeNullThiOTicketDeTrong() {
        val ticket = TicketMapper.toTicket(issue(assignee = null))

        assertNotNull(ticket)
        assertNull(ticket.assignee)
    }

    @Test
    fun priorityNullThiOTicketDeTrong() {
        val ticket = TicketMapper.toTicket(issue(priority = null))

        assertNotNull(ticket)
        assertNull(ticket.priority)
    }

    @Test
    fun updatedSaiDinhDangKhongLamSap() {
        val ticket = TicketMapper.toTicket(issue(updated = "không-phải-thời-gian"))

        assertNotNull(ticket)
        assertEquals("không-phải-thời-gian", ticket.updated)
    }

    @Test
    fun updatedNullThiTraVeChuoiRong() {
        val ticket = TicketMapper.toTicket(issue(updated = null))

        assertNotNull(ticket)
        assertEquals("", ticket.updated)
    }

    @Test
    fun updatedDuocDinhDangLaiDeDoc() {
        val ticket = TicketMapper.toTicket(issue(updated = "2026-10-30T08:15:00.000+0700"))

        assertNotNull(ticket)
        assertEquals("2026-10-30 08:15", ticket.updated)
    }

    @Test
    fun tieuDeRatDaiDuocGiuNguyenDuLieu() {
        val longSummary = "T".repeat(500)

        val ticket = TicketMapper.toTicket(issue(summary = longSummary))

        assertNotNull(ticket)
        assertEquals(500, ticket.summary.length)
        assertEquals(longSummary, ticket.summary)
    }

    @Test
    fun issueThieuTruongBatBuocDuocXuLyAnToan() {
        val ticket = TicketMapper.toTicket(IssueDto(key = "HSP-9", fields = FieldsDto()))

        assertNotNull(ticket)
        assertEquals("HSP-9", ticket.key)
        assertEquals("", ticket.summary)
        assertEquals("", ticket.status)
    }

    @Test
    fun issueKhongCoKeyBiBoQua() {
        assertNull(TicketMapper.toTicket(IssueDto(key = null, fields = FieldsDto(summary = "Mồ côi"))))
        assertNull(TicketMapper.toTicket(IssueDto(key = "   ", fields = FieldsDto(summary = "Mồ côi"))))
    }

    @Test
    fun toPageBoQuaIssueHongVaGiuCacIssueHopLe() {
        val response =
            SearchJqlResponse(
                issues = listOf(issue(key = "HSP-1"), IssueDto(key = null), issue(key = "HSP-2")),
                nextPageToken = "token-2",
                isLast = false,
            )

        val page = TicketMapper.toPage(response, approximateTotal = 42)

        assertEquals(listOf("HSP-1", "HSP-2"), page.tickets.map { it.key })
        assertEquals("token-2", page.nextPageToken)
        assertEquals(false, page.isLast)
        assertEquals(42, page.approximateTotal)
    }

    @Test
    fun isLastTrueThiKhongConTrangTiep() {
        val response =
            SearchJqlResponse(
                issues = listOf(issue(key = "HSP-1")),
                nextPageToken = "token-cu",
                isLast = true,
            )

        val page = TicketMapper.toPage(response)

        assertTrue(page.isLast)
        assertNull(page.nextPageToken)
    }

    @Test
    fun thieuIsLastVaThieuTokenThiCoiNhuHetTrang() {
        val page = TicketMapper.toPage(SearchJqlResponse(issues = listOf(issue(key = "HSP-1"))))

        assertTrue(page.isLast)
        assertNull(page.nextPageToken)
    }

    @Test
    fun namesTrongPhanHoiKhongLamThayDoiNhanCotTiengViet() {
        val response =
            SearchJqlResponse(
                issues = listOf(issue(key = "HSP-1")),
                names = mapOf("summary" to "Summary (Jira)", "status" to "Status (Jira)"),
            )

        val page = TicketMapper.toPage(response)

        assertEquals(1, page.tickets.size)
        assertEquals(
            listOf("Mã", "Tiêu đề", "Trạng thái", "Người xử lý", "Độ ưu tiên", "Cập nhật"),
            ColumnKey.entries.take(6).map { it.label },
        )
    }

    @Test
    fun mapsStableIssueFieldsOriginalEstimateAndUpdatedInstant() {
        val dto =
            json.decodeFromString<IssueDto>(
                """
                {
                  "id": "10001",
                  "key": "HSP-1",
                  "fields": {
                    "summary": "Work item",
                    "status": {
                      "name": "Doing",
                      "statusCategory": {"key": "indeterminate"}
                    },
                    "updated": "2026-10-30T08:15:00.000+0700",
                    "timetracking": {"originalEstimate": "1d 2h"}
                  }
                }
                """.trimIndent(),
            )

        val ticket = assertNotNull(TicketMapper.toTicket(dto))

        assertEquals("10001", ticket.issueId)
        assertEquals("indeterminate", ticket.statusCategoryKey)
        assertEquals("1d 2h", ticket.originalExpectedTime)
        assertEquals("2026-10-30 08:15", ticket.updated)
        assertEquals(Instant.parse("2026-10-30T01:15:00Z").toEpochMilliseconds(), ticket.updatedAtMillis)
    }

    @Test
    fun invalidUpdatedTimestampStaysVisibleButHasNoInstant() {
        val dto =
            json.decodeFromString<IssueDto>(
                """{"id":"10002","key":"HSP-2","fields":{"updated":"not-a-timestamp"}}""",
            )

        val ticket = assertNotNull(TicketMapper.toTicket(dto))

        assertEquals("not-a-timestamp", ticket.updated)
        assertNull(ticket.updatedAtMillis)
    }

    @Test
    fun mapsDueDateAsCalendarDateAndKeepsMissingValueNull() {
        val dto =
            json.decodeFromString<IssueDto>(
                """{"key":"HSP-3","fields":{"duedate":"2026-11-15"}}""",
            )

        assertEquals("2026-11-15", TicketMapper.toTicket(dto)?.dueDate)
        assertNull(TicketMapper.toTicket(IssueDto(key = "HSP-4", fields = FieldsDto()))?.dueDate)
        assertTrue("duedate" in TicketMapper.requestFields)
    }

    @Test
    fun mapsParentSubtaskRelationsAndParentFlag() {
        val parent =
            TicketMapper.toTicket(
                IssueDto(
                    key = "HSP-10",
                    fields =
                        FieldsDto(
                            subtasks = listOf(SubtaskDto(key = "HSP-11")),
                        ),
                ),
            )
        val child =
            TicketMapper.toTicket(
                IssueDto(
                    key = "HSP-11",
                    fields =
                        FieldsDto(
                            parent = ParentDto(key = "HSP-10"),
                            issuetype = IssueTypeDto(subtask = true),
                        ),
                ),
            )

        assertTrue(parent!!.hasKnownSubtasks)
        assertTrue(parent.isParent)
        assertEquals("HSP-10", child?.parentKey)
        assertTrue(child?.isSubtask == true)
        assertTrue("parent" in TicketMapper.requestFields)
        assertTrue("issuetype" in TicketMapper.requestFields)
        assertTrue("subtasks" in TicketMapper.requestFields)
    }

    @Test
    fun parentWithNoVisibleSubtaskIsNotMarkedParentBySubtaskVisibility() {
        val dto =
            IssueDto(
                key = "HSP-12",
                fields = FieldsDto(subtasks = emptyList()),
            )

        assertEquals(false, TicketMapper.toTicket(dto)?.isParent)
    }

    private fun issue(
        key: String = "HSP-1",
        summary: String = "Sai số lượng hàng tồn",
        status: String? = "In Progress",
        assignee: String? = "Nguyen Van A",
        priority: String? = "High",
        updated: String? = "2026-10-30T08:15:00.000+0700",
    ): IssueDto =
        IssueDto(
            key = key,
            fields =
                FieldsDto(
                    summary = summary,
                    status = status?.let { StatusDto(name = it) },
                    assignee = assignee?.let { UserDto(displayName = it) },
                    priority = priority?.let { PriorityDto(name = it) },
                    updated = updated,
                ),
        )
}
