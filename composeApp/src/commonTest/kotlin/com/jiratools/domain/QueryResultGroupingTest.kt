package com.jiratools.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class QueryResultGroupingTest {
    @Test
    fun parentAppearsBeforeItsSubtasks() {
        val child = ticket("CHILD-1", parentKey = "PARENT-1", isSubtask = true)
        val parent = ticket("PARENT-1", isParent = true)

        assertEquals(
            listOf("PARENT-1", "CHILD-1"),
            groupQueryResultTickets(listOf(child, parent)).map { it.key },
        )
    }

    @Test
    fun unrelatedTicketDoesNotMoveInsideParentGroup() {
        val other = ticket("OTHER-1")
        val child = ticket("CHILD-1", parentKey = "PARENT-1", isSubtask = true)
        val parent = ticket("PARENT-1", isParent = true)

        assertEquals(
            listOf("OTHER-1", "PARENT-1", "CHILD-1"),
            groupQueryResultTickets(listOf(child, other, parent)).map { it.key },
        )
    }

    @Test
    fun siblingSubtasksKeepInputOrder() {
        val first = ticket("CHILD-1", parentKey = "PARENT-1", isSubtask = true)
        val second = ticket("CHILD-2", parentKey = "PARENT-1", isSubtask = true)
        val parent = ticket("PARENT-1", isParent = true)

        assertEquals(
            listOf("PARENT-1", "CHILD-1", "CHILD-2"),
            groupQueryResultTickets(listOf(first, second, parent)).map { it.key },
        )
    }

    @Test
    fun orphanSubtaskRemainsWithoutSyntheticParent() {
        val orphan = ticket("CHILD-1", parentKey = "MISSING-1", isSubtask = true)

        assertEquals(listOf("CHILD-1"), groupQueryResultTickets(listOf(orphan)).map { it.key })
    }

    @Test
    fun treeBranchesMarkNonLastAndLastSubtask() {
        val tickets =
            groupQueryResultTickets(
                listOf(
                    ticket("CHILD-1", parentKey = "PARENT-1", isSubtask = true),
                    ticket("CHILD-2", parentKey = "PARENT-1", isSubtask = true),
                    ticket("PARENT-1", isParent = true),
                ),
            )

        assertEquals(QueryTreeBranch.NON_LAST, queryTreeBranch(tickets, tickets[1]))
        assertEquals(QueryTreeBranch.LAST, queryTreeBranch(tickets, tickets[2]))
    }

    private fun ticket(
        key: String,
        parentKey: String? = null,
        isSubtask: Boolean = false,
        isParent: Boolean = false,
    ): Ticket =
        Ticket(
            key = key,
            summary = key,
            status = "Open",
            parentKey = parentKey,
            isSubtask = isSubtask,
            isParent = isParent,
        )
}
