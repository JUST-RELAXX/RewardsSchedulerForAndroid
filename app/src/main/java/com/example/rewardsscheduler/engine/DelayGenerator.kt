package com.example.rewardsscheduler.engine

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Delay & Search-Count Generator.
 * Implements the EXACT mathematical spec from Prompt.md:
 * 1. Every run starts from prompt #1 (index 0).
 * 2. Each search = 3 points. Required searches N = (maxPoints + 1) / 3 (integer division).
 * 3. Timeframe T: generate exactly N delays summing to T seconds within [minDelay, maxDelay]
 *    using the chaos generation algorithm. Feasibility clamped to nearest feasible T with logging.
 * 4. Extra searches E: chosen uniformly at random from 3..7 inclusive, separate tail after T.
 * 5. Prompt queue assembly: combining API prompts and fallback offline queries without repeats.
 */
object DelayGenerator {

    /**
     * Compute required searches N for a given maxPoints cap.
     * Rounding: fraction < 0.5 rounds down, >= 0.5 rounds up.
     * For non-negative integers: N = (maxPoints + 1) / 3.
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
     * Compute default [minDelay, maxDelay] bounds if not manually configured.
     * Based on PC app:
     *   avg = targetSeconds / searches
     *   min = max(0, floor(avg * 0.3))
     *   max = min(500, ceil(avg * 1.7))
     */
    fun computeAutoBounds(targetSeconds: Int, searches: Int): Pair<Int, Int> {
        if (searches <= 0) return 5 to 60
        val avg = targetSeconds.toDouble() / searches
        val minVal = max(0, floor(avg * 0.3).toInt())
        val maxVal = min(500, max(minVal + 1, ceil(avg * 1.7).toInt()))
        return minVal to maxVal
    }

    /**
     * Generate exactly N delays that sum to [timeframeSeconds],
     * with each delay in [[minDelay], [maxDelay]].
     *
     * If [timeframeSeconds] is infeasible (N * minDelay > T or N * maxDelay < T),
     * clamps T to the nearest feasible value and reports via [onAdjustment].
     */
    fun generateDelays(
        count: Int,
        timeframeSeconds: Int,
        minDelay: Int,
        maxDelay: Int,
        random: Random = Random.Default,
        onAdjustment: (String) -> Unit = {}
    ): Pair<List<Int>, Int> {
        if (count <= 0) return emptyList<Int>() to 0

        val effectiveMin = max(0, minDelay)
        val effectiveMax = max(effectiveMin, maxDelay)

        val minPossible = count * effectiveMin
        val maxPossible = count * effectiveMax

        var targetSum = timeframeSeconds
        if (targetSum < minPossible) {
            onAdjustment("Timeframe ${timeframeSeconds}s is too short for $count searches (min ${minPossible}s). Clamped to ${minPossible}s.")
            targetSum = minPossible
        } else if (targetSum > maxPossible) {
            onAdjustment("Timeframe ${timeframeSeconds}s is too long for $count searches (max ${maxPossible}s). Clamped to ${maxPossible}s.")
            targetSum = maxPossible
        }

        // Start every delay at minDelay
        val delays = IntArray(count) { effectiveMin }
        var remaining = targetSum - (count * effectiveMin)

        // Distribute remaining randomly respecting maxDelay
        var iterations = 0
        val maxIterations = count * 2000
        while (remaining > 0 && iterations < maxIterations) {
            iterations++
            val idx = random.nextInt(count)
            val headroom = effectiveMax - delays[idx]
            if (headroom <= 0) continue

            val maxAdd = min(remaining, headroom)
            val add = random.nextInt(1, maxAdd + 1)
            delays[idx] += add
            remaining -= add
        }

        // Deterministic fallback if random distribution had leftover
        if (remaining > 0) {
            for (i in 0 until count) {
                if (remaining <= 0) break
                val headroom = effectiveMax - delays[i]
                if (headroom > 0) {
                    val add = min(remaining, headroom)
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

        // Pairwise jitter: randomly shift small amounts between adjacent pairs
        for (i in 0 until count - 1 step 2) {
            if (random.nextBoolean()) {
                val maxDown = delays[i] - effectiveMin
                val maxUp = effectiveMax - delays[i + 1]
                val maxShift = min(maxDown, maxUp)
                if (maxShift > 0) {
                    val shift = random.nextInt(0, min(maxShift, 3) + 1)
                    delays[i] -= shift
                    delays[i + 1] += shift
                }
            }
        }

        return delays.toList() to targetSum
    }

    /**
     * Generate extra search delays drawn from [minDelay, maxDelay].
     */
    fun generateExtraDelays(
        extraCount: Int,
        minDelay: Int,
        maxDelay: Int,
        random: Random = Random.Default
    ): List<Int> {
        val effectiveMin = max(0, minDelay)
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
        val clampedTimeframeSeconds: Int
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

        val (minD, maxD) = if (customMinDelay != null && customMaxDelay != null) {
            customMinDelay to customMaxDelay
        } else {
            computeAutoBounds(T, N)
        }

        val (requiredDelays, clampedT) = generateDelays(N, T, minD, maxD, random, onAdjustment)
        val extraDelays = generateExtraDelays(E, minD, maxD, random)

        return Plan(
            startIndex = 0,
            requiredCount = N,
            extraCount = E,
            requiredDelays = requiredDelays,
            extraDelays = extraDelays,
            clampedTimeframeSeconds = clampedT
        )
    }
}
