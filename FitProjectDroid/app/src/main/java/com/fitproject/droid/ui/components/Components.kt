package com.fitproject.droid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import androidx.compose.foundation.Canvas

@Composable
fun BWSCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(BWSColors.CardRadius.dp),
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(BWSColors.CardRadius.dp))
            .background(BWSColors.Surface)
            .border(
                width = 0.5.dp,
                color = BWSColors.Separator,
                shape = RoundedCornerShape(BWSColors.CardRadius.dp)
            )
            .padding(padding)
    ) {
        content()
    }
}

@Composable
fun AppleGroupedSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(BWSColors.CardRadius.dp))
            .background(BWSColors.Surface)
            .border(0.5.dp, BWSColors.Separator, RoundedCornerShape(BWSColors.CardRadius.dp))
    ) {
        content()
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = BWSTypography.LargeTitle, color = BWSColors.TextPrimary)
        subtitle?.let {
            Text(it, style = BWSTypography.Body, color = BWSColors.TextSecondary)
        }
    }
}

@Composable
fun ProgressRing(
    progress: Double,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    lineWidth: Dp = 5.dp,
    showLabel: Boolean = true
) {
    val clampedProgress = progress.coerceIn(0.0, 1.0)
    val trackColor = BWSColors.SurfaceHighlight
    val progressColor = BWSColors.Accent

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round)
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - lineWidth.toPx(),
                this.size.height - lineWidth.toPx()
            )
            val topLeft = androidx.compose.ui.geometry.Offset(lineWidth.toPx() / 2, lineWidth.toPx() / 2)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = (clampedProgress * 360).toFloat(),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )
        }

        if (showLabel) {
            Text(
                text = "${(clampedProgress * 100).toInt()}%",
                fontSize = (size.value * 0.22f).sp,
                fontWeight = FontWeight.Bold,
                color = BWSColors.TextPrimary
            )
        }
    }
}

@Composable
fun BWSPrimaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(BWSColors.ButtonRadius.dp))
            .background(
                if (enabled) BWSColors.Accent else BWSColors.Accent.copy(alpha = 0.4f)
            )
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
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    style = BWSTypography.Headline,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BWSSecondaryButton(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(BWSColors.ButtonRadius.dp))
            .background(BWSColors.Fill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(title, style = BWSTypography.Headline, color = BWSColors.Accent)
    }
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = BWSColors.Accent,
            modifier = Modifier.width(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = BWSTypography.Headline,
                color = BWSColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = BWSTypography.Caption,
                color = BWSColors.TextSecondary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = BWSColors.TextTertiary
        )
    }
}

@Composable
fun ProfileMenuDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(
        modifier = modifier.padding(start = 56.dp),
        thickness = 0.5.dp,
        color = BWSColors.Separator
    )
}

@Composable
fun WeekProgressDots(
    completed: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val count = maxOf(total, 1)
        for (day in 1..count) {
            Box(contentAlignment = Alignment.Center) {
                if (day == completed + 1) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .border(2.dp, BWSColors.Accent.copy(alpha = 0.4f), CircleShape)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(
                            if (day <= completed) BWSColors.Accent else BWSColors.SurfaceHighlight
                        )
                )
            }
        }
    }
}

@Composable
fun PRBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(BWSColors.Warning.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "New Personal Record",
            style = BWSTypography.Caption,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.Warning
        )
    }
}