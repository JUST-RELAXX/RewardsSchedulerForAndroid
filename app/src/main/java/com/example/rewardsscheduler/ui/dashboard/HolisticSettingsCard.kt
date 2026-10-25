package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*

/**
 * Replicates the PC Electron app's Holistic Settings module (.holistic-module) 1:1:
 * - Module Header: Cyan Sliders icon + "Holistic Settings" text + "Reset All" pill
 * - Global Completion Time (Mins): Input field with "Default: 30"
 * - Fiery ACTIVATE FLASH button (horizontal red/orange gradient, 2px red border)
 * - Dashed separator
 * - PROFILE LEVEL CHANGES header with "Reset" pill
 * - Profile dropdown selector (ONLY lists profiles currently in the active session, plus 'None' option to keep app clean)
 * - Complete Profile Level Changes settings matching PC Electron app:
 *   - Flash Mode Delay (3-15s) with 5s display
 *   - Profile ACTIVATE FLASH button + FINISH IN: --
 *   - Random delay toggle (Off/On) with dual RangeSlider on the seconds line (start & end adjustable)
 *   - Automize query number toggle (Off/On) with RangeSlider
 *   - Auto-stop Duration (minutes) input
 *   - Maximum Searches to Complete input
 *   - Typing Behavior (Typing speed 50-300ms, random variation checkbox, human-like simulation checkbox)
 * - Zero emojis! Uses clean vector icons.
 */
