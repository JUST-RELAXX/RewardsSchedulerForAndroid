package com.example.rewardsscheduler.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rewardsscheduler.theme.*

/**
 * Replicates the PC app's .action-bar:
 * - Run All Accounts (primary gradient button)
 * - Run Selected (accent outlined button)
 * - Refresh (ghost button)
 * - NO emojis! Uses clean vector icons.
 */
@Composable
fun QuickActionBar(
    onRunAllClick: () -> Unit,
    onRunSelectedClick: () -> Unit,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Run All Accounts (Primary button)
        Button(
            onClick = onRunAllClick,
            modifier = Modifier
                .weight(1.3f)
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = AppIcons.Play,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Run All Accounts",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Run Selected (Accent)
        Button(
            onClick = onRunSelectedClick,
            modifier = Modifier
                .weight(1.1f)
                .height(42.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BgElevated,
                contentColor = Accent
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(BorderAccent)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = AppIcons.Check,
                    contentDescription = null,
                    tint = Accent,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Run Selected",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }

        // Refresh (Ghost)
        OutlinedButton(
            onClick = onRefreshClick,
            modifier = Modifier.height(42.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TextSecondary
            ),
            border = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Border)
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.Refresh,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Refresh", fontSize = 12.sp, color = TextSecondary)
            }
        }
    }
}
