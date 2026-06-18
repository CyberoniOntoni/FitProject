package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.data.FPFormAnswer
import com.fitproject.droid.data.FPFormField
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import kotlinx.coroutines.launch

@Composable
fun FormFillScreen(
    form: FPForm,
    onSubmit: suspend (FPForm, List<FPFormAnswer>) -> Result<Unit>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var answers by remember(form.id) {
        mutableStateOf(
            form.fields.associate { field ->
                field.id to defaultAnswerForField(field)
            }
        )
    }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BWSColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "← Back",
            style = BWSTypography.Caption,
            color = BWSColors.Accent,
            modifier = Modifier.clickable(enabled = !isSubmitting, onClick = onDismiss)
        )
        Text(
            form.title,
            style = BWSTypography.Headline,
            color = BWSColors.TextPrimary
        )
        form.description?.takeIf { it.isNotEmpty() }?.let {
            Text(it, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
        }

        form.fields.sortedBy { it.index }.forEach { field ->
            FormFieldView(
                field = field,
                value = answers[field.id] ?: "",
                onValueChange = { answers = answers + (field.id to it) },
                enabled = !isSubmitting
            )
        }

        BWSPrimaryButton(
            title = if (isSubmitting) "Submitting…" else "Submit Form",
            isLoading = isSubmitting,
            enabled = !isSubmitting,
            onClick = {
                val missing = form.fields
                    .filter { it.required && isInputField(it.type) }
                    .filter { (answers[it.id] ?: "").isBlank() }
                    .map { it.question }

                if (missing.isNotEmpty()) {
                    validationMessage = "Please complete: ${missing.joinToString(", ")}"
                    return@BWSPrimaryButton
                }

                val submissionAnswers = form.fields
                    .filter { isInputField(it.type) }
                    .mapNotNull { field ->
                        val value = answers[field.id]?.trim().orEmpty()
                        if (value.isEmpty()) return@mapNotNull null
                        FPFormAnswer(
                            fieldId = field.id,
                            question = field.question,
                            type = field.type,
                            value = value
                        )
                    }

                scope.launch {
                    isSubmitting = true
                    onSubmit(form, submissionAnswers)
                        .onSuccess { onDismiss() }
                        .onFailure { error ->
                            validationMessage = error.localizedMessage ?: "Failed to submit form"
                            isSubmitting = false
                        }
                }
            }
        )
    }

    validationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { validationMessage = null },
            title = { Text("Form") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { validationMessage = null }) {
                    Text("OK", color = BWSColors.Accent)
                }
            },
            containerColor = BWSColors.SurfaceElevated,
            titleContentColor = BWSColors.TextPrimary,
            textContentColor = BWSColors.TextSecondary
        )
    }
}

