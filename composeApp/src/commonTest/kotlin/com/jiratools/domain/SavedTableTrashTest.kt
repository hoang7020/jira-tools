package com.jiratools.domain

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedTableTrashTest {
    @Test
    fun trashedSavedTableSerializationPreservesTableAndDeletionTime() {
        val trashed =
            TrashedSavedTable(
                table =
                    SavedTable(
                        id = "table-1",
                        name = "Báo cáo",
                        jql = "project = HSP",
                        jiraBaseUrl = "https://example.atlassian.net",
                        savedAtMillis = 100L,
                        tickets = listOf(Ticket(key = "HSP-1", summary = "Ticket", status = "Done")),
                        burndownSnapshots = listOf(BurndownSnapshot(90L, 1, 0, 0)),
                    ),
                deletedAtMillis = 200L,
            )

        val encoded = Json.encodeToString(trashed)
        val decoded = Json.decodeFromString<TrashedSavedTable>(encoded)

        assertEquals(trashed, decoded)
    }
}
