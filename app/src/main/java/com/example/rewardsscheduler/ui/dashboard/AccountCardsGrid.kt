package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.data.AccountProfile
import com.example.rewardsscheduler.theme.*

@Composable
fun AccountCardsGrid(
    profiles: List<AccountProfile>,
    selectedProfileIds: Set<String>,
    onToggleSelect: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onAddProfile: (name: String, maxPoints: Int) -> Unit,
    onSignInClick: (AccountProfile) -> Unit,
    onConfigClick: (AccountProfile) -> Unit,
    onDeleteClick: (AccountProfile) -> Unit,
    onMarkDone: (AccountProfile) -> Unit = {},
    onMarkPending: (AccountProfile) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val doneCount = profiles.count { it.searchesDone }
    val totalCount = profiles.size

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Section Header (.section-header)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Profiles",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                letterSpacing = 0.5.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Done counter badge (.meta-badge)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (doneCount == totalCount && totalCount > 0) SuccessGlow else BgElevated)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$doneCount/$totalCount done today",
                        color = if (doneCount == totalCount && totalCount > 0) Success else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Select All button (.select-all-btn)
                val allSelected = selectedProfileIds.size == profiles.size && profiles.isNotEmpty()
                OutlinedButton(
                    onClick = if (allSelected) onDeselectAll else onSelectAll,
                    modifier = Modifier.height(28.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (allSelected) Accent else TextSecondary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (allSelected) Accent else Border),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text(if (allSelected) "Clear" else "Select All", fontSize = 11.sp)
                }
            }
        }

        // 2-Column Grid with Cards + AddProfileCard at the end
        val allCards = profiles.map<AccountProfile, @Composable (Modifier) -> Unit> { prof ->
            { mod ->
                AccountCard(
                    profile = prof,
                    isSelected = selectedProfileIds.contains(prof.id),
                    onToggleSelect = { onToggleSelect(prof.id) },
                    onSignInClick = { onSignInClick(prof) },
                    onConfigClick = { onConfigClick(prof) },
                    onDeleteClick = { onDeleteClick(prof) },
                    onMarkDone = { onMarkDone(prof) },
                    onMarkPending = { onMarkPending(prof) },
                    modifier = mod
                )
            }
        } + listOf<@Composable (Modifier) -> Unit> { mod ->
            AddProfileCard(
                onAddProfile = { name, maxPts -> onAddProfile(name, maxPts) },
                modifier = mod
            )
        }

        val rows = allCards.chunked(2)
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems[0](Modifier.weight(1f))
                if (rowItems.size > 1) {
                    rowItems[1](Modifier.weight(1f))
                } else {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}
