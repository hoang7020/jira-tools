package com.jiratools.ui.saved

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiratools.platform.AppClock

/** Tên gợi ý cho bảng mới, kèm thời điểm để người dùng phân biệt các lần lưu (FR-001). */
fun defaultTableName(clock: AppClock): String = "Bảng ${clock.format(clock.nowMillis())}"

/**
 * Hộp thoại đặt tên và lưu kết quả truy vấn đang hiển thị thành một bảng riêng (FR-001, FR-011).
 *
 * Lỗi tên bảng do tầng nghiệp vụ quyết định và được truyền vào qua [error]; huỷ hộp thoại không
 * thay đổi dữ liệu nào.
 */
@Composable
fun SaveTableDialog(
    defaultName: String,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TableNameDialog(
        title = "Lưu bảng kết quả",
        hint = "Đặt tên cho bảng để mở lại sau này.",
        initialName = defaultName,
        confirmLabel = "Xác nhận",
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/**
 * Hộp thoại đổi tên một bảng đã lưu (FR-009, FR-011).
 *
 * Dùng chung khung nhập tên và thông báo lỗi với hộp thoại lưu bảng, nên hai luồng luôn kiểm tra tên
 * giống nhau.
 */
@Composable
fun RenameTableDialog(
    currentName: String,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TableNameDialog(
        title = "Đổi tên bảng",
        hint = "Đặt tên mới cho bảng đã lưu.",
        initialName = currentName,
        confirmLabel = "Lưu tên",
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
fun RestoreTableDialog(
    currentName: String,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    TableNameDialog(
        title = "Khôi phục bảng",
        hint = "Tên bảng đang được sử dụng. Hãy nhập tên khác để khôi phục.",
        initialName = currentName,
        confirmLabel = "Khôi phục",
        error = error,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

/** Hộp thoại xác nhận chuyển bảng đang hoạt động vào thùng rác. */
@Composable
fun DeleteTableDialog(
    tableName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chuyển bảng vào thùng rác") },
        text = {
            Text(
                text = "Bảng \"$tableName\" sẽ được chuyển vào thùng rác. Bạn có thể khôi phục bảng sau.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Chuyển")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        },
    )
}

@Composable
fun PermanentlyDeleteTrashedTableDialog(
    tableName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xoá vĩnh viễn") },
        text = {
            Text(
                text =
                    "Bảng \"$tableName\" cùng ticket và lịch sử sẽ bị xoá khỏi máy. " +
                        "Thao tác này không thể hoàn tác.",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text("Xoá vĩnh viễn")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        },
    )
}

/** Khung hộp thoại nhập tên bảng, dùng chung cho lưu mới và đổi tên (FR-009, FR-011). */
@Composable
private fun TableNameDialog(
    title: String,
    hint: String,
    initialName: String,
    confirmLabel: String,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text = hint, style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = name,
                    onValueChange = { updated -> name = updated },
                    label = { Text("Tên bảng") },
                    singleLine = true,
                    isError = error != null,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (error != null) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Huỷ") }
        },
    )
}
