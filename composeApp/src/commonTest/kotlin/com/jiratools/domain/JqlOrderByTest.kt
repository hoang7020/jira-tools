package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Kiểm thử hàm viết lại mệnh đề `ORDER BY` (FR-012).
 *
 * Sắp xếp phải diễn ra phía server vì sắp xếp một trang đơn lẻ phía client sẽ cho kết quả sai
 * khi truy vấn có nhiều trang.
 */
class JqlOrderByTest {
    @Test
    fun themOrderByKhiChuaCo() {
        assertEquals(
            "project = HSP ORDER BY updated DESC",
            JqlOrderBy.apply(jql = "project = HSP", column = ColumnKey.UPDATED, ascending = false),
        )
    }

    @Test
    fun thayTheOrderByDaCoODuoi() {
        assertEquals(
            "project = HSP ORDER BY updated DESC",
            JqlOrderBy.apply(
                jql = "project = HSP ORDER BY created ASC",
                column = ColumnKey.UPDATED,
                ascending = false,
            ),
        )
    }

    @Test
    fun thayTheOrderByVietThuong() {
        assertEquals(
            "project = HSP ORDER BY status ASC",
            JqlOrderBy.apply(jql = "project = HSP order by updated desc", column = ColumnKey.STATUS, ascending = true),
        )
    }

    @Test
    fun giuNguyenPhanConLaiCuaTruyVan() {
        assertEquals(
            "project = HSP AND status != Done AND assignee = currentUser() ORDER BY priority ASC",
            JqlOrderBy.apply(
                jql = "project = HSP AND status != Done AND assignee = currentUser()",
                column = ColumnKey.PRIORITY,
                ascending = true,
            ),
        )
    }

    @Test
    fun khongSuaOrderByNamTrongChuoiNhayKep() {
        val jql = """project = HSP AND summary ~ "ORDER BY updated DESC" """

        assertEquals(
            """project = HSP AND summary ~ "ORDER BY updated DESC" ORDER BY key ASC""",
            JqlOrderBy.apply(jql = jql.trimEnd(), column = ColumnKey.KEY, ascending = true),
        )
    }

    @Test
    fun khongSuaOrderByNamTrongChuoiNhayDon() {
        val jql = "project = HSP AND labels = 'ORDER BY updated'"

        assertEquals(
            "$jql ORDER BY summary DESC",
            JqlOrderBy.apply(jql = jql, column = ColumnKey.SUMMARY, ascending = false),
        )
    }

    @Test
    fun khongSuaOrderByNamTrongNgoac() {
        val jql = "project = HSP AND (status = Done ORDER BY created)"

        assertEquals(
            "$jql ORDER BY assignee ASC",
            JqlOrderBy.apply(jql = jql, column = ColumnKey.ASSIGNEE, ascending = true),
        )
    }

    @Test
    fun jqlRongTraVeNguyenVen() {
        assertEquals("", JqlOrderBy.apply(jql = "", column = ColumnKey.UPDATED, ascending = true))
        assertEquals("   ", JqlOrderBy.apply(jql = "   ", column = ColumnKey.UPDATED, ascending = true))
    }

    @Test
    fun khongCoCotSapXepThiKhongDoiJql() {
        assertEquals(
            "project = HSP ORDER BY created ASC",
            JqlOrderBy.apply(jql = "project = HSP ORDER BY created ASC", column = null, ascending = true),
        )
    }

    @Test
    fun dungTuKhoaTiengAnhChuanCuaJql() {
        val result = JqlOrderBy.apply(jql = "project = HSP", column = ColumnKey.UPDATED, ascending = true)

        assertEquals("project = HSP ORDER BY updated ASC", result)
    }

    @Test
    fun khongDeLaiKhoangTrangThua() {
        assertEquals(
            "project = HSP ORDER BY updated DESC",
            JqlOrderBy.apply(jql = "project = HSP   ", column = ColumnKey.UPDATED, ascending = false),
        )
    }

    @Test
    fun originalExpectedTimeUsesJiraOriginalEstimateField() {
        val column = ColumnKey.valueOf("ORIGINAL_EXPECTED_TIME")

        assertEquals(
            "project = HSP ORDER BY originalEstimate ASC",
            JqlOrderBy.apply(jql = "project = HSP", column = column, ascending = true),
        )
    }
}
