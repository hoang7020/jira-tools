package com.jiratools.domain

/** Bỏ khoảng trắng thừa hai đầu tên bảng; đây là dạng được lưu và hiển thị (FR-011). */
fun normalizeTableName(raw: String): String = raw.trim()

/** Kết quả kiểm tra tên bảng trước khi lưu mới hoặc đổi tên (FR-011). */
sealed interface TableNameValidation {
    /** Tên dùng được. */
    data object Valid : TableNameValidation

    /** Tên rỗng sau khi cắt khoảng trắng. */
    data object Empty : TableNameValidation

    /** Tên đã được một bảng khác dùng (so sánh không phân biệt hoa/thường). */
    data object Duplicate : TableNameValidation
}

/**
 * Kiểm tra tên bảng mới hoặc tên sau khi đổi.
 *
 * [otherNames] là tên của **các bảng khác**, không bao gồm bảng đang đổi tên, nhờ đó người dùng giữ
 * nguyên tên cũ mà không bị coi là trùng. So sánh dùng khoá `trim().lowercase()` để hai bảng chỉ
 * khác nhau về hoa/thường hoặc khoảng trắng thừa không cùng tồn tại.
 */
fun validateTableName(
    raw: String,
    otherNames: List<String>,
): TableNameValidation {
    val name = normalizeTableName(raw)
    if (name.isEmpty()) return TableNameValidation.Empty
    val key = name.lowercase()
    val duplicated = otherNames.any { normalizeTableName(it).lowercase() == key }
    return if (duplicated) TableNameValidation.Duplicate else TableNameValidation.Valid
}

/**
 * Thông báo hiển thị cho người dùng khi tên bảng chưa dùng được (FR-011).
 *
 * Là nơi duy nhất định nghĩa câu chữ cho lỗi tên bảng, để hộp thoại và thông báo chung không lệch nhau.
 */
fun TableNameValidation.errorMessage(): String? =
    when (this) {
        TableNameValidation.Valid -> null
        TableNameValidation.Empty -> "Tên bảng không được để trống."
        TableNameValidation.Duplicate -> "Tên bảng đã tồn tại."
    }

/**
 * Sinh định danh duy nhất cho bảng mới từ thời điểm lưu.
 *
 * Dùng chính thời điểm lưu làm định danh để dễ đối chiếu, và thêm hậu tố `-2`, `-3`, … khi người
 * dùng lưu hai bảng trong cùng một mili giây (FR-009).
 */
fun newTableId(
    nowMillis: Long,
    existingIds: List<String>,
): String {
    val base = nowMillis.toString()
    if (base !in existingIds) return base
    var suffix = 2
    while ("$base-$suffix" in existingIds) {
        suffix += 1
    }
    return "$base-$suffix"
}
