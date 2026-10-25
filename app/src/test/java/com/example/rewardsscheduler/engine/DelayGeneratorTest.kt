package com.example.rewardsscheduler.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DelayGeneratorTest {

    @Test
    fun testRoundingTableExactSpecCases() {
        // Spec required test cases:
        // 60→20, 30→10, 15→5, 43→14, 44→15, 41→14, 40→13, 42→14, 3→1, 2→1, 1→0
        val testCases = listOf(
            60 to 20,
            30 to 10,
            15 to 5,
            43 to 14,
            44 to 15,
            41 to 14,
            40 to 13,
            42 to 14,
            3 to 1,
            2 to 1,
            1 to 0,
            0 to 0
        )

        for ((maxPoints, expectedSearches) in testCases) {
            val result = DelayGenerator.calculateRequiredSearches(maxPoints)
            assertEquals("Failed for maxPoints = $maxPoints", expectedSearches, result)
        }
    }

    @Test
    fun testExtraSearchesAlwaysWithinThreeToSeven() {
        val random = Random(42)
        val seen = mutableSetOf<Int>()
        for (i in 0 until 1000) {
            val e = DelayGenerator.generateExtraSearches(random)
            assertTrue("E must be >= 3 but was $e", e >= 3)
            assertTrue("E must be <= 7 but was $e", e <= 7)
            seen.add(e)
        }
        assertEquals("Should cover all possible extra values [3, 4, 5, 6, 7]", setOf(3, 4, 5, 6, 7), seen)
    }

    @Test
    fun testDelaySumEqualsTargetAfterFeasibilityClamp() {
        // Test standard 30 min (1800s) for 20 searches, min=10, max=150
        val count = 20
        val targetT = 1800
        val minD = 10
        val maxD = 150

        val (delays, clampedT) = DelayGenerator.generateDelays(count, targetT, minD, maxD, Random(123))
        assertEquals(count, delays.size)
        assertEquals(targetT, clampedT)
        assertEquals("Sum of delays must equal target timeframe", targetT, delays.sum())

        for (d in delays) {
            assertTrue("Delay $d must be >= min ($minD)", d >= minD)
            assertTrue("Delay $d must be <= max ($maxD)", d <= maxD)
        }
    }

    @Test
    fun testDelayClampingWhenTooShort() {
        // 20 searches, min 30s -> minimum possible = 600s
        // Requested timeframe = 300s (too short)
        var adjustmentMsg: String? = null
        val (delays, clampedT) = DelayGenerator.generateDelays(20, 300, 30, 100, Random(456)) {
            adjustmentMsg = it
        }

        assertNotNull("Should report adjustment", adjustmentMsg)
        assertEquals("Should clamp to 600s", 600, clampedT)
        assertEquals("Sum must match clamped value", 600, delays.sum())
        assertTrue(delays.all { it >= 30 })
    }

    @Test
    fun testDelayClampingWhenTooLong() {
        // 10 searches, max 50s -> maximum possible = 500s
        // Requested timeframe = 1000s (too long)
        var adjustmentMsg: String? = null
        val (delays, clampedT) = DelayGenerator.generateDelays(10, 1000, 10, 50, Random(789)) {
            adjustmentMsg = it
        }

        assertNotNull("Should report adjustment", adjustmentMsg)
        assertEquals("Should clamp to 500s", 500, clampedT)
        assertEquals("Sum must match clamped value", 500, delays.sum())
        assertTrue(delays.all { it <= 50 })
    }

    @Test
    fun testEveryDelayWithinBounds() {
        for (seed in 1..20) {
            val rng = Random(seed)
            val n = rng.nextInt(5, 30)
            val minD = rng.nextInt(5, 20)
            val maxD = rng.nextInt(minD + 10, 200)
            val avg = (minD + maxD) / 2
            val target = n * avg

            val (delays, clampedT) = DelayGenerator.generateDelays(n, target, minD, maxD, rng)
            assertEquals("Sum must equal clamped", clampedT, delays.sum())
            for (d in delays) {
                assertTrue("Delay $d was < min $minD", d >= minD)
                assertTrue("Delay $d was > max $maxD", d <= maxD)
            }
        }
    }

    @Test
    fun testFullPlanStartsAtZeroAndHasCorrectTail() {
        val plan = DelayGenerator.createPlan(
            maxPoints = 60,
            timeframeMinutes = 30,
            random = Random(999)
        )

        assertEquals("StartIndex must be 0", 0, plan.startIndex)
        assertEquals("Required searches for 60pts must be 20", 20, plan.requiredCount)
        assertTrue("Extra searches must be 3..7", plan.extraCount in 3..7)
        assertEquals("Total searches must be required + extra", plan.requiredCount + plan.extraCount, plan.totalSearches)
        assertEquals(plan.requiredCount, plan.requiredDelays.size)
        assertEquals(plan.extraCount, plan.extraDelays.size)
        assertEquals(plan.clampedTimeframeSeconds, plan.requiredDelays.sum())
    }
}
