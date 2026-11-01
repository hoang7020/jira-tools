package com.jiratools.domain

enum class QueryTreeBranch(
    val marker: String?,
) {
    NONE(null),
    NON_LAST("\u251c"),
    LAST("\u2514"),
}

fun groupQueryResultTickets(tickets: List<Ticket>): List<Ticket> {
    val knownChildParentKeys = tickets.mapNotNull { it.parentKey }.toSet()
    val normalizedTickets =
        tickets.map { ticket ->
            ticket.copy(isParent = ticket.isParent || ticket.key in knownChildParentKeys)
        }
    val byKey = normalizedTickets.associateBy { it.key }
    val parentKeys =
        byKey.keys.filterTo(mutableSetOf()) { key ->
            normalizedTickets.any { ticket ->
                ticket.parentKey == key && byKey[ticket.parentKey] != null
            }
        }
    val grouped = mutableListOf<Ticket>()
    val groupedKeys = mutableSetOf<String>()

    normalizedTickets.forEach { ticket ->
        if (ticket.key in parentKeys) {
            if (groupedKeys.add(ticket.key)) grouped += ticket
            normalizedTickets.forEach { child ->
                if (child.parentKey == ticket.key && child.isSubtask && groupedKeys.add(child.key)) {
                    grouped += child
                }
            }
            return@forEach
        }
        val hasPresentParent = ticket.parentKey != null && ticket.parentKey in parentKeys
        if (!hasPresentParent && groupedKeys.add(ticket.key)) {
            grouped += ticket
        }
    }
    return grouped
}

fun queryTreeBranch(
    tickets: List<Ticket>,
    ticket: Ticket,
): QueryTreeBranch {
    val parentKey = ticket.parentKey ?: return QueryTreeBranch.NONE
    if (!ticket.isSubtask) return QueryTreeBranch.NONE
    val siblings =
        tickets.filter {
            it.isSubtask && it.parentKey == parentKey
        }
    val index = siblings.indexOfFirst { it.key == ticket.key }
    if (index < 0) return QueryTreeBranch.NONE
    return if (index == siblings.lastIndex) QueryTreeBranch.LAST else QueryTreeBranch.NON_LAST
}
