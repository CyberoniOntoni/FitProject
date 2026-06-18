package com.fitproject.droid.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
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

            WeightTrendChart(
                measurements = measurements,
                unitPreferences = unitPreferences
            )

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
                text = if (hasValue) formatDisplayValue(latest.value, type, unitPreferences) else "—",
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
            text = if (hasValue) "Last: ${formatShortDate(latest.date)}" else "Not recorded",
            style = BWSTypography.Caption,
            color = if (hasValue) BWSColors.TextSecondary else BWSColors.TextTertiary
        )
    }
}

private val BodyweightChartColor = Color(0xFFc93477)

@Composable
private fun WeightTrendChart(
    measurements: List<FPMeasurement>,
    unitPreferences: FPUnitPreferences
) {
    val bodyweightType = remember { MeasurementCatalog.findById("DfqsrFQBGi04aHWAPA7I") }
    val weightLogs = remember(measurements) {
        measurements
            .filter {
                it.typeId == "DfqsrFQBGi04aHWAPA7I" ||
                    it.name.contains("bodyweight", ignoreCase = true) ||
                    (it.name.contains("weight", ignoreCase = true) &&
                        !it.name.contains("body fat", ignoreCase = true))
            }
            .sortedBy { it.date }
            .takeLast(24)
    }

    if (weightLogs.size < 2 || bodyweightType == null) return

    var selectedIndex by remember(weightLogs) {
        mutableIntStateOf(weightLogs.lastIndex)
    }

    val displayValues = remember(weightLogs, unitPreferences) {
        weightLogs.map {
            UnitConversionHelper.convertMassForDisplay(it.value, unitPreferences.mass)
        }
    }
    val minDisplay = displayValues.minOrNull() ?: 0.0
    val maxDisplay = displayValues.maxOrNull() ?: 0.0
    val displayRange = maxOf(maxDisplay - minDisplay, 0.5)
    val unitLabel = UnitConversionHelper.displayUnit(bodyweightType, unitPreferences)

    val selected = weightLogs[selectedIndex]
    val selectedDisplay = displayValues[selectedIndex]
    val previousDisplay = displayValues.getOrNull(selectedIndex - 1)
    val delta = previousDisplay?.let { selectedDisplay - it }

    BWSCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    "Bodyweight Trend",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                Text(
                    "Drag to explore",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextTertiary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BWSColors.SurfaceHighlight.copy(alpha = 0.45f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        formatDisplayValue(selected.value, bodyweightType, unitPreferences),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = BodyweightChartColor
                    )
                    Text(unitLabel, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        formatFullDate(selected.date),
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary,
                        textAlign = TextAlign.End
                    )
                    delta?.let { change ->
                        val sign = if (change >= 0) "+" else ""
                        val deltaText = if (change % 1.0 == 0.0) {
                            "$sign${change.roundToInt()} $unitLabel"
                        } else {
                            "$sign${"%.1f".format(change)} $unitLabel"
                        }
                        Text(
                            deltaText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = when {
                                change > 0 -> BWSColors.Warning
                                change < 0 -> BWSColors.Success
                                else -> BWSColors.TextTertiary
                            }
                        )
                    }
                }
            }

            val chartHeight = 168.dp
            val textMeasurer = rememberTextMeasurer()
            val yAxisLabelStyle = TextStyle(fontSize = 10.sp, color = BWSColors.TextSecondary)
            val xAxisLabelStyle = TextStyle(fontSize = 10.sp, color = BWSColors.TextTertiary)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .pointerInput(weightLogs) {
                        fun indexAt(x: Float): Int {
                            if (weightLogs.size <= 1) return 0
                            val leftPad = 36f
                            val rightPad = 12f
                            val chartWidth = size.width - leftPad - rightPad
                            val step = chartWidth / (weightLogs.size - 1)
                            return ((x - leftPad) / step).roundToInt().coerceIn(0, weightLogs.lastIndex)
                        }

                        detectTapGestures { offset ->
                            selectedIndex = indexAt(offset.x)
                        }
                        detectDragGestures(
                            onDragStart = { offset -> selectedIndex = indexAt(offset.x) },
                            onDrag = { change, _ ->
                                change.consume()
                                selectedIndex = indexAt(change.position.x)
                            }
                        )
                    }
            ) {
                val leftPad = 36f
                val rightPad = 12f
                val topPad = 14f
                val bottomPad = 28f
                val chartWidth = size.width - leftPad - rightPad
                val chartHeightInner = size.height - topPad - bottomPad

                fun pointAt(index: Int): Offset {
                    val x = leftPad + chartWidth * index / (weightLogs.size - 1).coerceAtLeast(1)
                    val normalized = (displayValues[index] - minDisplay) / displayRange
                    val y = topPad + chartHeightInner * (1f - normalized.toFloat())
                    return Offset(x, y)
                }

                val gridColor = Color.White.copy(alpha = 0.06f)
                for (i in 0..3) {
                    val y = topPad + chartHeightInner * i / 3f
                    drawLine(
                        color = gridColor,
                        start = Offset(leftPad, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1f
                    )
                }

                val points = weightLogs.indices.map { pointAt(it) }

                val fillPath = Path().apply {
                    moveTo(points.first().x, topPad + chartHeightInner)
                    lineTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                    lineTo(points.last().x, topPad + chartHeightInner)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            BodyweightChartColor.copy(alpha = 0.28f),
                            BodyweightChartColor.copy(alpha = 0.02f)
                        ),
                        startY = topPad,
                        endY = topPad + chartHeightInner
                    )
                )

                val linePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    for (i in 1 until points.size) {
                        val prev = points[i - 1]
                        val curr = points[i]
                        val midX = (prev.x + curr.x) / 2f
                        cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
                    }
                }
                drawPath(
                    path = linePath,
                    color = Color.White.copy(alpha = 0.12f),
                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path = linePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            BodyweightChartColor.copy(alpha = 0.65f),
                            BodyweightChartColor,
                            BWSColors.AccentSecondary.copy(alpha = 0.85f)
                        )
                    ),
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                if (selectedIndex < points.lastIndex) {
                    val futurePath = Path().apply {
                        moveTo(points[selectedIndex].x, points[selectedIndex].y)
                        for (i in (selectedIndex + 1)..points.lastIndex) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = futurePath,
                        color = Color.White.copy(alpha = 0.1f),
                        style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                if (selectedIndex > 0) {
                    val highlightPath = Path().apply {
                        moveTo(points[0].x, points[0].y)
                        for (i in 1..selectedIndex) {
                            lineTo(points[i].x, points[i].y)
                        }
                    }
                    drawPath(
                        path = highlightPath,
                        color = BodyweightChartColor.copy(alpha = 0.18f),
                        style = Stroke(width = 12f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = highlightPath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(BodyweightChartColor.copy(alpha = 0.7f), BodyweightChartColor)
                        ),
                        style = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }

                val selectedPoint = points[selectedIndex]
                drawLine(
                    color = BodyweightChartColor.copy(alpha = 0.35f),
                    start = Offset(selectedPoint.x, topPad),
                    end = Offset(selectedPoint.x, topPad + chartHeightInner),
                    strokeWidth = 1.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )

                points.forEachIndexed { index, point ->
                    val isSelected = index == selectedIndex
                    val radius = if (isSelected) 7f else 4f
                    val alpha = when {
                        isSelected -> 1f
                        index <= selectedIndex -> 0.85f
                        else -> 0.35f
                    }
                    if (isSelected) {
                        drawCircle(
                            color = BodyweightChartColor.copy(alpha = 0.25f),
                            radius = 14f,
                            center = point
                        )
                    }
                    drawCircle(
                        color = BodyweightChartColor.copy(alpha = alpha),
                        radius = radius,
                        center = point
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = if (isSelected) 0.95f else 0.7f),
                        radius = radius * 0.45f,
                        center = point
                    )
                }

                listOf(maxDisplay, minDisplay + displayRange / 2, minDisplay).forEachIndexed { i, value ->
                    val y = topPad + chartHeightInner * i / 2f
                    val label = if (value % 1.0 == 0.0) "%.0f".format(value) else "%.1f".format(value)
                    val layout = textMeasurer.measure(label, yAxisLabelStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(4f, y - layout.size.height / 2f)
                    )
                }

                listOf(0, weightLogs.lastIndex / 2, weightLogs.lastIndex).distinct().forEach { index ->
                    val point = points[index]
                    val label = formatMonthDay(weightLogs[index].date)
                    val layout = textMeasurer.measure(label, xAxisLabelStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = point.x - layout.size.width / 2f,
                            y = size.height - layout.size.height - 2f
                        )
                    )
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
    val display = when (type.category) {
        "Circumference" -> UnitConversionHelper.convertCircumferenceForDisplay(canonical, prefs.circumference)
        "Body Composition" if type.unitType == "MASS" ->
            UnitConversionHelper.convertMassForDisplay(canonical, prefs.mass)
        else -> canonical
    }
    return if (display % 1.0 == 0.0) "%.0f".format(display) else "%.1f".format(display)
}

private fun canonicalValue(input: Double, type: FPMeasurementTypeDef, prefs: FPUnitPreferences): Double =
    when (type.category) {
        "Circumference" -> UnitConversionHelper.circumferenceToCanonical(input, prefs.circumference)
        "Body Composition" if type.unitType == "MASS" ->
            UnitConversionHelper.massToCanonical(input, prefs.mass)

        else -> input
    }

private fun formatShortDate(date: Date): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)

private fun formatFullDate(date: Date): String =
    SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(date)

private fun formatMonthDay(date: Date): String =
    SimpleDateFormat("MMM d", Locale.getDefault()).format(date)