@Composable
fun HolisticSettingsCard(
    globalTimeframeMinutes: Int,
    onTimeframeChange: (Int) -> Unit,
    maxConcurrentProfiles: Int,
    onConcurrentChange: (Int) -> Unit,
    isFlashMode: Boolean,
    onFlashModeToggle: () -> Unit,
    isDesktopSite: Boolean = false,
    onDesktopSiteToggle: (Boolean) -> Unit = {},
    runningFlashProfileIds: Set<String> = emptySet(),
    onStartProfileFlash: (profileId: String, delaySec: Int) -> Unit = { _, _ -> },
    onStopProfileFlash: (profileId: String) -> Unit = {},
    onResetAll: () -> Unit = {},
    onResetProfile: (profileId: String) -> Unit = {},
    profiles: List<AccountProfile> = emptyList(),
    modifier: Modifier = Modifier
) {
    var selectedProfileId by rememberSaveable {
        mutableStateOf("none")
    }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Profile level settings states matching PC exactly
    var profFlashDelay by remember { mutableFloatStateOf(5f) }

    var profRandomDelayOn by remember { mutableStateOf(false) }
    var profRandomDelayMin by remember { mutableFloatStateOf(1f) }
    var profRandomDelayMax by remember { mutableFloatStateOf(10f) }

    var profAutomizeQueryOn by remember { mutableStateOf(false) }
    var profAutomizeMin by remember { mutableFloatStateOf(1f) }
    var profAutomizeMax by remember { mutableFloatStateOf(5f) }

    var autoStopDuration by remember { mutableStateOf("0") }
    var maxSearches by remember { mutableStateOf("0") }

    var typingSpeed by remember { mutableFloatStateOf(100f) }
    var addRandomTypingVariation by remember { mutableStateOf(false) }
    var enableHumanLikeTyping by remember { mutableStateOf(true) }

    val selectedProfile = if (selectedProfileId == "none" || selectedProfileId.isBlank()) null else profiles.find { it.id == selectedProfileId }

    val fieryGradient = Brush.horizontalGradient(
        colors = listOf(
            Color(0xFFFF0000),
            Color(0xFFFF4D4D),
            Color(0xFFFF8000),
            Color(0xFFFF0000)
        )
    )

    var isExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ═══════════════════════════════════════════════════
            // 1. Module Header (.module-header) + Reset All pill + Collapse
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Sliders,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Holistic Settings",
                        color = Accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.3.sp
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ResetPillButton(
                        label = "Reset All",
                        onClick = {
                            onTimeframeChange(30)
                            selectedProfileId = "none"
                            profFlashDelay = 5f
                            profRandomDelayOn = false
                            profRandomDelayMin = 1f
                            profRandomDelayMax = 10f
                            profAutomizeQueryOn = false
                            profAutomizeMin = 1f
                            profAutomizeMax = 5f
                            autoStopDuration = "0"
                            maxSearches = "0"
                            typingSpeed = 100f
                            onResetAll()
                        }
                    )

                    // Collapse / Expand toggle
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(if (isExpanded) "Collapse" else "Expand", color = Accent, fontSize = 11.sp)
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            // ═══════════════════════════════════════════════════
            // 2. Global Completion Time (Mins)
            // ═══════════════════════════════════════════════════
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Global Completion Time (Mins):",
                    color = Color(0xFFB0BEC5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                var textValue by remember(globalTimeframeMinutes) {
                    mutableStateOf(if (globalTimeframeMinutes == 30) "" else globalTimeframeMinutes.toString())
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.25f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (textValue.isEmpty()) {
                        Text(
                            text = "Default: 30",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    BasicTextField(
                        value = textValue,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }
                            textValue = clean
                            val num = clean.toIntOrNull()
                            if (num != null && num > 0) {
                                onTimeframeChange(num)
                            } else if (clean.isEmpty()) {
                                onTimeframeChange(30)
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ═══════════════════════════════════════════════════
            // 3. Fiery ACTIVATE FLASH Button (.flash-btn)
            // ═══════════════════════════════════════════════════
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .then(
                        if (isFlashMode) {
                            Modifier
                                .background(Color(0xFF333333))
                                .border(2.dp, Color(0xFF666666), RoundedCornerShape(8.dp))
                        } else {
                            Modifier
                                .background(fieryGradient)
                                .border(2.dp, Color(0xFFFF0000), RoundedCornerShape(8.dp))
                        }
                    )
                    .clickable { onFlashModeToggle() },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isFlashMode) "STOP FLASH" else "ACTIVATE FLASH",
                        color = if (isFlashMode) Color(0xFFAAAAAA) else Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        imageVector = AppIcons.Flash,
                        contentDescription = null,
                        tint = if (isFlashMode) Color(0xFFAAAAAA) else Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Dashed Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // ═══════════════════════════════════════════════════
            // 4. Desktop Site Toggle (.desktop-site-setting)
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDesktopSite) AccentGlow else Color.Black.copy(alpha = 0.2f))
                    .border(
                        1.dp,
                        if (isDesktopSite) Accent.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f),
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onDesktopSiteToggle(!isDesktopSite) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Desktop Site",
                            color = if (isDesktopSite) Accent else TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isDesktopSite) SuccessGlow else BgElevated)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isDesktopSite) "PC MODE ON" else "MOBILE",
                                color = if (isDesktopSite) Success else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Text(
                        text = "Converts all WebViews to PC desktop layout for bonus tasks (+5 pts) and rewards status",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                Switch(
                    checked = isDesktopSite,
                    onCheckedChange = { onDesktopSiteToggle(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Accent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = BgElevated
                    )
                )
            }

            // Dashed Divider
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f))
            )

            // ═══════════════════════════════════════════════════
            // 5. PROFILE LEVEL CHANGES Section (Exact 1:1 Image 4)
            // ═══════════════════════════════════════════════════
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROFILE LEVEL CHANGES",
                    color = Accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )

                ResetPillButton(
                    label = "Reset",
                    onClick = {
                        val currentTarget = selectedProfileId
                        selectedProfileId = "none"
                        profFlashDelay = 5f
                        profRandomDelayOn = false
                        profRandomDelayMin = 1f
                        profRandomDelayMax = 10f
                        profAutomizeQueryOn = false
                        profAutomizeMin = 1f
                        profAutomizeMax = 5f
                        autoStopDuration = "0"
                        maxSearches = "0"
                        typingSpeed = 100f
                        onResetProfile(currentTarget)
                    }
                )
            }

            // Profile Dropdown Selector (Lists 'None' plus active session profiles)
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgElevated)
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                        .clickable { dropdownExpanded = true }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = selectedProfile?.label ?: "None",
                            color = if (selectedProfile != null) Color.White else Color(0xFFB0BEC5),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "▼",
                            color = TextSecondary,
                            fontSize = 9.sp
                        )
                    }
                }

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.background(BgElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("None", color = TextSecondary, fontSize = 12.sp) },
                        onClick = {
                            selectedProfileId = "none"
                            dropdownExpanded = false
                        }
                    )
                    profiles.forEach { prof ->
                        DropdownMenuItem(
                            text = { Text(prof.label, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                selectedProfileId = prof.id
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // Profile level options only visible when a profile is selected (hidden when 'None')
            AnimatedVisibility(visible = selectedProfileId != "none" && selectedProfile != null) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 4a. Profile Flash Mode Delay (seconds) Box
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFF0000).copy(alpha = 0.05f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFFFF8000).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Flash,
                                    contentDescription = null,
                                    tint = Color(0xFFFF5252),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Flash Mode Delay (seconds):",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${profFlashDelay.toInt()}s",
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Slider(
                            value = profFlashDelay,
                            onValueChange = { profFlashDelay = it },
                            valueRange = 3f..15f,
                            steps = 11,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFFFF0000),
                                activeTrackColor = Color(0xFFFF0000),
                                inactiveTrackColor = Color(0xFFFF0000).copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    val isProfFlashRunning = selectedProfile != null && runningFlashProfileIds.contains(selectedProfile.id)

                    // 4b. Profile Flash Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(
                                if (isProfFlashRunning) {
                                    Modifier
                                        .background(Color(0xFF333333))
                                        .border(2.dp, Color(0xFF666666), RoundedCornerShape(6.dp))
                                } else {
                                    Modifier
                                        .background(fieryGradient)
                                        .border(2.dp, Color(0xFFFF0000), RoundedCornerShape(6.dp))
                                }
                            )
                            .clickable {
                                selectedProfile?.let { prof ->
                                    if (isProfFlashRunning) {
                                        onStopProfileFlash(prof.id)
                                    } else {
                                        onStartProfileFlash(prof.id, profFlashDelay.toInt())
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = if (isProfFlashRunning) "STOP FLASH" else "ACTIVATE FLASH",
                                color = if (isProfFlashRunning) Color(0xFFAAAAAA) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = AppIcons.Flash,
                                contentDescription = null,
                                tint = if (isProfFlashRunning) Color(0xFFAAAAAA) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Finish in text
                    val finishInText = if (isProfFlashRunning && selectedProfile != null) {
                        val remainingSearches = ((selectedProfile.maxPoints / 3) - selectedProfile.searchCount).coerceAtLeast(0)
                        val totalSecs = remainingSearches * profFlashDelay.toInt()
                        if (totalSecs > 0) {
                            val m = totalSecs / 60
                            val s = totalSecs % 60
                            "${m}m ${s}s"
                        } else {
                            "--"
                        }
                    } else {
                        "--"
                    }

                    Text(
                        text = "FINISH IN: $finishInText",
                        color = Color(0xFFFF5252),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )

                    // Dashed Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // 4c. Random Delay Toggle (Off / On) with Dual RangeSlider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Random delay:",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { profRandomDelayOn = false }
                            ) {
                                RadioButton(
                                    selected = !profRandomDelayOn,
                                    onClick = { profRandomDelayOn = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = Accent)
                                )
                                Text("Off", color = TextPrimary, fontSize = 11.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { profRandomDelayOn = true }
                            ) {
                                RadioButton(
                                    selected = profRandomDelayOn,
                                    onClick = { profRandomDelayOn = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = Accent)
                                )
                                Text("On", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }

                    AnimatedVisibility(visible = profRandomDelayOn) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Delay range (seconds):", color = TextSecondary, fontSize = 11.sp)
                                Text("${profRandomDelayMin.toInt()}s - ${profRandomDelayMax.toInt()}s", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            RangeSlider(
                                value = profRandomDelayMin..profRandomDelayMax,
                                onValueChange = { range ->
                                    profRandomDelayMin = range.start
                                    profRandomDelayMax = range.endInclusive
                                },
                                valueRange = 0f..500f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Accent,
                                    activeTrackColor = Accent,
                                    inactiveTrackColor = Accent.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Dashed Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // 4d. Automize query number (Off / On) with RangeSlider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Automize query number:",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { profAutomizeQueryOn = false }
                            ) {
                                RadioButton(
                                    selected = !profAutomizeQueryOn,
                                    onClick = { profAutomizeQueryOn = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = Accent)
                                )
                                Text("Off", color = TextPrimary, fontSize = 11.sp)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clickable { profAutomizeQueryOn = true }
                            ) {
                                RadioButton(
                                    selected = profAutomizeQueryOn,
                                    onClick = { profAutomizeQueryOn = true },
                                    colors = RadioButtonDefaults.colors(selectedColor = Accent)
                                )
                                Text("On", color = TextPrimary, fontSize = 11.sp)
                            }
                        }
                    }

                    AnimatedVisibility(visible = profAutomizeQueryOn) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Query number range:", color = TextSecondary, fontSize = 11.sp)
                                Text("${profAutomizeMin.toInt()} - ${profAutomizeMax.toInt()}", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            RangeSlider(
                                value = profAutomizeMin..profAutomizeMax,
                                onValueChange = { range ->
                                    profAutomizeMin = range.start
                                    profAutomizeMax = range.endInclusive
                                },
                                valueRange = 1f..60f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Accent,
                                    activeTrackColor = Accent,
                                    inactiveTrackColor = Accent.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }

                    // Dashed Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // 4e. Auto-stop Duration (minutes) & Maximum Searches
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Auto-stop Duration (minutes):",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = autoStopDuration,
                                onValueChange = { autoStopDuration = it.filter { ch -> ch.isDigit() } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                cursorBrush = SolidColor(Accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Text(
                            text = "Maximum Searches to Complete:",
                            color = Color(0xFFB0BEC5),
                            fontSize = 11.sp
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.Black.copy(alpha = 0.25f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            BasicTextField(
                                value = maxSearches,
                                onValueChange = { maxSearches = it.filter { ch -> ch.isDigit() } },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                textStyle = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                cursorBrush = SolidColor(Accent),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Text(
                            text = "Set a limit for searches in one session (0 for all).",
                            color = Color(0xFF8899AA),
                            fontSize = 10.sp
                        )
                    }

                    // Dashed Divider
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )

                    // 4f. Typing Behavior
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Typing Behavior",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Typing Speed (ms per character):", color = Color(0xFFB0BEC5), fontSize = 11.sp)
                            Text("${typingSpeed.toInt()}ms", color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Slider(
                            value = typingSpeed,
                            onValueChange = { typingSpeed = it },
                            valueRange = 50f..300f,
                            steps = 24,
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = Accent.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { addRandomTypingVariation = !addRandomTypingVariation }
                        ) {
                            Checkbox(
                                checked = addRandomTypingVariation,
                                onCheckedChange = { addRandomTypingVariation = it },
                                colors = CheckboxDefaults.colors(checkedColor = Accent)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Add random variation to typing speed",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { enableHumanLikeTyping = !enableHumanLikeTyping }
                        ) {
                            Checkbox(
                                checked = enableHumanLikeTyping,
                                onCheckedChange = { enableHumanLikeTyping = it },
                                colors = CheckboxDefaults.colors(checkedColor = Accent)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = "Enable human-like typing simulation",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
                }
            }
        }
    }
}

@Composable
private fun ResetPillButton(
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Transparent)
            .border(1.dp, Accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = AppIcons.RotateCcw,
                contentDescription = null,
                tint = Accent.copy(alpha = 0.85f),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                color = Accent.copy(alpha = 0.85f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
