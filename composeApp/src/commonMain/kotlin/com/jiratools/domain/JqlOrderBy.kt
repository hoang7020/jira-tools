package com.jiratools.domain

private const val ORDER_BY = "ORDER BY"

/**
 * Viết lại mệnh đề `ORDER BY` của một truy vấn JQL theo cột và chiều sắp xếp đã chọn (FR-012).
 *
 * Sắp xếp được thực hiện phía Jira vì bảng chỉ giữ một phần kết quả trong bộ nhớ; sắp xếp phía
 * client sẽ cho thứ tự sai khi truy vấn có nhiều trang.
 *
 * `ORDER BY` nằm trong chuỗi hoặc trong ngoặc không phải mệnh đề sắp xếp nên được giữ nguyên,
 * và mệnh đề sắp xếp mới được thêm vào cuối truy vấn.
 */
object JqlOrderBy {
    fun apply(
        jql: String,
        column: ColumnKey?,
        ascending: Boolean,
    ): String {
        if (column == null || column.jqlField == null || jql.isBlank()) return jql
        val direction = if (ascending) "ASC" else "DESC"
        val clause = "$ORDER_BY ${column.jqlField} $direction"
        val start = trailingOrderByStart(jql)
        return if (start == null) {
            "${jql.trimEnd()} $clause"
        } else {
            "${jql.substring(0, start).trimEnd()} $clause"
        }
    }

    /**
     * Vị trí của mệnh đề `ORDER BY` ở cấp cao nhất, hoặc null khi truy vấn chưa có.
     *
     * Bộ quét bỏ qua nội dung trong nháy đơn, nháy kép và trong ngoặc để không sửa nhầm chuỗi.
     */
    private fun trailingOrderByStart(jql: String): Int? {
        var inSingleQuote = false
        var inDoubleQuote = false
        var depth = 0
        var index = 0
        var found: Int? = null
        while (index < jql.length) {
            val char = jql[index]
            when {
                (inSingleQuote || inDoubleQuote) && char == '\\' -> index++
                char == '\'' && !inDoubleQuote -> inSingleQuote = !inSingleQuote
                char == '"' && !inSingleQuote -> inDoubleQuote = !inDoubleQuote
                inSingleQuote || inDoubleQuote -> Unit
                char == '(' -> depth++
                char == ')' -> depth--
                depth == 0 && isOrderByAt(jql, index) -> found = index
            }
            index++
        }
        return found
    }

    private fun isOrderByAt(
        jql: String,
        index: Int,
    ): Boolean {
        val end = index + ORDER_BY.length
        if (end > jql.length) return false
        if (!jql.regionMatches(index, ORDER_BY, 0, ORDER_BY.length, ignoreCase = true)) return false
        val beforeIsBoundary = index == 0 || !jql[index - 1].isLetterOrDigit()
        val afterIsBoundary = end == jql.length || jql[end].isWhitespace()
        return beforeIsBoundary && afterIsBoundary
    }
}
