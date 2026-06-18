package com.fitproject.droid.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPPersonalRecord
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PersonalRecordsScreen(
    personalRecords: List<FPPersonalRecord>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (personalRecords.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.EmojiEvents, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(48.dp))
                Text("No personal records yet", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
                Text(
                    "PRs are logged automatically when you beat your best during workouts.",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextSecondary
                )
            }
        } else {
            personalRecords.forEach { record ->
                BWSCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("🏆", fontSize = 14.sp)
                                Text("PR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BWSColors.PrGold)
                            }
                            Text(
                                record.exerciseName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BWSColors.TextPrimary
                            )
                            Text(
                                formatDate(record.date),
                                style = BWSTypography.Caption,
                                color = BWSColors.TextSecondary
                            )
                            record.previousValue?.let {
                                Text("Previous: $it", style = BWSTypography.Caption, color = BWSColors.TextTertiary)
                            }
                        }
                        Text(
                            "${record.value} ${record.metric}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = BWSColors.Accent
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.size(24.dp))
    }
}

private fun formatDate(date: java.util.Date): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)