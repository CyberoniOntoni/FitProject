package com.fitproject.droid.ui.theme

import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.components.BWSSecondaryButton
import com.fitproject.droid.ui.components.ScreenHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

object AppleColors {
    val Background: Color @Composable @ReadOnlyComposable get() = BWSColors.Background
    val Card: Color @Composable @ReadOnlyComposable get() = BWSColors.Surface
    val Label: Color @Composable @ReadOnlyComposable get() = BWSColors.TextPrimary
    val SecondaryLabel: Color @Composable @ReadOnlyComposable get() = BWSColors.TextSecondary
    val TertiaryLabel: Color @Composable @ReadOnlyComposable get() = BWSColors.TextTertiary
    val Accent: Color @Composable @ReadOnlyComposable get() = BWSColors.Accent
    val AccentPressed: Color @Composable @ReadOnlyComposable get() = Color(0xFF0056CC)
    val Separator: Color @Composable @ReadOnlyComposable get() = BWSColors.Separator
    val Fill: Color @Composable @ReadOnlyComposable get() = BWSColors.Fill
    val Destructive: Color @Composable @ReadOnlyComposable get() = BWSColors.Error
    val Success: Color @Composable @ReadOnlyComposable get() = BWSColors.Success
}

object AppleTypography {
    val LargeTitle = BWSTypography.LargeTitle
    val Title = BWSTypography.Title
    val Headline = BWSTypography.Headline
    val Body = BWSTypography.Body
    val Callout = BWSTypography.Callout
    val Subhead = BWSTypography.Subhead
    val Footnote = BWSTypography.Caption
    val Caption = BWSTypography.Footnote
}

@Composable
fun AppleProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp)),
        color = AppleColors.Accent,
        trackColor = AppleColors.Fill
    )
}

@Composable
fun AppleStepHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) = ScreenHeader(title = title, subtitle = subtitle, modifier = modifier)

@Composable
fun ApplePrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) = BWSPrimaryButton(
    title = title,
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    isLoading = isLoading
)

@Composable
fun AppleSecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) = BWSSecondaryButton(title = title, onClick = onClick, modifier = modifier)

@Composable
fun AppleSelectionCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (BWSColors.UseCardShadow && selected) {
                    Modifier.shadow(1.dp, RoundedCornerShape(12.dp))
                } else Modifier
            )
            .clip(RoundedCornerShape(12.dp))
            .background(AppleColors.Card)
            .border(
                width = if (selected) 2.dp else 0.5.dp,
                color = if (selected) AppleColors.Accent else AppleColors.Separator,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = AppleTypography.Headline, color = AppleColors.Label)
            subtitle?.let {
                Text(it, style = AppleTypography.Subhead, color = AppleColors.SecondaryLabel)
            }
        }
        if (selected) {
            Icon(Icons.Default.Check, null, tint = AppleColors.Accent, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
fun AppleTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = AppleTypography.Body.copy(color = AppleColors.Label),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppleColors.Card)
            .border(0.5.dp, AppleColors.Separator, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(placeholder, style = AppleTypography.Body, color = AppleColors.TertiaryLabel)
                }
                Box(modifier = Modifier.fillMaxWidth()) {
                    innerTextField()
                }
            }
        }
    )
}

@Composable
fun AppleNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    suffix: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = AppleTypography.Caption, color = AppleColors.SecondaryLabel)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(AppleColors.Card)
                .border(0.5.dp, AppleColors.Separator, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = AppleTypography.Title.copy(color = AppleColors.Label),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        innerTextField()
                    }
                }
            )
            Text(suffix, style = AppleTypography.Subhead, color = AppleColors.SecondaryLabel)
        }
    }
}

@Composable
fun AppleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) AppleColors.Accent else AppleColors.Card)
            .border(
                0.5.dp,
                if (selected) AppleColors.Accent else AppleColors.Separator,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = AppleTypography.Subhead,
            color = if (selected) BWSColors.OnAccent else AppleColors.Label,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppleBodyFatGauge(percent: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppleColors.Card)
            .border(0.5.dp, AppleColors.Separator, RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("%.1f%%".format(percent), style = AppleTypography.LargeTitle, color = AppleColors.Accent)
        Text("Estimated body fat", style = AppleTypography.Subhead, color = AppleColors.SecondaryLabel)
        Text(
            "U.S. Navy formula (public domain)",
            style = AppleTypography.Caption,
            color = AppleColors.TertiaryLabel,
            textAlign = TextAlign.Center
        )
    }
}