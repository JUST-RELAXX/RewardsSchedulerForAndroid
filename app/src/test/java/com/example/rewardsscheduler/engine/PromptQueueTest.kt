package com.example.rewardsscheduler.engine

import org.junit.Assert.*
import org.junit.Test

class PromptQueueTest {

    @Test
    fun testFallbackQueriesHasAllQueries() {
        assertEquals("Fallback queries count must be 60", 60, PromptQueue.FALLBACK_QUERIES.size)
        assertTrue("No query should be blank", PromptQueue.FALLBACK_QUERIES.all { it.isNotBlank() })
    }

    @Test
    fun testBuildSessionQueuePrioritizesFetchedPromptsFromIndexZero() {
        val fetched = listOf("alpha prompt", "beta prompt", "gamma prompt")
        val totalNeeded = 10

        val queue = PromptQueue.buildSessionQueue(fetched, totalNeeded)

        assertEquals(totalNeeded, queue.size)
        assertEquals("First prompt must be fetched[0]", "alpha prompt", queue[0])
        assertEquals("Second prompt must be fetched[1]", "beta prompt", queue[1])
        assertEquals("Third prompt must be fetched[2]", "gamma prompt", queue[2])
    }

    @Test
    fun testBuildSessionQueueNoDuplicates() {
        val fetched = listOf("query a", "query b")
        val totalNeeded = 30
        val queue = PromptQueue.buildSessionQueue(fetched, totalNeeded)

        val uniqueLower = queue.map { it.lowercase() }.toSet()
        assertEquals("All queries in queue must be unique", queue.size, uniqueLower.size)
    }

    @Test
    fun testBuildSessionQueueCanFulfillLargeCountsWithoutStalling() {
        val fetched = emptyList<String>()
        val totalNeeded = 80
        val queue = PromptQueue.buildSessionQueue(fetched, totalNeeded)

        assertEquals(80, queue.size)
        val uniqueLower = queue.map { it.lowercase() }.toSet()
        assertEquals(80, uniqueLower.size)
    }
}
