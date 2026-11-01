package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TicketOverdueTest {
    @Test
    fun overdueUsesSecondPrecision() {
        assertTrue(ticket(dueMillis = 10_999L).isOverdueAt(nowMillis = 11_000L))
        assertFalse(ticket(dueMillis = 11_000L).isOverdueAt(nowMillis = 11_999L))
    }

    @Test
    fun equalSecondIsNotOverdue() {
        assertFalse(ticket(dueMillis = 11_999L).isOverdueAt(nowMillis = 11_000L))
        assertFalse(ticket(dueMillis = 11_000L).isOverdueAt(nowMillis = 11_999L))
    }

    @Test
    fun futureAndMissingDueDatesAreNotOverdue() {
        assertFalse(ticket(dueMillis = 12_000L).isOverdueAt(nowMillis = 11_999L))
        assertFalse(ticket(dueMillis = null).isOverdueAt(nowMillis = 99_999L))
    }

    private fun ticket(dueMillis: Long?): Ticket =
        Ticket(
            key = "TEST-1",
            summary = "Test",
            status = "Open",
            dynamicDueAtMillis = dueMillis,
        )
}
