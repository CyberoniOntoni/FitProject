package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPUnitPreferences
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography

@Composable
fun SettingsScreen(
    unitPreferences: FPUnitPreferences,
    onUpdatePreference: (key: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "Unit Preferences",
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.TextPrimary
        )
        Text(
            "Synced with FitPros.io — changes apply across web and mobile.",
            style = BWSTypography.Caption,
            color = BWSColors.TextSecondary
        )

        BWSCard {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PreferenceGroup(
                    title = "Body Weight / Mass",
                    options = listOf("kg" to "KILOGRAM", "lb" to "POUND"),
                    current = unitPreferences.mass,
                    onSelect = { onUpdatePreference("mass", it) }
                )
                PreferenceGroup(
                    title = "Circumference",
                    options = listOf("cm" to "CENTIMETER", "in" to "INCH"),
                    current = unitPreferences.circumference,
                    onSelect = { onUpdatePreference("circumference", it) }
                )
                PreferenceGroup(
                    title = "Distance",
                    options = listOf("km" to "KILOMETER", "mi" to "MILE"),
                    current = unitPreferences.distance,
                    onSelect = { onUpdatePreference("distance", it) }
                )
                PreferenceGroup(
                    title = "Time",
                    options = listOf("sec" to "SECOND", "min" to "MINUTE"),
                    current = unitPreferences.time,
                    onSelect = { onUpdatePreference("time", it) }
                )
            }
        }
    }
}

@Composable
private fun PreferenceGroup(
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.TextPrimary
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, value) ->
                val selected = value == current
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else BWSColors.TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) BWSColors.Accent else BWSColors.SurfaceHighlight)
                        .clickable(enabled = !selected) { onSelect(value) }
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}