package com.example.rewardsscheduler.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * Rewards Scheduler theme — dark only, replicating the PC app's premium dark design.
 * No light theme, no dynamic color — always the exact PC app palette.
 */
private val RewardsColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = TextPrimary,
    primaryContainer = AccentDark,
    secondary = Warning,
    onSecondary = BgPrimary,
    tertiary = Success,
    background = BgPrimary,
    onBackground = TextPrimary,
    surface = BgCard,
    onSurface = TextPrimary,
    surfaceVariant = BgElevated,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = TextPrimary,
    outline = BorderAccent,
    outlineVariant = Border,
)

@Composable
fun RewardsSchedulerTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = RewardsColorScheme,
        typography = Typography,
        content = content
    )
}
