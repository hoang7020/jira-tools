package com.jiratools.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jiratools.domain.ConnectionStatus
import com.jiratools.domain.ThemeMode
import com.jiratools.ui.connection.ConnectionDraft
import com.jiratools.ui.connection.ConnectionForm

/** Màn hình Cài đặt, hiện tập trung vào cấu hình kết nối Jira. */
@Composable
fun SettingsScreen(
    status: ConnectionStatus,
    draft: ConnectionDraft,
    hasSavedConnection: Boolean,
    tokenSaved: Boolean?,
    notice: String?,
    onDraftChange: (ConnectionDraft) -> Unit,
    onValidate: (baseUrl: String, email: String, token: String) -> Unit,
    onDisconnect: () -> Unit,
    onDismissNotice: () -> Unit,
    themeMode: ThemeMode,
    appearanceNotice: String?,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDismissAppearanceNotice: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDisconnectConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(text = "Cài đặt", style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Text(text = "Giao diện", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Chọn giao diện theo hệ thống hoặc đặt chế độ sáng/tối.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            ThemeMode.entries.forEach { mode ->
                FilterChip(
                    selected = themeMode == mode,
                    onClick = { onThemeModeChange(mode) },
                    label = {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Hệ thống"
                                ThemeMode.LIGHT -> "Sáng"
                                ThemeMode.DARK -> "Tối"
                            },
                        )
                    },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        if (appearanceNotice != null) {
            Text(
                text = appearanceNotice,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = onDismissAppearanceNotice) { Text("Đóng") }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        ConnectionForm(
            status = status,
            draft = draft,
            tokenSaved = tokenSaved,
            notice = notice,
            onDraftChange = onDraftChange,
            onValidate = onValidate,
            onDismissNotice = onDismissNotice,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            onClick = { showDisconnectConfirmation = true },
            enabled = hasSavedConnection && status !is ConnectionStatus.Validating,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text("Ngắt kết nối")
        }
    }

    if (showDisconnectConfirmation) {
        AlertDialog(
            onDismissRequest = { showDisconnectConfirmation = false },
            title = { Text("Ngắt kết nối Jira?") },
            text = {
                Text("API token đã lưu, địa chỉ Jira và email sẽ bị xoá. Thao tác này không thể hoàn tác.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDisconnectConfirmation = false
                        onDisconnect()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text("Ngắt kết nối")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectConfirmation = false }) {
                    Text("Huỷ")
                }
            },
        )
    }
}
