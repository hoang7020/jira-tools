package com.jiratools.data

import kotlin.time.Instant

object StatusChangelogMapper {
    fun latestRealDueAtMillis(histories: List<IssueChangeLogDto>): Map<String, Long> =
        histories
            .groupBy { it.issueId?.trim()?.takeIf(String::isNotEmpty) }
            .mapNotNull { (issueId, issues) ->
                if (issueId == null) return@mapNotNull null
                val latest =
                    issues
                        .flatMap { it.changeHistories }
                        .mapNotNull { history ->
                            val atMillis = parseCreatedAtMillis(history.created) ?: return@mapNotNull null
                            val qualifies =
                                history.items.any { item ->
                                    val isStatus =
                                        item.fieldId?.equals("status", ignoreCase = true) == true ||
                                            item.field?.equals("status", ignoreCase = true) == true
                                    isStatus && isRealDueStatus(item.toString)
                                }
                            atMillis.takeIf { qualifies }
                        }.maxOrNull() ?: return@mapNotNull null
                issueId to latest
            }.toMap()

    private fun parseCreatedAtMillis(created: kotlinx.serialization.json.JsonPrimitive?): Long? {
        if (created == null) return null
        val value = created.content
        val numeric = value.toLongOrNull()
        if (numeric != null) {
            return if (numeric >= MIN_EPOCH_MILLIS) numeric else numeric.checkedSecondsToMillis()
        }
        val normalized =
            Regex("([+-]\\d{2})(\\d{2})$").replace(value) {
                "${it.groupValues[1]}:${it.groupValues[2]}"
            }
        return try {
            Instant.parse(normalized).toEpochMilliseconds()
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun Long.checkedSecondsToMillis(): Long? {
        if (this > Long.MAX_VALUE / 1000 || this < Long.MIN_VALUE / 1000) return null
        return this * 1000
    }

    private fun isRealDueStatus(status: String?): Boolean = status?.trim()?.lowercase() in REAL_DUE_STATUSES

    private const val MIN_EPOCH_MILLIS = 100_000_000_000L
    private val REAL_DUE_STATUSES = setOf("close", "resolve", "done")
}
