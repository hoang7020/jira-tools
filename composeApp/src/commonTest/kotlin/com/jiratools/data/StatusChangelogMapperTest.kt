package com.jiratools.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class StatusChangelogMapperTest {
    @Test
    fun picksNewestCloseResolveOrDoneTransitionOnly() {
        val selected =
            StatusChangelogMapper.latestRealDueAtMillis(
                listOf(
                    issueHistory(
                        "100",
                        history("2026-01-01T10:00:00Z", "Open", "Close"),
                        history("2026-01-02T10:00:00Z", "Close", "In Progress"),
                        history("2026-01-03T10:00:00Z", "In Progress", " rEsOlVe "),
                        history("2026-01-04T10:00:00Z", "Resolve", " dOnE "),
                    ),
                ),
            )

        assertEquals(Instant.parse("2026-01-04T10:00:00Z").toEpochMilliseconds(), selected["100"])
    }

    @Test
    fun missingInvalidOrIneligibleHistoryProducesNoRealDueDate() {
        assertTrue(StatusChangelogMapper.latestRealDueAtMillis(emptyList()).isEmpty())
        assertTrue(
            StatusChangelogMapper
                .latestRealDueAtMillis(
                    listOf(issueHistory("100", history("2026-01-01T10:00:00Z", "Open", "Done Review"))),
                ).isEmpty(),
        )
        assertNull(
            StatusChangelogMapper.latestRealDueAtMillis(
                listOf(issueHistory("100", history("not-a-date", "Open", "Close"))),
            )["100"],
        )
    }

    @Test
    fun combinesMultipleChangelogPagesForTheSameIssue() {
        val selected =
            StatusChangelogMapper.latestRealDueAtMillis(
                listOf(
                    issueHistory("100", history("2026-01-01T10:00:00Z", "Open", "Close")),
                    issueHistory("100", history("2026-01-03T10:00:00Z", "In Progress", "Resolve")),
                ),
            )

        assertEquals(Instant.parse("2026-01-03T10:00:00Z").toEpochMilliseconds(), selected["100"])
    }

    @Test
    fun doneTransitionMatchesTrimmedCaseInsensitiveName() {
        val selected =
            StatusChangelogMapper.latestRealDueAtMillis(
                listOf(issueHistory("100", history("2026-01-05T10:00:00Z", "In Progress", "  DONE  "))),
            )

        assertEquals(Instant.parse("2026-01-05T10:00:00Z").toEpochMilliseconds(), selected["100"])
    }

    @Test
    fun newestValidTransitionWinsAcrossDoneCloseAndResolve() {
        val selected =
            StatusChangelogMapper.latestRealDueAtMillis(
                listOf(
                    issueHistory(
                        "100",
                        history("2026-01-01T10:00:00Z", "In Progress", "Done"),
                        history("2026-01-02T10:00:00Z", "Done", "In Progress"),
                        history("2026-01-03T10:00:00Z", "In Progress", "Resolve"),
                        history("2026-01-04T10:00:00Z", "Resolve", "Close"),
                    ),
                ),
            )

        assertEquals(Instant.parse("2026-01-04T10:00:00Z").toEpochMilliseconds(), selected["100"])
    }

    private fun history(
        created: String,
        from: String,
        to: String,
    ) = ChangelogHistoryDto(
        created = kotlinx.serialization.json.JsonPrimitive(created),
        items = listOf(ChangelogItemDto(field = "status", fromString = from, toString = to)),
    )

    private fun issueHistory(
        issueId: String,
        vararg histories: ChangelogHistoryDto,
    ) = IssueChangeLogDto(issueId = issueId, changeHistories = histories.toList())
}
