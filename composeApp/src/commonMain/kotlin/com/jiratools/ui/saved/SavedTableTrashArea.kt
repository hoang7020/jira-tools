package com.jiratools.ui.saved

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jiratools.domain.TrashedSavedTable
import com.jiratools.platform.AppClock

@Composable
fun SavedTableTrashArea(
    state: SavedTablesUiState,
    clock: AppClock,
    onRetryLoad: () -> Unit,
    onRestore: (TrashedSavedTable) -> Unit,
    onPermanentlyDelete: (TrashedSavedTable) -> Unit,
    onDismissMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trashedTables = remember(state.trashedTables) { state.trashedTables.sortedByDescending { it.deletedAtMillis } }

    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        SavedMessageBanner(message = state.message, onDismiss = onDismissMessage)
        Text("Thùng rác", style = MaterialTheme.typography.headlineSmall)
        if (state.loadWarning != null) {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    text = state.loadWarning,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onRetryLoad) { Text("Thử lại") }
            }
        } else if (trashedTables.isEmpty()) {
            Text(
                text = "Thùng rác đang trống. Bảng bạn chuyển vào thùng rác sẽ xuất hiện ở đây.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 12.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(trashedTables, key = { it.table.id }) { trashed ->
                    TrashedTableItem(
                        trashed = trashed,
                        deletedAt = clock.format(trashed.deletedAtMillis),
                        onRestore = { onRestore(trashed) },
                        onPermanentlyDelete = { onPermanentlyDelete(trashed) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TrashedTableItem(
    trashed: TrashedSavedTable,
    deletedAt: String,
    onRestore: () -> Unit,
    onPermanentlyDelete: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(trashed.table.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            text = "${trashed.table.tickets.size} ticket • Đã xoá: $deletedAt",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = trashed.table.jql,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row {
            OutlinedButton(onClick = onRestore) { Text("Khôi phục") }
            TextButton(onClick = onPermanentlyDelete) { Text("Xoá vĩnh viễn") }
        }
    }
}