@Composable
private fun FormFieldView(
    field: FPFormField,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    when (field.type) {
        "Header" -> Text(
            text = field.question.uppercase(),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = BWSColors.Accent,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(top = 8.dp)
        )
        "Divider" -> HorizontalDivider(
            color = BWSColors.SurfaceHighlight,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        "Paragraph" -> Text(
            text = field.question,
            style = BWSTypography.Caption,
            color = BWSColors.TextSecondary
        )
        "Rating" -> RatingField(field, value, onValueChange, enabled)
        "Linear Scale" -> ScaleField(field, value, onValueChange, enabled)
        "Multiple Choice" -> ChoiceField(field, value, onValueChange, multi = false, enabled)
        "Checkboxes" -> ChoiceField(field, value, onValueChange, multi = true, enabled)
        "Number" -> InputField(field, value, onValueChange, KeyboardType.Decimal, enabled)
        "Date" -> InputField(field, value, onValueChange, KeyboardType.Text, enabled, placeholder = "YYYY-MM-DD")
        "Text", "TextArea" -> InputField(
            field = field,
            value = value,
            onValueChange = onValueChange,
            keyboard = KeyboardType.Text,
            enabled = enabled,
            multiline = true
        )
        else -> InputField(field, value, onValueChange, KeyboardType.Text, enabled)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RatingField(
    field: FPFormField,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeader(field)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rating in 1..field.maxRating) {
                val selected = value == rating.toString()
                Text(
                    text = rating.toString(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else BWSColors.TextPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) BWSColors.Accent else BWSColors.SurfaceHighlight)
                        .clickable(enabled = enabled) { onValueChange(rating.toString()) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun ScaleField(
    field: FPFormField,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean
) {
    val current = value.toFloatOrNull()
        ?: ((field.scaleMin + field.scaleMax) / 2f)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeader(field)
        Slider(
            value = current,
            onValueChange = { onValueChange(it.toInt().toString()) },
            valueRange = field.scaleMin.toFloat()..field.scaleMax.toFloat(),
            steps = (field.scaleMax - field.scaleMin - 1).coerceAtLeast(0),
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = BWSColors.Accent,
                activeTrackColor = BWSColors.Accent,
                inactiveTrackColor = BWSColors.SurfaceHighlight
            )
        )
        Text(
            text = current.toInt().toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = BWSColors.Accent
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                field.scaleMinLabel ?: field.scaleMin.toString(),
                style = BWSTypography.Caption,
                color = BWSColors.TextSecondary
            )
            Text(
                field.scaleMaxLabel ?: field.scaleMax.toString(),
                style = BWSTypography.Caption,
                color = BWSColors.TextSecondary,
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoiceField(
    field: FPFormField,
    value: String,
    onValueChange: (String) -> Unit,
    multi: Boolean,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeader(field)
        if (multi) {
            field.options.forEach { option ->
                val selected = value.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = enabled) {
                            val updated = selected.toMutableSet()
                            if (option in updated) updated.remove(option) else updated.add(option)
                            onValueChange(updated.sorted().joinToString(", "))
                        }
                ) {
                    Checkbox(
                        checked = option in selected,
                        onCheckedChange = {
                            val updated = selected.toMutableSet()
                            if (it) updated.add(option) else updated.remove(option)
                            onValueChange(updated.sorted().joinToString(", "))
                        },
                        enabled = enabled,
                        colors = CheckboxDefaults.colors(
                            checkedColor = BWSColors.Accent,
                            uncheckedColor = BWSColors.TextTertiary
                        )
                    )
                    Text(option, color = BWSColors.TextPrimary, modifier = Modifier.padding(start = 4.dp))
                }
            }
        } else {
            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { if (enabled) expanded = it }
            ) {
                OutlinedTextField(
                    value = value.ifEmpty { "Select an option" },
                    onValueChange = {},
                    readOnly = true,
                    enabled = enabled,
                    modifier = Modifier
                        .menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = enabled
                        )
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    colors = fieldColors()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    containerColor = BWSColors.SurfaceElevated
                ) {
                    field.options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option, color = BWSColors.TextPrimary) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InputField(
    field: FPFormField,
    value: String,
    onValueChange: (String) -> Unit,
    keyboard: KeyboardType,
    enabled: Boolean,
    placeholder: String = "Your answer",
    multiline: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FieldHeader(field)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            placeholder = { Text(placeholder, color = BWSColors.TextTertiary) },
            singleLine = !multiline,
            minLines = if (multiline) 3 else 1,
            maxLines = if (multiline) 6 else 1,
            keyboardOptions = KeyboardOptions(keyboardType = keyboard),
            colors = fieldColors(),
            shape = RoundedCornerShape(8.dp)
        )
    }
}

@Composable
private fun FieldHeader(field: FPFormField) {
    if (field.question.isNotBlank()) {
        Text(
            text = field.question + if (field.required) " *" else "",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.TextPrimary
        )
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = BWSColors.TextPrimary,
    unfocusedTextColor = BWSColors.TextPrimary,
    focusedContainerColor = BWSColors.SurfaceHighlight,
    unfocusedContainerColor = BWSColors.SurfaceHighlight,
    focusedBorderColor = BWSColors.Accent.copy(alpha = 0.5f),
    unfocusedBorderColor = BWSColors.SurfaceHighlight,
    cursorColor = BWSColors.Accent
)

private fun defaultAnswerForField(field: FPFormField): String =
    if (field.type == "Linear Scale") {
        ((field.scaleMin + field.scaleMax) / 2).toString()
    } else {
        ""
    }

private fun isInputField(type: String): Boolean =
    type !in setOf("Header", "Divider", "Paragraph")