package com.jiratools.ui.query

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/**
 * Ô nhập JQL và nút chạy truy vấn.
 *
 * Nút và tổ hợp Ctrl + Enter bị vô hiệu hoá khi đang tải để tránh gửi trùng truy vấn (FR-010).
 */
@Composable
fun QueryInput(
    jql: String,
    onJqlChange: (String) -> Unit,
    onRun: () -> Unit,
    running: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = jql,
            onValueChange = onJqlChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onPreviewKeyEvent { event ->
                        val ctrlEnter =
                            event.type == KeyEventType.KeyDown &&
                                event.key == Key.Enter &&
                                event.isCtrlPressed
                        if (ctrlEnter && !running) {
                            onRun()
                            true
                        } else {
                            false
                        }
                    },
            label = { Text("Truy vấn JQL") },
            placeholder = { Text("project = HSP ORDER BY updated DESC") },
            minLines = 2,
            maxLines = 4,
            singleLine = false,
            enabled = !running,
            supportingText = { Text("Nhấn Ctrl + Enter để chạy truy vấn.") },
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (running) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp).padding(end = 12.dp),
                )
            }
            Button(onClick = onRun, enabled = !running) {
                Text("Chạy truy vấn")
            }
        }
    }
}
