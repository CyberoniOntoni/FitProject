package com.fitproject.droid.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.data.FPFormAnswer
import com.fitproject.droid.data.FPFormField
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.screens.AddProgressPhotoScreen
import com.fitproject.droid.ui.screens.FormsListScreen
import com.fitproject.droid.ui.screens.HabitsScreen
import com.fitproject.droid.ui.screens.MeasurementsScreen
import com.fitproject.droid.ui.screens.PersonalRecordsScreen
import com.fitproject.droid.ui.screens.ProfileScreen
import com.fitproject.droid.ui.screens.ProgressPhotosScreen
import com.fitproject.droid.ui.screens.SettingsScreen
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import com.fitproject.droid.viewmodel.AppViewModel

enum class ProfileDestination(val route: String, val title: String) {
    PROFILE("profile", "Profile"),
    HABITS("habits", "Habits"),
    PROGRESS_PHOTOS("progress_photos", "Progress Photos"),
    ADD_PROGRESS_PHOTO("add_progress_photo", "Add Progress Photo"),
    MEASUREMENTS("measurements", "Body Measurements"),
    PERSONAL_RECORDS("personal_records", "Personal Records"),
    FORMS("forms", "Forms"),
    SETTINGS("settings", "Unit Preferences")
}

@Composable
fun ProfileNavHost(
    appViewModel: AppViewModel,
    navController: NavHostController = rememberNavController(),
    onDismiss: () -> Unit,
    onFormTap: (FPForm) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val user by appViewModel.currentUser.collectAsStateWithLifecycle()
    val isSyncing by appViewModel.isSyncing.collectAsStateWithLifecycle()
    val lastSyncDate by appViewModel.lastSyncDate.collectAsStateWithLifecycle()
    val habits by appViewModel.habits.collectAsStateWithLifecycle()
    val progressSessions by appViewModel.progressSessions.collectAsStateWithLifecycle()
    val allProgressPictures by appViewModel.allProgressPictures.collectAsStateWithLifecycle()
    val measurements by appViewModel.measurements.collectAsStateWithLifecycle()
    val personalRecords by appViewModel.personalRecords.collectAsStateWithLifecycle()
    val forms by appViewModel.forms.collectAsStateWithLifecycle()
    val unitPreferences by appViewModel.unitPreferences.collectAsStateWithLifecycle()

    val pendingFormsCount = remember(forms, user) {
        val userId = user?.id
        if (userId == null) 0 else forms.count { !it.isCompleted(userId) }
    }

    NavHost(
        navController = navController,
        startDestination = ProfileDestination.PROFILE.route,
        modifier = modifier.background(BWSColors.Background)
    ) {
        composable(ProfileDestination.PROFILE.route) {
            Column {
                ProfileSheetHeader(
                    title = ProfileDestination.PROFILE.title,
                    showBack = false,
                    onBack = {},
                    onClose = onDismiss
                )
                ProfileScreen(
                    user = user,
                    isSyncing = isSyncing,
                    lastSyncDate = lastSyncDate,
                    habitsCount = habits.size,
                    progressSessionsCount = progressSessions.size,
                    measurementsCount = measurements.size,
                    personalRecordsCount = personalRecords.size,
                    pendingFormsCount = pendingFormsCount,
                    massAbbreviation = unitPreferences.massAbbreviation,
                    onNavigate = { destination -> navController.navigate(destination.route) },
                    onSignOut = {
                        onDismiss()
                        appViewModel.signOut()
                    }
                )
            }
        }

        composable(ProfileDestination.HABITS.route) {
            ProfileSubScreen(
                title = ProfileDestination.HABITS.title,
                onBack = { navController.popBackStack() },
                onClose = onDismiss
            ) {
                HabitsScreen(habits = habits, onUpdateHabit = appViewModel::updateHabit)
            }
        }

        composable(ProfileDestination.PROGRESS_PHOTOS.route) {
            ProfileSubScreen(
                title = ProfileDestination.PROGRESS_PHOTOS.title,
                onBack = { navController.popBackStack() },
                onClose = onDismiss
            ) {
                ProgressPhotosScreen(
                    progressSessions = progressSessions,
                    allProgressPictures = allProgressPictures,
                    onAddPhoto = { navController.navigate(ProfileDestination.ADD_PROGRESS_PHOTO.route) }
                )
            }
        }

        composable(ProfileDestination.ADD_PROGRESS_PHOTO.route) {
            AddProgressPhotoScreen(
                onSave = { draft ->
                    appViewModel.saveProgressPhoto(draft)
                    navController.popBackStack()
                },
                onDismiss = { navController.popBackStack() }
            )
        }

        composable(ProfileDestination.MEASUREMENTS.route) {
            ProfileSubScreen(
                title = ProfileDestination.MEASUREMENTS.title,
                onBack = { navController.popBackStack() },
                onClose = onDismiss
            ) {
                MeasurementsScreen(
                    measurements = measurements,
                    unitPreferences = unitPreferences,
                    onSaveMeasurement = appViewModel::saveMeasurement
                )
            }
        }

        composable(ProfileDestination.PERSONAL_RECORDS.route) {
            ProfileSubScreen(
                title = ProfileDestination.PERSONAL_RECORDS.title,
                onBack = { navController.popBackStack() },
                onClose = onDismiss
            ) {
                PersonalRecordsScreen(personalRecords = personalRecords)
            }
        }

        composable(ProfileDestination.FORMS.route) {
            ProfileSubScreen(
                title = ProfileDestination.FORMS.title,
                onBack = { navController.popBackStack() },
                onClose = onDismiss
            ) {
                FormsListScreen(
                    forms = forms,
                    userId = user?.id,
                    onFormTap = onFormTap
                )
            }
        }

        composable(ProfileDestination.SETTINGS.route) {
            ProfileSubScreen(
                title = ProfileDestination.SETTINGS.title,
                onBack = { navController.popBackStack() },
                onClose = onDismiss
            ) {
                SettingsScreen(
                    unitPreferences = unitPreferences,
                    onUpdatePreference = appViewModel::updateUnitPreference
                )
            }
        }
    }
}

