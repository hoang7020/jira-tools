package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class BurndownSnapshotTest {
    @Test
    fun acceptsValidSnapshotCounts() {
        val snapshot =
            BurndownSnapshot(
                recordedAtMillis = 1234L,
                ticketCount = 5,
                remainingCount = 3,
                unknownCategoryCount = 1,
            )

        assertEquals(1234L, snapshot.recordedAtMillis)
        assertEquals(5, snapshot.ticketCount)
        assertEquals(3, snapshot.remainingCount)
        assertEquals(1, snapshot.unknownCategoryCount)
    }

    @Test
    fun rejectsNegativeTicketCount() {
        assertFailsWith<IllegalArgumentException> {
            BurndownSnapshot(recordedAtMillis = 0, ticketCount = -1, remainingCount = 0, unknownCategoryCount = 0)
        }
    }

    @Test
    fun rejectsRemainingCountOutsideTicketCount() {
        assertFailsWith<IllegalArgumentException> {
            BurndownSnapshot(recordedAtMillis = 0, ticketCount = 2, remainingCount = 3, unknownCategoryCount = 0)
        }
    }

    @Test
    fun rejectsNegativeRemainingCount() {
        assertFailsWith<IllegalArgumentException> {
            BurndownSnapshot(recordedAtMillis = 0, ticketCount = 2, remainingCount = -1, unknownCategoryCount = 0)
        }
    }

    @Test
    fun rejectsUnknownCountOutsideRemainingCount() {
        assertFailsWith<IllegalArgumentException> {
            BurndownSnapshot(recordedAtMillis = 0, ticketCount = 2, remainingCount = 1, unknownCategoryCount = 2)
        }
    }

    @Test
    fun rejectsNegativeUnknownCategoryCount() {
        assertFailsWith<IllegalArgumentException> {
            BurndownSnapshot(recordedAtMillis = 0, ticketCount = 2, remainingCount = 1, unknownCategoryCount = -1)
        }
    }
}
