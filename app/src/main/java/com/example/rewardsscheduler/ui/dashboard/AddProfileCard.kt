package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.theme.*

/**
 * Replicates PC app's .add-profile-card with inline form.
 */
@Composable
fun AddProfileCard(
    onAddProfile: (name: String, maxPoints: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFormOpen by remember { mutableStateOf(false) }
    var profileName by remember { mutableStateOf("") }
    var maxPointsStr by remember { mutableStateOf("60") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(12.dp))
            .border(
                1.5.dp,
                if (isFormOpen) Accent else Color(0x33FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = !isFormOpen) { isFormOpen = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x661E293B))
    ) {
        if (!isFormOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle text
                Text(
                    text = "ADD MORE PROFILES",
                    color = Color(0x0DFFFFFF),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                // Foreground plus and label
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "+",
                        color = Accent,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Light,
                        lineHeight = 42.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Add more profiles",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "New Profile",
                    color = Accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    placeholder = { Text("Profile Name (e.g. Work)", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = maxPointsStr,
                    onValueChange = { maxPointsStr = it },
                    placeholder = { Text("Max Points (e.g. 60)", fontSize = 11.sp, color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Accent,
                        unfocusedBorderColor = Border,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isFormOpen = false
                            profileName = ""
                            maxPointsStr = "60"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("CANCEL", fontSize = 11.sp, color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            val pts = maxPointsStr.toIntOrNull() ?: 60
                            onAddProfile(profileName, pts)
                            isFormOpen = false
                            profileName = ""
                            maxPointsStr = "60"
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Text("DONE", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
