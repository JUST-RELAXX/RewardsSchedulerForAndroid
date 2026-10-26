package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.engine.DelayGenerator
import com.example.rewardsscheduler.theme.*

/**
 * Homepage Account Settings Dialog:
 * Allows configuring:
 * 1) Profile's Name / Label
 * 2) Max Points (default 60, formula N = (maxPoints + 1) / 3)
 * 3) Profile Timeframe override in minutes (empty = use global)
 */
@Composable
fun ProfileConfigDialog(
    profile: AccountProfile,
    onDismiss: () -> Unit,
    onSave: (AccountProfile) -> Unit
) {
    var label by remember { mutableStateOf(profile.label) }
    var maxPointsStr by remember { mutableStateOf(profile.maxPoints.toString()) }
    var timeframeMinutesStr by remember { mutableStateOf(profile.timeframeMinutes?.toString() ?: "") }

    val scrollState = rememberScrollState()

    val currentMaxPoints = maxPointsStr.toIntOrNull() ?: profile.maxPoints
    val requiredSearches = DelayGenerator.calculateRequiredSearches(currentMaxPoints)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = BgPrimary,
            border = androidx.compose.foundation.BorderStroke(1.dp, Border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = AppIcons.Settings,
                            contentDescription = null,
                            tint = Accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Account Settings: ${profile.label}",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = AppIcons.Close,
                            contentDescription = "Close",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Color.White.copy(alpha = 0.08f))
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Profile Name
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Profile Name",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // 2. Max Points
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Max Points", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("$requiredSearches required searches", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = maxPointsStr,
                            onValueChange = { maxPointsStr = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("60", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    // 3. Profile Timeframe Override in minutes (empty = use global)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Timeframe Override (Minutes)", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            val overrideVal = timeframeMinutesStr.toIntOrNull()
                            Text(
                                text = if (overrideVal != null && overrideVal > 0) "${overrideVal}m override" else "Using Global",
                                color = if (overrideVal != null && overrideVal > 0) Accent else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedTextField(
                            value = timeframeMinutesStr,
                            onValueChange = { timeframeMinutesStr = it.filter { ch -> ch.isDigit() } },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = { Text("Empty = Use Global timeframe (30m)", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Accent,
                                unfocusedBorderColor = Border,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Border)
                    ) {
                        Text("Cancel", color = TextSecondary, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val parsedPts = maxPointsStr.toIntOrNull()?.coerceAtLeast(1) ?: profile.maxPoints
                            val parsedTimeframe = timeframeMinutesStr.toIntOrNull()?.takeIf { it > 0 }
                            val updated = profile.copy(
                                label = label.ifBlank { profile.label },
                                maxPoints = parsedPts,
                                timeframeMinutes = parsedTimeframe
                            )
                            onSave(updated)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(42.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Accent,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save Config", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
