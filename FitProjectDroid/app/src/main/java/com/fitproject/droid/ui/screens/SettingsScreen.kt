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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPUnitPreferences
import com.fitproject.droid.data.UnitSystem
import com.fitproject.droid.ui.components.AppleGroupedSection
import com.fitproject.droid.ui.components.FitnessSegmentedControl
import com.fitproject.droid.ui.components.ScreenHeader
import com.fitproject.droid.ui.theme.AppThemeMode
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography

@Composable
fun SettingsScreen(
    unitPreferences: FPUnitPreferences,
    themeMode: AppThemeMode,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onUpdatePreference: (key: String, value: String) -> Unit,
    onUpdateUnitSystem: (UnitSystem) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            subtitle = "Appearance and units — synced with FitPros.io"
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "APPEARANCE",
                style = BWSTypography.Footnote,
                color = BWSColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            AppleGroupedSection {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Theme",
                        style = BWSTypography.Headline,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        "Dark matches Apple Fitness. Light matches Apple Health.",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary
                    )
                    FitnessSegmentedControl(
                        options = AppThemeMode.entries.map { it.label },
                        selectedIndex = AppThemeMode.entries.indexOf(themeMode),
                        onSelect = { index -> onThemeModeChange(AppThemeMode.entries[index]) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "UNITS",
                style = BWSTypography.Footnote,
                color = BWSColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            AppleGroupedSection {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            "Unit System",
                            style = BWSTypography.Subhead,
                            fontWeight = FontWeight.SemiBold,
                            color = BWSColors.TextPrimary
                        )
                        Text(
                            "Metric uses kg, cm, and km. Imperial uses lb, in, and mi.",
                            style = BWSTypography.Caption,
                            color = BWSColors.TextSecondary
                        )
                        FitnessSegmentedControl(
                            options = UnitSystem.entries.map { it.label },
                            selectedIndex = UnitSystem.entries.indexOf(unitPreferences.unitSystem),
                            onSelect = { index -> onUpdateUnitSystem(UnitSystem.entries[index]) }
                        )
                    }
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
}

@Composable
private fun PreferenceGroup(
    title: String,
    options: List<Pair<String, String>>,
    current: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, style = BWSTypography.Subhead, fontWeight = FontWeight.SemiBold, color = BWSColors.TextPrimary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (label, value) ->
                val selected = value == current
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) BWSColors.OnAccent else BWSColors.TextPrimary,
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