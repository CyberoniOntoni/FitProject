package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPContent
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography

@Composable
fun LearnScreen(
    forms: List<FPForm>,
    content: List<FPContent>,
    userId: String?,
    onFormTap: (FPForm) -> Unit,
    onContentTap: (FPContent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Learn",
            style = BWSTypography.Title,
            color = BWSColors.TextPrimary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        if (forms.isEmpty() && content.isEmpty()) {
            LearnEmptyState()
        } else {
            if (forms.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Check-In Forms",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    forms.forEach { form ->
                        val completed = userId != null && form.isCompleted(userId)
                        FormCard(
                            form = form,
                            completed = completed,
                            onClick = { if (!completed) onFormTap(form) }
                        )
                    }
                }
            }

            if (content.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Guides & Articles",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextPrimary,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    content.forEach { item ->
                        ContentCard(item = item, onClick = { onContentTap(item) })
                    }
                }
            }
        }
    }
}

@Composable
fun FormsListScreen(
    forms: List<FPForm>,
    userId: String?,
    onFormTap: (FPForm) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (forms.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(Icons.Default.Description, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(48.dp))
                Text("No forms assigned", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
                Text(
                    "Check-in forms from your coach will appear here.",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextSecondary
                )
            }
        } else {
            forms.forEach { form ->
                val completed = userId != null && form.isCompleted(userId)
                FormCard(
                    form = form,
                    completed = completed,
                    onClick = { if (!completed) onFormTap(form) }
                )
            }
        }
    }
}

@Composable
private fun LearnEmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 40.dp, end = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.MenuBook, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(48.dp))
        Text("No content yet", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
        Text(
            "Guides, articles, and check-in forms from your coach will appear here.",
            style = BWSTypography.Caption,
            color = BWSColors.TextSecondary
        )
    }
}

@Composable
private fun FormCard(
    form: FPForm,
    completed: Boolean,
    onClick: () -> Unit
) {
    BWSCard(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .then(if (!completed) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    form.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary
                )
                form.description?.let {
                    Text(it, style = BWSTypography.Caption, color = BWSColors.TextSecondary, maxLines = 2)
                }
            }
            if (completed) {
                Icon(Icons.Default.CheckCircle, null, tint = BWSColors.Success)
            } else {
                Text(
                    "Pending",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.Warning,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(BWSColors.Warning.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = BWSColors.TextTertiary)
            }
        }
    }
}

@Composable
private fun ContentCard(
    item: FPContent,
    onClick: () -> Unit
) {
    BWSCard(
        padding = 0.dp,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            if (!item.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(topStart = BWSColors.CardRadius.dp, bottomStart = BWSColors.CardRadius.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    item.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary,
                    maxLines = 2
                )
                Text(
                    item.type.replaceFirstChar { it.uppercase() },
                    style = BWSTypography.Caption,
                    color = BWSColors.Accent
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BWSColors.TextTertiary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 12.dp)
            )
        }
    }
}

@Composable
fun ContentDetailScreen(
    content: FPContent,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (!content.imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = content.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(BWSColors.CardRadius.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Text(content.title, style = BWSTypography.Headline, color = BWSColors.TextPrimary)
        content.body?.let {
            Text(it, style = BWSTypography.Body, color = BWSColors.TextSecondary, lineHeight = 24.sp)
        }
    }
}