package com.fitproject.droid.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object AppleColors {
    val Background = Color(0xFFF2F2F7)
    val Card = Color.White
    val Label = Color(0xFF000000)
    val SecondaryLabel = Color(0xFF3C3C43).copy(alpha = 0.6f)
    val TertiaryLabel = Color(0xFF3C3C43).copy(alpha = 0.3f)
    val Accent = Color(0xFF007AFF)
    val AccentPressed = Color(0xFF0056CC)
    val Separator = Color(0xFF3C3C43).copy(alpha = 0.12f)
    val Fill = Color(0xFF787880).copy(alpha = 0.12f)
    val Destructive = Color(0xFFFF3B30)
    val Success = Color(0xFF34C759)
}

object AppleTypography {
    val LargeTitle = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, lineHeight = 41.sp)
    val Title = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp)
    val Headline = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
    val Body = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Normal, lineHeight = 22.sp)
    val Callout = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal, lineHeight = 21.sp)
    val Subhead = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp)
    val Footnote = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp)
    val Caption = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp)
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
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = AppleTypography.LargeTitle, color = AppleColors.Label)
        subtitle?.let {
            Text(it, style = AppleTypography.Body, color = AppleColors.SecondaryLabel)
        }
    }
}

@Composable
fun ApplePrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .shadow(0.dp, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) AppleColors.Accent else AppleColors.Accent.copy(alpha = 0.4f))
            .clickable(enabled = enabled && !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(title, style = AppleTypography.Headline, color = Color.White)
        }
    }
}

@Composable
fun AppleSecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AppleColors.Fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, style = AppleTypography.Headline, color = AppleColors.Accent)
    }
}

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
            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(AppleColors.Card)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) AppleColors.Accent else AppleColors.Separator,
                shape = RoundedCornerShape(16.dp)
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
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = AppleColors.Accent,
                modifier = Modifier.size(22.dp)
            )
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
            .border(1.dp, AppleColors.Separator, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) {
                    Text(placeholder, style = AppleTypography.Body, color = AppleColors.TertiaryLabel)
                }
                inner()
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
                .border(1.dp, AppleColors.Separator, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = AppleTypography.Title.copy(color = AppleColors.Label),
                modifier = Modifier.weight(1f)
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
            .border(1.dp, if (selected) AppleColors.Accent else AppleColors.Separator, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = AppleTypography.Subhead,
            color = if (selected) Color.White else AppleColors.Label,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AppleBodyFatGauge(percent: Double, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(AppleColors.Card)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "%.1f%%".format(percent),
            style = AppleTypography.LargeTitle,
            color = AppleColors.Accent
        )
        Text("Estimated body fat", style = AppleTypography.Subhead, color = AppleColors.SecondaryLabel)
        Text(
            "U.S. Navy formula (public domain)",
            style = AppleTypography.Caption,
            color = AppleColors.TertiaryLabel,
            textAlign = TextAlign.Center
        )
    }
}