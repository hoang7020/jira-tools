package com.jiratools.domain

fun Ticket.isOverdueAt(nowMillis: Long): Boolean {
    val dueMillis = dynamicDueAtMillis ?: return false
    return dueMillis / SECOND_IN_MILLIS < nowMillis / SECOND_IN_MILLIS
}

fun shouldHighlightOverdueRow(
    enabled: Boolean,
    ticket: Ticket,
    nowMillis: Long,
): Boolean = enabled && ticket.isOverdueAt(nowMillis)

private const val SECOND_IN_MILLIS = 1_000L
