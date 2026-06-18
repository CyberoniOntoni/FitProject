package com.fitproject.droid.ui.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.fitproject.droid.data.FPUser
import com.fitproject.droid.ui.components.AppleGroupedSection
import com.fitproject.droid.ui.components.BWSCard
import com.fitproject.droid.ui.components.ProfileMenuDivider
import com.fitproject.droid.ui.components.ProfileMenuRow

import com.fitproject.droid.ui.navigation.ProfileDestination
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun ProfileScreen(
    user: FPUser?,
    isSyncing: Boolean,
    lastSyncDate: Date?,
    syncError: String? = null,
    habitsCount: Int,
    progressSessionsCount: Int,
    measurementsCount: Int,
    personalRecordsCount: Int,
    pendingFormsCount: Int,
    massAbbreviation: String,
    onNavigate: (ProfileDestination) -> Unit,
    onRefresh: () -> Unit = {},
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ProfileHeader(user = user)

        SyncStatusCard(
            isSyncing = isSyncing,
            lastSyncDate = lastSyncDate,
            syncError = syncError,
            onRefresh = onRefresh
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "TRACK & RECORD",
                style = BWSTypography.Footnote,
                color = BWSColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            AppleGroupedSection {
                ProfileMenuRow(
                    icon = Icons.Default.TaskAlt,
                    title = "Habits",
                    subtitle = "Record daily habits · $habitsCount active",
                    onClick = { onNavigate(ProfileDestination.HABITS) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = Icons.Default.CameraAlt,
                    title = "Progress Photos",
                    subtitle = "Visual body transformation · $progressSessionsCount sessions",
                    onClick = { onNavigate(ProfileDestination.PROGRESS_PHOTOS) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = Icons.Default.Straighten,
                    title = "Body Measurements",
                    subtitle = "Weight, body comp & more · $measurementsCount entries",
                    onClick = { onNavigate(ProfileDestination.MEASUREMENTS) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = Icons.Default.EmojiEvents,
                    title = "Personal Records",
                    subtitle = "$personalRecordsCount records synced from workouts",
                    onClick = { onNavigate(ProfileDestination.PERSONAL_RECORDS) }
                )
                ProfileMenuDivider()
                ProfileMenuRow(
                    icon = Icons.Default.Description,
                    title = "Forms",
                    subtitle = "$pendingFormsCount pending",
                    onClick = { onNavigate(ProfileDestination.FORMS) }
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "SETTINGS",
                style = BWSTypography.Footnote,
                color = BWSColors.TextSecondary,
                modifier = Modifier.padding(start = 4.dp)
            )
            AppleGroupedSection {
                ProfileMenuRow(
                    icon = Icons.Default.Settings,
                    title = "Unit Preferences",
                    subtitle = "Weight, circumference & more · $massAbbreviation",
                    onClick = { onNavigate(ProfileDestination.SETTINGS) }
                )
            }
        }

        AppleGroupedSection {
            Text(
                text = "Sign Out",
                style = BWSTypography.Headline,
                color = BWSColors.Error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSignOut)
                    .padding(vertical = 14.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun ProfileHeader(user: FPUser?) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(BWSColors.Accent),
            contentAlignment = Alignment.Center
        ) {
            if (!user?.profilePictureUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    user?.initials ?: "?",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Text(user?.displayName ?: "User", style = BWSTypography.Headline, color = BWSColors.TextPrimary)
        Text(user?.email ?: "", style = BWSTypography.Caption, color = BWSColors.TextSecondary)
    }
}

@Composable
private fun SyncStatusCard(
    isSyncing: Boolean,
    lastSyncDate: Date?,
    syncError: String?,
    onRefresh: () -> Unit
) {
    BWSCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Sync, null, tint = BWSColors.Accent)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        "FitPros.io Sync",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BWSColors.TextPrimary
                    )
                    Text(
                        text = when {
                            isSyncing -> "Syncing..."
                            syncError != null -> syncError
                            lastSyncDate != null -> "Last synced ${formatRelativeTime(lastSyncDate)}"
                            else -> "Not synced yet"
                        },
                        style = BWSTypography.Caption,
                        color = if (syncError != null) BWSColors.Error else BWSColors.TextSecondary
                    )
                }
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = BWSColors.Accent,
                        strokeWidth = 2.dp
                    )
                } else if (syncError == null) {
                    Icon(Icons.Default.CheckCircle, null, tint = BWSColors.Success)
                }
            }

            Text(
                text = "Refresh Data",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isSyncing) BWSColors.TextTertiary else BWSColors.Accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(BWSColors.ButtonRadius.dp))
                    .background(BWSColors.Accent.copy(alpha = 0.1f))
                    .clickable(enabled = !isSyncing, onClick = onRefresh)
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

private fun formatRelativeTime(date: Date): String {
    val diffMs = System.currentTimeMillis() - date.time
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diffMs)
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 1440 -> "${minutes / 60} hr ago"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}