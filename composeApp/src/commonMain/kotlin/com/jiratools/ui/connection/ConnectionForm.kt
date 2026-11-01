package com.jiratools.ui.connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.jiratools.domain.ConnectionStatus

/** Nội dung nhập liệu đang tạm giữ trong bộ nhớ; token không được ghi vào tệp cấu hình. */
data class ConnectionDraft(
    val baseUrl: String = "",
    val email: String = "",
    val token: String = "",
    val tokenVisible: Boolean = false,
)

/** Biểu mẫu cấu hình kết nối Jira trong màn hình Cài đặt. */
@Composable
fun ConnectionForm(
    status: ConnectionStatus,
    draft: ConnectionDraft,
    tokenSaved: Boolean?,
    notice: String?,
    onDraftChange: (ConnectionDraft) -> Unit,
    onValidate: (baseUrl: String, email: String, token: String) -> Unit,
    onDismissNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val validating = status is ConnectionStatus.Validating

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Kết nối Jira", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = draft.baseUrl,
            onValueChange = { onDraftChange(draft.copy(baseUrl = it)) },
            label = { Text("Địa chỉ Jira") },
            placeholder = { Text("https://congty.atlassian.net") },
            singleLine = true,
            enabled = !validating,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = draft.email,
            onValueChange = { onDraftChange(draft.copy(email = it)) },
            label = { Text("Email") },
            placeholder = { Text("dev@congty.vn") },
            singleLine = true,
            enabled = !validating,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = tokenSavedText(tokenSaved), style = MaterialTheme.typography.bodySmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = draft.token,
                onValueChange = { onDraftChange(draft.copy(token = it)) },
                label = { Text("API token") },
                placeholder = {
                    if (tokenSaved == true) {
                        Text("Đã có token được lưu — nhập để thay thế")
                    }
                },
                singleLine = true,
                enabled = !validating,
                visualTransformation =
                    if (draft.tokenVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                modifier = Modifier.weight(1f),
            )
            TextButton(
                onClick = { onDraftChange(draft.copy(tokenVisible = !draft.tokenVisible)) },
                enabled = !validating,
            ) {
                Text(if (draft.tokenVisible) "Ẩn" else "Hiện")
            }
        }
        Button(
            onClick = { onValidate(draft.baseUrl, draft.email, draft.token) },
            enabled = !validating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Kiểm tra kết nối")
        }
        if (validating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = connectionStatusDetail(status),
            color =
                if (status is ConnectionStatus.Invalid) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            style = MaterialTheme.typography.bodyMedium,
        )
        if (notice != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissNotice) { Text("Đóng") }
            }
        }
    }
}

private fun tokenSavedText(tokenSaved: Boolean?): String =
    when (tokenSaved) {
        true -> "Đã có token được lưu trên máy."
        false -> "Chưa có token được lưu."
        null -> "Đang kiểm tra…"
    }