@Composable
private fun ProfileSubScreen(
    title: String,
    onBack: () -> Unit,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ProfileSheetHeader(
            title = title,
            showBack = true,
            onBack = onBack,
            onClose = onClose
        )
        Box(modifier = Modifier.weight(1f)) {
            content()
        }
    }
}

@Composable
fun ProfileSheetHeader(
    title: String,
    showBack: Boolean,
    onBack: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(BWSColors.SurfaceElevated)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (showBack) {
            Text(
                "←",
                fontSize = 20.sp,
                color = BWSColors.Accent,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(end = 12.dp)
            )
        }
        Text(
            title,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            "✕",
            fontSize = 18.sp,
            color = BWSColors.TextSecondary,
            modifier = Modifier.clickable(onClick = onClose)
        )
    }
}

@Composable
fun FormFillScreen(
    form: FPForm,
    onSubmit: (FPForm, List<FPFormAnswer>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var answers by remember(form.id) {
        mutableStateOf(form.fields.associate { it.id to "" })
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BWSColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            "← Back",
            style = BWSTypography.Caption,
            color = BWSColors.Accent,
            modifier = Modifier.clickable(onClick = onDismiss)
        )
        Text(
            form.title,
            style = BWSTypography.Headline,
            color = BWSColors.TextPrimary,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        form.description?.let {
            Text(it, style = BWSTypography.Caption, color = BWSColors.TextSecondary)
        }

        form.fields.sortedBy { it.index }.forEach { field ->
            FormFieldInput(
                field = field,
                value = answers[field.id] ?: "",
                onValueChange = { answers = answers + (field.id to it) }
            )
        }

        BWSPrimaryButton(
            title = "Submit",
            onClick = {
                val formAnswers = form.fields.map { field ->
                    FPFormAnswer(
                        fieldId = field.id,
                        question = field.question,
                        type = field.type,
                        value = answers[field.id] ?: ""
                    )
                }
                onSubmit(form, formAnswers)
                onDismiss()
            },
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
private fun FormFieldInput(
    field: FPFormField,
    value: String,
    onValueChange: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            field.question,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BWSColors.TextPrimary
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            minLines = if (field.type == "TextArea") 3 else 1,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BWSColors.TextPrimary,
                unfocusedTextColor = BWSColors.TextPrimary,
                focusedContainerColor = BWSColors.Surface,
                unfocusedContainerColor = BWSColors.Surface,
                focusedBorderColor = BWSColors.Accent.copy(alpha = 0.5f),
                unfocusedBorderColor = BWSColors.SurfaceHighlight,
                cursorColor = BWSColors.Accent
            )
        )
    }
}