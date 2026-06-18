package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPHabit
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.ProgressRing
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography

@Composable
fun HabitsScreen(
    habits: List<FPHabit>,
    onUpdateHabit: (FPHabit, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (habits.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(48.dp))
                Text("No habits tracked", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
                Text(
                    "Habits set up on FitPros.io sync here automatically.",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextSecondary
                )
            }
        } else {
            val completed = habits.count { it.isComplete }
            BWSCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Today's Progress",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BWSColors.TextPrimary
                        )
                        Text(
                            "$completed of ${habits.size} habits complete",
                            style = BWSTypography.Caption,
                            color = BWSColors.TextSecondary
                        )
                    }
                    ProgressRing(
                        progress = if (habits.isEmpty()) 0.0 else completed.toDouble() / habits.size
                    )
                }
            }

            habits.forEach { habit ->
                HabitDetailCard(habit = habit, onUpdate = { onUpdateHabit(habit, it) })
            }
        }
    }
}

@Composable
private fun HabitDetailCard(
    habit: FPHabit,
    onUpdate: (Double) -> Unit
) {
    var inputValue by remember(habit.id, habit.currentValue) {
        mutableStateOf(formatHabitInput(habit.currentValue))
    }

    BWSCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    habit.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                if (habit.isComplete) {
                    Text(
                        "On target",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BWSColors.Success
                    )
                }
            }

            habit.description?.let {
                Text(it, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(BWSColors.SurfaceHighlight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(habit.progress.toFloat())
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(BWSColors.Accent)
                )
            }

            Text(
                text = if (habit.isComplete) "✓ Complete" else habit.progressText,
                style = BWSTypography.Caption,
                color = if (habit.isComplete) BWSColors.Success else BWSColors.TextSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Log value",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextSecondary
                )

                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BWSColors.TextPrimary,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BWSColors.TextPrimary,
                        unfocusedTextColor = BWSColors.TextPrimary,
                        focusedContainerColor = BWSColors.SurfaceHighlight,
                        unfocusedContainerColor = BWSColors.SurfaceHighlight,
                        focusedBorderColor = BWSColors.SurfaceHighlight,
                        unfocusedBorderColor = BWSColors.SurfaceHighlight,
                        cursorColor = BWSColors.Accent
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                Text(
                    text = "Save",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BWSColors.Accent)
                        .clickable { inputValue.toDoubleOrNull()?.let(onUpdate) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickActionButton("-1", Modifier.weight(1f)) { onUpdate(maxOf(0.0, habit.currentValue - 1)) }
                QuickActionButton("+1", Modifier.weight(1f)) { onUpdate(habit.currentValue + 1) }
                QuickActionButton("Hit target", Modifier.weight(1f)) {
                    val target = if (habit.targetType == "RANGE") habit.targetMax else habit.targetMin
                    onUpdate(target)
                }
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = BWSColors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BWSColors.SurfaceHighlight)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 10.dp)
    )
}

private fun formatHabitInput(value: Double): String =
    if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)