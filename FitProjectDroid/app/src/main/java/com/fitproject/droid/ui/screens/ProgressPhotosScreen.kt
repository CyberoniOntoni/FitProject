package com.fitproject.droid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPProgressPicture
import com.fitproject.droid.data.FPProgressSession
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ProgressPhotosScreen(
    progressSessions: List<FPProgressSession>,
    allProgressPictures: List<FPProgressPicture>,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf("sessions") }
    var compareBeforeId by remember { mutableStateOf<String?>(null) }
    var compareAfterId by remember { mutableStateOf<String?>(null) }
    var sliderValue by remember { mutableDoubleStateOf(0.5) }

    val sortedPictures = remember(allProgressPictures) {
        allProgressPictures.sortedByDescending { it.dateCreated }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TabButton("Sessions", activeTab == "sessions") { activeTab = "sessions" }
            TabButton("Compare", activeTab == "compare") { activeTab = "compare" }
            Box(modifier = Modifier.weight(1f))
            IconButton(onClick = onAddPhoto) {
                Icon(Icons.Default.Add, contentDescription = "Add photo", tint = BWSColors.Accent)
            }
        }

        if (activeTab == "sessions") {
            SessionsTab(sessions = progressSessions)
        } else {
            CompareTab(
                pictures = sortedPictures,
                compareBeforeId = compareBeforeId,
                compareAfterId = compareAfterId,
                sliderValue = sliderValue,
                onSliderChange = { sliderValue = it },
                onPictureSelected = { picture ->
                    when {
                        compareBeforeId == null -> compareBeforeId = picture.id
                        compareAfterId == null && picture.id != compareBeforeId -> compareAfterId = picture.id
                        else -> {
                            compareBeforeId = picture.id
                            compareAfterId = null
                        }
                    }
                },
                onReset = {
                    compareBeforeId = null
                    compareAfterId = null
                    sliderValue = 0.5
                }
            )
        }
    }
}

@Composable
private fun TabButton(title: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) Color.White else BWSColors.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) BWSColors.Accent else BWSColors.SurfaceHighlight)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    )
}

@Composable
private fun SessionsTab(sessions: List<FPProgressSession>) {
    if (sessions.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 60.dp, start = 40.dp, end = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(Icons.Default.CameraAlt, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(48.dp))
            Text("No progress photos yet", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
            Text(
                "Tap + to add front, side, and back photos. They sync with FitPros.io automatically.",
                style = BWSTypography.Caption,
                color = BWSColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    } else {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sessions.forEach { session ->
                SessionCard(session = session)
            }
        }
    }
}

@Composable
private fun SessionCard(session: FPProgressSession) {
    BWSCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    formatSessionDate(session.dateCreated),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "${session.pictures.size} photo${if (session.pictures.size == 1) "" else "s"}",
                    style = BWSTypography.Caption,
                    color = BWSColors.TextSecondary
                )
            }
            session.notes?.takeIf { it.isNotEmpty() }?.let {
                Text(it, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                session.pictures.forEach { picture ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AsyncImage(
                            model = picture.imageUrl,
                            contentDescription = picture.poseType,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(
                            picture.poseType.replaceFirstChar { it.uppercase() },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = BWSColors.TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareTab(
    pictures: List<FPProgressPicture>,
    compareBeforeId: String?,
    compareAfterId: String?,
    sliderValue: Double,
    onSliderChange: (Double) -> Unit,
    onPictureSelected: (FPProgressPicture) -> Unit,
    onReset: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (pictures.size < 2) {
            Text(
                "Add at least two photos to compare your progress.",
                style = BWSTypography.Caption,
                color = BWSColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 40.dp)
            )
        } else {
            val before = pictures.find { it.id == compareBeforeId }
            val after = pictures.find { it.id == compareAfterId }

            Text(
                compareInstruction(before, after),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BWSColors.TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            )

            if (before != null && after != null) {
                CompareSlider(before = before, after = after, sliderValue = sliderValue, onSliderChange = onSliderChange)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                ) {
                    Text(
                        "Before · ${formatShortDate(before.dateCreated)}",
                        style = BWSTypography.Caption,
                        color = BWSColors.TextSecondary
                    )
                    Box(modifier = Modifier.weight(1f))
                    Text(
                        "After · ${formatShortDate(after.dateCreated)}",
                        style = BWSTypography.Caption,
                        color = BWSColors.Accent
                    )
                }

                Text(
                    "Reset selection",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BWSColors.TextSecondary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(onClick = onReset),
                    textAlign = TextAlign.End
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((pictures.size / 3 + 1) * 130).dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pictures, key = { it.id }) { picture ->
                    CompareThumbnail(
                        picture = picture,
                        isBefore = picture.id == compareBeforeId,
                        isAfter = picture.id == compareAfterId,
                        onClick = { onPictureSelected(picture) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareSlider(
    before: FPProgressPicture,
    after: FPProgressPicture,
    sliderValue: Double,
    onSliderChange: (Double) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .height(360.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = before.imageUrl,
                contentDescription = "Before",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(sliderValue.toFloat())
                    .clip(RoundedCornerShape(12.dp))
            ) {
                AsyncImage(
                    model = after.imageUrl,
                    contentDescription = "After",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(Color.White)
                )
            }
        }
        Slider(
            value = sliderValue.toFloat(),
            onValueChange = { onSliderChange(it.toDouble()) },
            modifier = Modifier.padding(horizontal = 20.dp),
            colors = SliderDefaults.colors(
                thumbColor = BWSColors.Accent,
                activeTrackColor = BWSColors.Accent,
                inactiveTrackColor = BWSColors.SurfaceHighlight
            )
        )
    }
}

@Composable
private fun CompareThumbnail(
    picture: FPProgressPicture,
    isBefore: Boolean,
    isAfter: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isBefore -> BWSColors.TextSecondary
        isAfter -> BWSColors.Accent
        else -> BWSColors.SurfaceHighlight
    }
    val borderWidth = if (isBefore || isAfter) 2.dp else 1.dp

    Column(
        modifier = Modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (picture.imageUrl.isNotEmpty()) {
            AsyncImage(
                model = picture.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(borderWidth, borderColor, RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
                    .background(BWSColors.SurfaceHighlight),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Photo, null, tint = BWSColors.TextTertiary)
            }
        }
        Text(formatShortDate(picture.dateCreated), fontSize = 11.sp, color = BWSColors.TextSecondary)
        if (isBefore || isAfter) {
            Text(
                if (isBefore) "Before" else "After",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isBefore) BWSColors.TextSecondary else BWSColors.Accent
            )
        }
    }
}

private fun compareInstruction(before: FPProgressPicture?, after: FPProgressPicture?): String = when {
    before == null -> "Select your \"before\" photo"
    after == null -> "Now select your \"after\" photo"
    else -> "Drag the slider to compare"
}

private fun formatSessionDate(date: java.util.Date): String =
    SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(date)

private fun formatShortDate(date: java.util.Date): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(date)