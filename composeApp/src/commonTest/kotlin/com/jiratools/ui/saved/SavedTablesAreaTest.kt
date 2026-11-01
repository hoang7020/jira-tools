package com.jiratools.ui.saved

import com.jiratools.domain.Ticket
import com.jiratools.domain.shouldHighlightOverdueRow
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SavedTablesAreaTest {
    @Test
    fun savedTableRowContractRequiresHighlightEnabled() {
        val ticket = ticket(dueMillis = 11_999L)

        assertFalse(shouldHighlightOverdueRow(enabled = false, ticket = ticket, nowMillis = 12_000L))
        assertFalse(shouldHighlightOverdueRow(enabled = true, ticket = ticket, nowMillis = 11_999L))
        assertTrue(shouldHighlightOverdueRow(enabled = true, ticket = ticket, nowMillis = 12_000L))
    }

    @Test
    fun savedTableRowTransitionsAtOneSecondBoundary() {
        val ticket = ticket(dueMillis = 12_000L)

        assertFalse(shouldHighlightOverdueRow(enabled = true, ticket = ticket, nowMillis = 11_999L))
        assertFalse(shouldHighlightOverdueRow(enabled = true, ticket = ticket, nowMillis = 12_000L))
        assertTrue(shouldHighlightOverdueRow(enabled = true, ticket = ticket, nowMillis = 13_000L))
    }

    private fun ticket(dueMillis: Long): Ticket =
        Ticket(
            key = "TEST-1",
            summary = "Test",
            status = "Open",
            dynamicDueAtMillis = dueMillis,
        )
}
