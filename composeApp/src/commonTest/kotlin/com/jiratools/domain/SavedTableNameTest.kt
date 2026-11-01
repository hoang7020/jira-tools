package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Quy tắc tên bảng: chuẩn hoá khoảng trắng, từ chối tên rỗng, so trùng không phân biệt hoa/thường
 * và sinh định danh duy nhất cho bảng mới (FR-011).
 */
class SavedTableNameTest {
    @Test
    fun normalizeTrimsSurroundingSpaces() {
        assertEquals("Báo cáo", normalizeTableName("  Báo cáo  "))
        assertEquals("", normalizeTableName("   "))
    }

    @Test
    fun blankNamesAreRejected() {
        assertIs<TableNameValidation.Empty>(validateTableName("", emptyList()))
        assertIs<TableNameValidation.Empty>(validateTableName("   ", listOf(EXISTING)))
    }

    @Test
    fun nameIsValidWhenNoOtherTableUsesIt() {
        assertIs<TableNameValidation.Valid>(validateTableName(EXISTING, emptyList()))
    }

    @Test
    fun duplicateIgnoresCaseAndSurroundingSpaces() {
        val existing = listOf(EXISTING)

        assertIs<TableNameValidation.Duplicate>(validateTableName("báo CÁO", existing))
        assertIs<TableNameValidation.Duplicate>(validateTableName(" $EXISTING ", existing))
    }

    @Test
    fun validatingOwnNameAgainstOtherTablesIsValid() {
        assertIs<TableNameValidation.Valid>(validateTableName(EXISTING, listOf("Việc trong tuần")))
    }

    @Test
    fun newIdUsesTimestampAndAddsSuffixWhenTaken() {
        assertEquals("1000", newTableId(nowMillis = 1000, existingIds = emptyList()))
        assertEquals("1000-2", newTableId(nowMillis = 1000, existingIds = listOf("1000")))
        assertEquals("1000-3", newTableId(nowMillis = 1000, existingIds = listOf("1000", "1000-2")))
    }

    private companion object {
        const val EXISTING = "Báo cáo"
    }
}
