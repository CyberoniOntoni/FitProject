package com.fitproject.droid.ui.screens

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fitproject.droid.data.FPProgressPhotoDraft
import com.fitproject.droid.data.PoseImagePayload
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun AddProgressPhotoScreen(
    onSave: suspend (FPProgressPhotoDraft) -> Result<Unit>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf(FPProgressPhotoDraft()) }
    var activePose by remember { mutableStateOf("front") }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var statusText by remember { mutableStateOf("Add at least one pose (front, side, or back).") }
    var isSaving by remember { mutableStateOf(false) }

    val poses = listOf("front", "side", "back")
    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    fun loadPreviewForPose(pose: String) {
        val payload = draft.poseImageData[pose]
        previewBitmap = payload?.data?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@rememberLauncherForActivityResult
            val bytes = inputStream.readBytes()
            inputStream.close()

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val jpegStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, jpegStream)
            val jpegBytes = jpegStream.toByteArray()

            draft = draft.copy(
                poseImageData = draft.poseImageData + (activePose to PoseImagePayload(
                    data = jpegBytes,
                    contentType = "image/jpeg",
                    fileExtension = "jpg"
                ))
            )
            previewBitmap = bitmap
            val count = draft.completedPoseCount
            statusText = if (count == 0) {
                "Add at least one pose (front, side, or back)."
            } else {
                "$count pose${if (count == 1) "" else "s"} ready — switch poses to add more."
            }
        } catch (_: Exception) {
            statusText = "Failed to load image. Please try again."
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BWSColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            "← Back",
            style = BWSTypography.Caption,
            color = BWSColors.Accent,
            modifier = Modifier.clickable(enabled = !isSaving, onClick = onDismiss)
        )

        Text("Add Progress Photo", style = BWSTypography.Headline, color = BWSColors.TextPrimary)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BWSColors.Surface)
                .clickable(enabled = !isSaving) {
                    val cal = Calendar.getInstance().apply { time = draft.dateCreated }
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            cal.set(year, month, day)
                            draft = draft.copy(dateCreated = cal.time)
                        },
                        cal.get(Calendar.YEAR),
                        cal.get(Calendar.MONTH),
                        cal.get(Calendar.DAY_OF_MONTH)
                    ).show()
                }
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Session Date", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextPrimary)
            Text(dateFormatter.format(draft.dateCreated), style = BWSTypography.Caption, color = BWSColors.Accent)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Pose", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextPrimary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                poses.forEach { pose ->
                    Text(
                        pose.replaceFirstChar { it.uppercase() },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (activePose == pose) androidx.compose.ui.graphics.Color.White else BWSColors.TextPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (activePose == pose) BWSColors.Accent else BWSColors.SurfaceHighlight)
                            .clickable(enabled = !isSaving) {
                                activePose = pose
                                loadPreviewForPose(pose)
                            }
                            .padding(vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        BWSCard(padding = 0.dp) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp),
                contentAlignment = Alignment.Center
            ) {
                val bitmap = previewBitmap
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = activePose,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.Photo, null, tint = BWSColors.TextTertiary, modifier = Modifier.size(40.dp))
                        Text(
                            "Tap below to add $activePose photo",
                            style = BWSTypography.Caption,
                            color = BWSColors.TextSecondary
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BWSColors.Surface)
                .border(1.dp, BWSColors.SurfaceHighlight, RoundedCornerShape(12.dp))
                .clickable(enabled = !isSaving) { imagePicker.launch("image/*") }
                .padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.PhotoLibrary, null, tint = BWSColors.Accent)
            Text(
                "  Choose Photo",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BWSColors.Accent
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notes (optional)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = BWSColors.TextPrimary)
            OutlinedTextField(
                value = draft.notes,
                onValueChange = { draft = draft.copy(notes = it) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BWSColors.TextPrimary,
                    unfocusedTextColor = BWSColors.TextPrimary,
                    focusedContainerColor = BWSColors.Surface,
                    unfocusedContainerColor = BWSColors.Surface,
                    focusedBorderColor = BWSColors.SurfaceHighlight,
                    unfocusedBorderColor = BWSColors.SurfaceHighlight,
                    cursorColor = BWSColors.Accent
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        Text(
            statusText,
            style = BWSTypography.Caption,
            color = BWSColors.TextSecondary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        BWSPrimaryButton(
            title = if (isSaving) "Uploading…" else "Save Session",
            isLoading = isSaving,
            enabled = !isSaving,
            onClick = {
                if (draft.completedPoseCount == 0) {
                    statusText = "Please add at least one photo before saving."
                    return@BWSPrimaryButton
                }
                scope.launch {
                    isSaving = true
                    statusText = "Uploading photos…"
                    onSave(draft)
                        .onSuccess { onDismiss() }
                        .onFailure { error ->
                            statusText = error.localizedMessage ?: "Upload failed. Please try again."
                            isSaving = false
                        }
                }
            }
        )
    }
}