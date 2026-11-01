package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class WorkDurationTest {
    @Test
    fun parsesSupportedSingleAndCombinedUnits() {
        listOf("15m", "1h", "1D", "1s", "0.5h", "1d 2h", "0").forEach { raw ->
            assertNotNull(WorkDuration.parse(raw), "Expected '$raw' to parse")
        }
    }

    @Test
    fun normalizesWorkdayAndWorkweekUnits() {
        assertEquals(WorkDuration.parse("8h"), WorkDuration.parse("1d"))
        assertEquals(WorkDuration.parse("40h"), WorkDuration.parse("1w"))
        assertEquals(WorkDuration.parse("30m"), WorkDuration.parse("0.5h"))
    }

    @Test
    fun rejectsEmptyNegativeMalformedUnsupportedAndOverflowValues() {
        listOf("", " ", "-1h", "1.5.2h", "1x", "2h trailing", "999999999999999999999999999999w").forEach { raw ->
            assertNull(WorkDuration.parse(raw), "Expected '$raw' to be rejected")
        }
    }
}
