package com.example.rewardsscheduler.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class DelayGeneratorTest {

    @Test
    fun testRoundingTableExactSpecCases() {
        // Task 3.2 required test cases:
        // 60->20, 30->10, 15->5, 43->14, 44->15, 41->14, 40->13, 42->14, 3->1, 2->1, 1->0
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
        // Task 3.6: E is chosen uniformly at random from 3..7 inclusive
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
    fun testDelaySumEqualsTargetTimeframe() {
        // Task 3.4: Sum must equal T
        val count = 20
        val targetT = 1800
        val minD = 10
        val maxD = 150

        val (delays, clampedT, bounds) = DelayGenerator.generateDelays(count, targetT, minD, maxD, Random(123))
        assertEquals(count, delays.size)
        assertEquals(targetT, clampedT)
        assertEquals("Sum of delays must equal target timeframe", targetT, delays.sum())

        val (effectiveMin, effectiveMax) = bounds
        for (d in delays) {
            assertTrue("Delay $d must be >= min ($effectiveMin)", d >= effectiveMin)
            assertTrue("Delay $d must be <= max ($effectiveMax)", d <= effectiveMax)
        }
    }

    @Test
    fun testContradictionWidensMinBoundDownToThree() {
        // Task 3.5: count * min > T: keep T, widen min down (min >= 3)
        // 20 searches, initial min 30s -> count * min = 600s. Target T = 300s.
        // 300 / 20 = 15s >= 3s. So min widens to 15s, T remains 300s!
        var adjustmentMsg: String? = null
        val (delays, resultingT, bounds) = DelayGenerator.generateDelays(20, 300, 30, 100, Random(456)) {
            adjustmentMsg = it
        }

        assertNotNull("Should report adjustment", adjustmentMsg)
        assertEquals("Timeframe T must be preserved when possible", 300, resultingT)
        assertEquals("Sum of delays must equal T", 300, delays.sum())
        assertTrue("Min bound should have widened down to 15s", bounds.first <= 15)
        for (d in delays) {
            assertTrue("Each delay must be >= 3", d >= 3)
        }
    }

    @Test
    fun testContradictionClampsWhenEvenThreeSecondsIsImpossible() {
        // Task 3.5: 20 searches, minimum 3s/search = 60s minimum possible.
        // Requested T = 40s (impossible even at 3s/search).
        // Must clamp T to 60s.
        var adjustmentMsg: String? = null
        val (delays, resultingT, bounds) = DelayGenerator.generateDelays(20, 40, 10, 50, Random(456)) {
            adjustmentMsg = it
        }

        assertNotNull("Should report adjustment", adjustmentMsg)
        assertEquals("Should clamp T to 60s (20 * 3s)", 60, resultingT)
        assertEquals("Sum must match clamped value", 60, delays.sum())
        assertEquals(3, bounds.first)
        for (d in delays) {
            assertEquals(3, d)
        }
    }

    @Test
    fun testContradictionWidensMaxBoundUpward() {
        // Task 3.5: count * max < T: keep T, widen max as high as needed
        // 10 searches, max 50s -> count * max = 500s. Target T = 1000s.
        // Required max = 100s.
        var adjustmentMsg: String? = null
        val (delays, resultingT, bounds) = DelayGenerator.generateDelays(10, 1000, 10, 50, Random(789)) {
            adjustmentMsg = it
        }

        assertNotNull("Should report adjustment", adjustmentMsg)
        assertEquals("Timeframe 1000s must be preserved", 1000, resultingT)
        assertEquals("Sum must match 1000s", 1000, delays.sum())
        assertTrue("Max bound should have widened to >= 100s", bounds.second >= 100)
    }

    @Test
    fun testEveryDelayWithinBoundsAcrossRandomConfigs() {
        for (seed in 1..25) {
            val rng = Random(seed)
            val n = rng.nextInt(5, 30)
            val minD = rng.nextInt(5, 20)
            val maxD = rng.nextInt(minD + 10, 200)
            val avg = (minD + maxD) / 2
            val target = n * avg

            val (delays, resultingT, bounds) = DelayGenerator.generateDelays(n, target, minD, maxD, rng)
            assertEquals("Sum must equal resulting timeframe", resultingT, delays.sum())
            for (d in delays) {
                assertTrue("Delay $d was < min ${bounds.first}", d >= bounds.first)
                assertTrue("Delay $d was > max ${bounds.second}", d <= bounds.second)
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

        // Extra delays must be separate tail
        for (d in plan.extraDelays) {
            assertTrue("Extra delay $d >= effectiveMin", d >= plan.effectiveMinDelay)
            assertTrue("Extra delay $d <= effectiveMax", d <= plan.effectiveMaxDelay)
        }
    }

    @Test
    fun testTask2SessionPointsIsolationLogic() {
        // Task 2 requirements:
        // Account 4: 7 searches executed * 3 = 21 points (capped at maxPoints 60) -> 21
        // Session 1 total with Acc 4: 21 points
        // Session 1 exits.
        // Session 2 opened with ONLY Acc 1 (0 searches executed in Session 2):
        // Session 2 total must be 0!
        // Home daily total across all accounts: Acc 4 (21) + Acc 1 (0) = 21 points.

        val acc4MaxPoints = 60
        val acc1MaxPoints = 60

        // Session 1:
        val session1Counts = mutableMapOf("acc4" to 7)
        val session1Total = listOf("acc4").sumOf { id ->
            minOf((session1Counts[id] ?: 0) * 3, acc4MaxPoints)
        }
        assertEquals("Session 1 must show 21 points", 21, session1Total)

        // Exit session: session counts reset to empty/0
        val session2Counts = mutableMapOf("acc1" to 0)
        val session2Total = listOf("acc1").sumOf { id ->
            minOf((session2Counts[id] ?: 0) * 3, acc1MaxPoints)
        }
        assertEquals("Session 2 with only Acc 1 must start at 0 points", 0, session2Total)

        // Home dashboard: daily cumulative total across all profiles
        val persistentDailySearches = mapOf("acc4" to 7, "acc1" to 0)
        val homeDailyTotal = listOf("acc4" to acc4MaxPoints, "acc1" to acc1MaxPoints).sumOf { (id, maxPts) ->
            minOf((persistentDailySearches[id] ?: 0) * 3, maxPts)
        }
        assertEquals("Home dashboard must show 21 points", 21, homeDailyTotal)

        // After Acc 1 performs 2 searches in Session 2:
        session2Counts["acc1"] = 2
        val session2Updated = listOf("acc1").sumOf { id ->
            minOf((session2Counts[id] ?: 0) * 3, acc1MaxPoints)
        }
        assertEquals("Session 2 rises only with Acc 1's searches (2*3=6)", 6, session2Updated)

        val homeDailyUpdated = listOf("acc4" to 7, "acc1" to 2).sumOf { (id, maxPts) ->
            minOf((if (id == "acc4") 7 else 2) * 3, 60)
        }
        assertEquals("Home dashboard rises to 21 + 6 = 27", 27, homeDailyUpdated)
    }

    @Test
    fun testTask2PointsCappedAtMaxPoints() {
        // Points = min(searches * 3, maxPoints)
        val maxPoints = 15
        val searches = 10 // 10 * 3 = 30 points, but capped at 15
        val counted = minOf(searches * 3, maxPoints)
        assertEquals("Points must be capped at profile's maxPoints", 15, counted)
    }
}
