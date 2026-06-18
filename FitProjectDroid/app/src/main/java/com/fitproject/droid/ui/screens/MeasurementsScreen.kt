package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPMeasurement
import com.fitproject.droid.data.FPMeasurementTypeDef
import com.fitproject.droid.data.FPUnitPreferences
import com.fitproject.droid.data.MeasurementCatalog
import com.fitproject.droid.data.UnitConversionHelper
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@Composable
fun MeasurementsScreen(
    measurements: List<FPMeasurement>,
    unitPreferences: FPUnitPreferences,
    onSaveMeasurement: (FPMeasurement) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedTypeId by remember { mutableStateOf(MeasurementCatalog.types.first().id) }
    var newValue by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Current Measurements",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        "Synced with FitPros.io — tap a measurement to log a new entry.",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary
                    )
                }
                IconButton(onClick = {
                    selectedTypeId = MeasurementCatalog.types.first().id
                    newValue = ""
                    showAddDialog = true
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = BWSColors.Accent)
                }
            }

            MeasurementCatalog.categories.forEach { category ->
                Text(
                    category.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BWSColors.Accent,
                    letterSpacing = 1.2.sp
                )
                val types = MeasurementCatalog.typesInCategory(category)
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    types.chunked(2).forEach { rowTypes ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowTypes.forEach { type ->
                                OverviewCell(
                                    type = type,
                                    latest = latestMeasurement(measurements, type),
                                    unitPreferences = unitPreferences,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selectedTypeId = type.id
                                        newValue = ""
                                        showAddDialog = true
                                    }
                                )
                            }
                            if (rowTypes.size == 1) {
                                Box(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            WeightTrendChart(measurements = measurements, unitPreferences = unitPreferences)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Recent History",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                val recent = measurements.sortedByDescending { it.date }.take(30)
                if (recent.isEmpty()) {
                    Text("No entries logged yet.", style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                } else {
                    recent.forEach { measurement ->
                        BWSCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        measurement.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BWSColors.TextPrimary
                                    )
                                    Text(
                                        formatFullDate(measurement.date),
                                        style = BWSTypography.Caption,
                                        color = BWSColors.TextSecondary
                                    )
                                }
                                Text(
                                    UnitConversionHelper.formatMeasurementValue(measurement, unitPreferences),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BWSColors.Accent
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddMeasurementDialog(
            selectedTypeId = selectedTypeId,
            onTypeChange = { selectedTypeId = it },
            value = newValue,
            onValueChange = { newValue = it },
            unitPreferences = unitPreferences,
            onDismiss = { showAddDialog = false },
            onSave = {
                val type = MeasurementCatalog.findById(selectedTypeId) ?: return@AddMeasurementDialog
                val input = newValue.toDoubleOrNull() ?: return@AddMeasurementDialog
                val canonical = canonicalValue(input, type, unitPreferences)
                val measurement = FPMeasurement(
                    id = UUID.randomUUID().toString(),
                    typeId = type.id,
                    name = type.name,
                    unit = UnitConversionHelper.displayUnit(type, unitPreferences),
                    value = canonical,
                    date = Date(),
                    sessionId = UUID.randomUUID().toString()
                )
                onSaveMeasurement(measurement)
                showAddDialog = false
                newValue = ""
            }
        )
    }
}

@Composable
private fun OverviewCell(
    type: FPMeasurementTypeDef,
    latest: FPMeasurement?,
    unitPreferences: FPUnitPreferences,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasValue = latest != null
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BWSColors.Surface)
            .border(1.dp, BWSColors.SurfaceHighlight, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(type.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextPrimary)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (hasValue) formatDisplayValue(latest!!.value, type, unitPreferences) else "—",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasValue) BWSColors.Accent else BWSColors.TextTertiary
            )
            Text(
                text = UnitConversionHelper.displayUnit(type, unitPreferences),
                style = BWSTypography.Caption,
                color = if (hasValue) BWSColors.TextSecondary else BWSColors.TextTertiary,
                modifier = Modifier.padding(start = 3.dp, bottom = 4.dp)
            )
        }
        Text(
            text = if (hasValue) "Last: ${formatShortDate(latest!!.date)}" else "Not recorded",
            style = BWSTypography.Caption,
            color = if (hasValue) BWSColors.TextSecondary else BWSColors.TextTertiary
        )
    }
}

