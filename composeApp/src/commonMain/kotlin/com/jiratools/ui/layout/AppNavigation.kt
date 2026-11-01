package com.jiratools.ui.layout

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class AppDestination(
    val label: String,
    val symbol: String,
) {
    QUERY("Truy vấn", "⌕"),
    SAVED_TABLES("Bảng đã lưu", "▤"),
    TRASH("Thùng rác", "▱"),
    SETTINGS("Cài đặt", "⚙"),
}

@Composable
fun AppNavigation(
    destination: AppDestination,
    collapsed: Boolean,
    onDestinationSelected: (AppDestination) -> Unit,
    onToggleCollapsed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = if (collapsed) 80.dp else 248.dp
    Surface(
        modifier = modifier.width(width).fillMaxHeight().animateContentSize(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .padding(horizontal = if (collapsed) 0.dp else 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(start = if (collapsed) 0.dp else 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.SpaceBetween,
            ) {
                if (!collapsed) {
                    Text(
                        text = "Jira Tools",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (collapsed) {
                    CircularNavigationButton(
                        contentDescription = "Mở rộng điều hướng",
                        selected = false,
                        onClick = onToggleCollapsed,
                    ) {
                        Text("›", style = MaterialTheme.typography.titleLarge)
                    }
                } else {
                    IconButton(
                        onClick = onToggleCollapsed,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Thu gọn điều hướng"
                            },
                    ) {
                        Text("‹", style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
            HorizontalDivider()
            AppDestination.entries.forEach { item ->
                if (collapsed) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularNavigationButton(
                            contentDescription = item.label,
                            selected = item == destination,
                            onClick = { onDestinationSelected(item) },
                        ) {
                            Text(
                                text = item.symbol,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                } else {
                    NavigationDrawerItem(
                        label = { Text(item.label, maxLines = 1) },
                        selected = item == destination,
                        onClick = { onDestinationSelected(item) },
                        icon = {
                            Text(
                                text = item.symbol,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.semantics { contentDescription = item.label },
                            )
                        },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .semantics { contentDescription = item.label },
                    )
                }
            }
            if (!collapsed) {
                Text(
                    text = "Jira issue workspace",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CircularNavigationButton(
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .size(56.dp)
                .semantics {
                    this.contentDescription = contentDescription
                    this.selected = selected
                },
        shape = CircleShape,
        color =
            if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
        contentColor =
            if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}
