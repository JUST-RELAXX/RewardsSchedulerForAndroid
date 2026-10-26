package com.example.rewardsscheduler.engine

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Delay & Search-Count Generator.
 * Implements the EXACT mathematical spec from Task 3:
 * 3.2: Required searches N = (maxPoints + 1) / 3 (integer division).
 * 3.3: Timeframe T = (profile override ?: global) * 60 seconds.
 * 3.4: Generate exactly N delays summing to T within [min, max].
 * 3.5: Contradiction handling (keep T, widen bounds down to min 3s, clamp T only if impossible).
 * 3.6: Extras E chosen uniformly in 3..7 inclusive, delays random in [min, max].
 */
object DelayGenerator {

    /**
     * Compute required searches N for a given maxPoints cap.
     * Non-negative integer division: N = (maxPoints + 1) / 3.
     * Rounding: fraction < 0.5 rounds down, >= 0.5 rounds up.
     * Must hold:
     *   60->20, 30->10, 15->5, 43->14, 44->15, 41->14, 40->13, 42->14, 3->1, 2->1, 1->0.
     */
    fun calculateRequiredSearches(maxPoints: Int): Int {
        if (maxPoints <= 0) return 0
        return (maxPoints + 1) / 3
    }

    /**
     * Extra searches E: chosen uniformly at random from 3..7 inclusive.
     */
    fun generateExtraSearches(random: Random = Random.Default): Int {
        return random.nextInt(3, 8) // 3, 4, 5, 6, 7
    }

    /**
     * Compute default [minDelay, maxDelay] bounds based on PC app:
     *   avg = targetSeconds / searches
     *   min = max(3, floor(avg * 0.3))
     *   max = max(min + 1, ceil(avg * 1.7))
     */
    fun computeAutoBounds(targetSeconds: Int, searches: Int): Pair<Int, Int> {
        if (searches <= 0) return 3 to 60
        val avg = targetSeconds.toDouble() / searches
        val minVal = max(3, floor(avg * 0.3).toInt())
        val maxVal = max(minVal + 1, ceil(avg * 1.7).toInt())
        return minVal to maxVal
    }

    /**
     * Generate exactly N delays that sum to [timeframeSeconds],
     * with each delay in [[minDelay], [maxDelay]].
     *
     * Handles contradictions per 3.5:
     * Keep user's T, widen bounds until T is reachable (min no lower than 3s, max as high as needed),
     * and only if impossible clamp T to nearest feasible value.
     */
    fun generateDelays(
        count: Int,
        timeframeSeconds: Int,
        initialMinDelay: Int,
        initialMaxDelay: Int,
        random: Random = Random.Default,
        onAdjustment: (String) -> Unit = {}
    ): Triple<List<Int>, Int, Pair<Int, Int>> {
        if (count <= 0) return Triple(emptyList(), 0, initialMinDelay to initialMaxDelay)

        var min = max(3, initialMinDelay)
        var max = max(min + 1, initialMaxDelay)
        var T = max(1, timeframeSeconds)

        // 3.5 Contradiction resolution
        if (count * min > T) {
            val possibleMin = T / count
            if (possibleMin >= 3) {
                min = possibleMin
                onAdjustment("Widened min delay bound down to ${min}s to fit ${T}s timeframe.")
            } else {
                min = 3
                val clampedT = count * 3
                onAdjustment("Timeframe ${T}s is too short for $count searches (minimum 3s/search). Clamped timeframe to ${clampedT}s.")
                T = clampedT
            }
            if (max < min + 1) {
                max = min + 1
            }
        } else if (count * max < T) {
            val requiredMax = ceil(T.toDouble() / count).toInt()
            max = max(max, requiredMax)
            onAdjustment("Widened max delay bound up to ${max}s to accommodate ${T}s timeframe.")
        }

        // 3.4 Algorithm
        val delays = IntArray(count) { min }
        var remaining = T - count * min

        var iterations = 0
        val maxIterations = count * 5000
        while (remaining > 0 && iterations < maxIterations) {
            iterations++
            val i = random.nextInt(count)
            val maxAdd = minOf(remaining, max - delays[i])
            if (maxAdd <= 0) continue
            val add = random.nextInt(1, maxAdd + 1)
            delays[i] += add
            remaining -= add
        }

        // Deterministic cleanup if any remaining
        if (remaining > 0) {
            for (i in 0 until count) {
                if (remaining <= 0) break
                val headroom = max - delays[i]
                if (headroom > 0) {
                    val add = minOf(remaining, headroom)
                    delays[i] += add
                    remaining -= add
                }
            }
        }

        // Fisher-Yates shuffle
        for (i in count - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val tmp = delays[i]
            delays[i] = delays[j]
            delays[j] = tmp
        }

        // Pairwise smoothing
        for (i in 0 until count - 1 step 2) {
            if (random.nextDouble() > 0.5) {
                val maxDiff = minOf(delays[i] - min, max - delays[i + 1], 3)
                if (maxDiff > 0) {
                    val diff = random.nextInt(0, maxDiff + 1)
                    if (diff > 0) {
                        delays[i] -= diff
                        delays[i + 1] += diff
                    }
                }
            }
        }

        return Triple(delays.toList(), T, min to max)
    }

    /**
     * Generate extra search delays drawn uniformly from [minDelay, maxDelay].
     */
    fun generateExtraDelays(
        extraCount: Int,
        minDelay: Int,
        maxDelay: Int,
        random: Random = Random.Default
    ): List<Int> {
        val effectiveMin = max(3, minDelay)
        val effectiveMax = max(effectiveMin, maxDelay)
        return List(extraCount) {
            if (effectiveMin == effectiveMax) effectiveMin
            else random.nextInt(effectiveMin, effectiveMax + 1)
        }
    }

    /**
     * Full delay execution plan for a search session.
     */
    data class Plan(
        val startIndex: Int = 0,
        val requiredCount: Int,
        val extraCount: Int,
        val requiredDelays: List<Int>,
        val extraDelays: List<Int>,
        val totalSearches: Int = requiredCount + extraCount,
        val clampedTimeframeSeconds: Int,
        val effectiveMinDelay: Int,
        val effectiveMaxDelay: Int
    ) {
        val allDelays: List<Int> get() = requiredDelays + extraDelays
    }

    /**
     * Create full plan for a session.
     */
    fun createPlan(
        maxPoints: Int = 60,
        timeframeMinutes: Int = 30,
        customMinDelay: Int? = null,
        customMaxDelay: Int? = null,
        random: Random = Random.Default,
        onAdjustment: (String) -> Unit = {}
    ): Plan {
        val N = calculateRequiredSearches(maxPoints)
        val E = generateExtraSearches(random)
        val T = timeframeMinutes * 60

        val (initialMin, initialMax) = if (customMinDelay != null && customMaxDelay != null) {
            customMinDelay to customMaxDelay
        } else {
            computeAutoBounds(T, N)
        }

        val (requiredDelays, clampedT, resolvedBounds) = generateDelays(
            count = N,
            timeframeSeconds = T,
            initialMinDelay = initialMin,
            initialMaxDelay = initialMax,
            random = random,
            onAdjustment = onAdjustment
        )

        val (effectiveMin, effectiveMax) = resolvedBounds
        val extraDelays = generateExtraDelays(E, effectiveMin, effectiveMax, random)

        return Plan(
            startIndex = 0,
            requiredCount = N,
            extraCount = E,
            requiredDelays = requiredDelays,
            extraDelays = extraDelays,
            clampedTimeframeSeconds = clampedT,
            effectiveMinDelay = effectiveMin,
            effectiveMaxDelay = effectiveMax
        )
    }
}