@Composable
private fun WeightTrendChart(
    measurements: List<FPMeasurement>,
    unitPreferences: FPUnitPreferences
) {
    val weightLogs = measurements
        .filter {
            it.typeId == "DfqsrFQBGi04aHWAPA7I" ||
                it.name.contains("bodyweight", ignoreCase = true) ||
                it.name.contains("weight", ignoreCase = true)
        }
        .sortedBy { it.date }
        .takeLast(10)

    if (weightLogs.size >= 2) {
        BWSCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Bodyweight Trend",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                val maxVal = weightLogs.maxOf { it.value }
                val minVal = weightLogs.minOf { it.value }
                val range = maxOf(maxVal - minVal, 1.0)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    weightLogs.forEach { m ->
                        val barHeight = ((m.value - minVal) / range) * 80 + 20).dp
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(barHeight)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(BWSColors.Accent)
                            )
                            Text(
                                formatMonthDay(m.date),
                                fontSize = 9.sp,
                                color = BWSColors.TextTertiary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMeasurementDialog(
    selectedTypeId: String,
    onTypeChange: (String) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    unitPreferences: FPUnitPreferences,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val selectedType = MeasurementCatalog.findById(selectedTypeId)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Measurement", color = BWSColors.TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                MeasurementCatalog.categories.forEach { category ->
                    Text(
                        category,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        MeasurementCatalog.typesInCategory(category).take(4).forEach { type ->
                            Text(
                                type.name,
                                fontSize = 12.sp,
                                color = if (type.id == selectedTypeId) BWSColors.Accent else BWSColors.TextPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (type.id == selectedTypeId) BWSColors.Accent.copy(alpha = 0.15f)
                                        else BWSColors.SurfaceHighlight
                                    )
                                    .clickable { onTypeChange(type.id) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("Value") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = BWSColors.TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = BWSColors.TextPrimary,
                        unfocusedTextColor = BWSColors.TextPrimary,
                        focusedContainerColor = BWSColors.Surface,
                        unfocusedContainerColor = BWSColors.Surface,
                        focusedBorderColor = BWSColors.Accent.copy(alpha = 0.5f),
                        unfocusedBorderColor = BWSColors.SurfaceHighlight,
                        cursorColor = BWSColors.Accent
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                selectedType?.let {
                    Text(
                        "Unit: ${UnitConversionHelper.displayUnit(it, unitPreferences)}",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text("Save", color = BWSColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = BWSColors.TextSecondary)
            }
        },
        containerColor = BWSColors.SurfaceElevated,
        titleContentColor = BWSColors.TextPrimary,
        textContentColor = BWSColors.TextSecondary
    )
}

private fun latestMeasurement(measurements: List<FPMeasurement>, type: FPMeasurementTypeDef): FPMeasurement? =
    measurements
        .filter { it.typeId == type.id || it.name.equals(type.name, ignoreCase = true) }
        .maxByOrNull { it.date }

private fun formatDisplayValue(canonical: Double, type: FPMeasurementTypeDef, prefs: FPUnitPreferences): String {
    val display = when {
        type.category == "Circumference" -> UnitConversionHelper.convertCircumferenceForDisplay(canonical, prefs.circumference)
        type.category == "Body Composition" && type.unitType == "MASS" ->
            UnitConversionHelper.convertMassForDisplay(canonical, prefs.mass)
        else -> canonical
    }
    return if (display % 1.0 == 0.0) "%.0f".format(display) else "%.1f".format(display)
}

private fun canonicalValue(input: Double, type: FPMeasurementTypeDef, prefs: FPUnitPreferences): Double = when {
    type.category == "Circumference" -> UnitConversionHelper.circumferenceToCanonical(input, prefs.circumference)
    type.category == "Body Composition" && type.unitType == "MASS" ->
        UnitConversionHelper.massToCanonical(input, prefs.mass)
    else -> input
}

private fun formatShortDate(date: Date): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)

private fun formatFullDate(date: Date): String =
    SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(date)

private fun formatMonthDay(date: Date): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(date)