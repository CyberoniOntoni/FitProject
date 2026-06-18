package com.fitproject.droid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fitproject.droid.data.AppTab
import com.fitproject.droid.data.FPContent
import com.fitproject.droid.data.FPForm
import com.fitproject.droid.ui.components.BWSPrimaryButton
import com.fitproject.droid.ui.screens.FormFillScreen
import com.fitproject.droid.ui.navigation.ProfileNavHost
import com.fitproject.droid.ui.screens.ContentDetailScreen
import com.fitproject.droid.ui.screens.HistoryScreen
import com.fitproject.droid.ui.screens.LearnScreen
import com.fitproject.droid.ui.screens.ProgramsScreen
import com.fitproject.droid.ui.screens.OnboardingWizardScreen
import com.fitproject.droid.ui.screens.TrainScreen
import com.fitproject.droid.ui.screens.WorkoutSessionScreen
import com.fitproject.droid.viewmodel.OnboardingViewModel
import com.fitproject.droid.ui.theme.BWSColors
import com.fitproject.droid.ui.theme.BWSTypography
import com.fitproject.droid.ui.theme.FitProjectTheme
import com.fitproject.droid.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitProjectTheme {
                FitProjectRoot()
            }
        }
    }
}

@Composable
fun FitProjectRoot(
    appViewModel: AppViewModel = viewModel(),
    onboardingViewModel: OnboardingViewModel = viewModel()
) {
    val isAuthenticated by appViewModel.isAuthenticated.collectAsStateWithLifecycle()
    val needsOnboarding by appViewModel.needsOnboarding.collectAsStateWithLifecycle()
    val forms by appViewModel.forms.collectAsStateWithLifecycle()
    val currentUser by appViewModel.currentUser.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val activeSession by appViewModel.activeWorkoutSession.collectAsStateWithLifecycle()
    var workoutDismissed by remember { mutableStateOf(false) }
    val sessionKey = activeSession?.let { "${it.workout.id}-${it.startedAt.time}" }

    LaunchedEffect(sessionKey) {
        if (sessionKey != null) {
            workoutDismissed = false
        }
    }

    LaunchedEffect(isAuthenticated, needsOnboarding) {
        when {
            !isAuthenticated -> {
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            }
            needsOnboarding -> {
                navController.navigate("onboarding") {
                    popUpTo("login") { inclusive = true }
                }
            }
            else -> {
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
    }

    LaunchedEffect(activeSession, workoutDismissed) {
        when {
            activeSession == null && navController.currentDestination?.route == "workout" ->
                navController.popBackStack()
            activeSession != null && !workoutDismissed &&
                navController.currentDestination?.route != "workout" ->
                navController.navigate("workout")
        }
    }

    NavHost(
        navController = navController,
        startDestination = if (isAuthenticated) "main" else "login",
        modifier = Modifier.background(BWSColors.Background)
    ) {
        composable("login") {
            LoginScreen(appViewModel = appViewModel)
        }
        composable("onboarding") {
            val profile by onboardingViewModel.profile.collectAsStateWithLifecycle()
            OnboardingWizardScreen(
                viewModel = onboardingViewModel,
                userId = currentUser?.id,
                forms = forms,
                harvestedWorkouts = appViewModel.harvestedWorkouts(),
                onComplete = {
                    appViewModel.completeOnboarding(profile.firstName)
                },
                onSubmitForm = { form, answers -> appViewModel.submitForm(form, answers) }
            )
        }
        composable("main") {
            MainShell(appViewModel = appViewModel)
        }
        composable("workout") {
            activeSession?.let { session ->
                WorkoutSessionScreen(
                    session = session,
                    appViewModel = appViewModel,
                    onDismiss = {
                        workoutDismissed = true
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(appViewModel: AppViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    val authLoading by appViewModel.authLoading.collectAsStateWithLifecycle()
    val authError by appViewModel.authError.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BWSColors.Background)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = null,
                tint = BWSColors.Accent,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "FitProjectDroid",
                style = BWSTypography.Title,
                color = BWSColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Science-backed training.\nSynced with FitPros.",
                style = BWSTypography.Body,
                color = BWSColors.TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(48.dp))

            val fieldColors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = BWSColors.TextPrimary,
                unfocusedTextColor = BWSColors.TextPrimary,
                focusedContainerColor = BWSColors.Surface,
                unfocusedContainerColor = BWSColors.Surface,
                focusedBorderColor = BWSColors.Accent.copy(alpha = 0.5f),
                unfocusedBorderColor = BWSColors.SurfaceHighlight,
                cursorColor = BWSColors.Accent
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(BWSColors.ButtonRadius.dp),
                colors = fieldColors
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = BWSColors.TextTertiary
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(BWSColors.ButtonRadius.dp),
                colors = fieldColors
            )

            authError?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error, style = BWSTypography.Caption, color = BWSColors.Error)
            }

            Spacer(modifier = Modifier.height(24.dp))
            BWSPrimaryButton(
                title = "Sign In",
                isLoading = authLoading,
                onClick = { appViewModel.signIn(email, password) }
            )

            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Syncs seamlessly with FitPros.io",
                style = BWSTypography.Caption,
                color = BWSColors.TextTertiary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainShell(appViewModel: AppViewModel) {
    val selectedTab by appViewModel.selectedTab.collectAsStateWithLifecycle()
    val showProfileSheet by appViewModel.showProfileSheet.collectAsStateWithLifecycle()

    val isSyncing by appViewModel.isSyncing.collectAsStateWithLifecycle()
    val weeklyCompleted by appViewModel.weeklyWorkoutsCompleted.collectAsStateWithLifecycle()
    val weeklyGoal by appViewModel.weeklyWorkoutGoal.collectAsStateWithLifecycle()
    val nextWorkout by appViewModel.nextWorkout.collectAsStateWithLifecycle()
    val nextProgram by appViewModel.nextProgram.collectAsStateWithLifecycle()
    val programs by appViewModel.programs.collectAsStateWithLifecycle()
    val programWeeks by appViewModel.programWeeks.collectAsStateWithLifecycle()
    val habits by appViewModel.habits.collectAsStateWithLifecycle()
    val workoutLogs by appViewModel.workoutLogs.collectAsStateWithLifecycle()
    val personalRecords by appViewModel.personalRecords.collectAsStateWithLifecycle()
    val forms by appViewModel.forms.collectAsStateWithLifecycle()
    val content by appViewModel.content.collectAsStateWithLifecycle()
    val currentUser by appViewModel.currentUser.collectAsStateWithLifecycle()

    var selectedContent by remember { mutableStateOf<FPContent?>(null) }
    var selectedForm by remember { mutableStateOf<FPForm?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = BWSColors.Background,
            bottomBar = {
                Column {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BWSColors.SurfaceElevated)
                            .padding(top = 10.dp, bottom = 24.dp)
                    ) {
                        TabBarItem(
                            icon = Icons.Default.FitnessCenter,
                            label = AppTab.TRAIN.label,
                            selected = selectedTab == AppTab.TRAIN,
                            onClick = { appViewModel.setSelectedTab(AppTab.TRAIN) },
                            modifier = Modifier.weight(1f)
                        )
                        TabBarItem(
                            icon = Icons.AutoMirrored.Filled.List,
                            label = AppTab.PROGRAMS.label,
                            selected = selectedTab == AppTab.PROGRAMS,
                            onClick = { appViewModel.setSelectedTab(AppTab.PROGRAMS) },
                            modifier = Modifier.weight(1f)
                        )
                        TabBarItem(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = AppTab.LEARN.label,
                            selected = selectedTab == AppTab.LEARN,
                            onClick = { appViewModel.setSelectedTab(AppTab.LEARN) },
                            modifier = Modifier.weight(1f)
                        )
                        TabBarItem(
                            icon = Icons.Default.AccessTime,
                            label = AppTab.HISTORY.label,
                            selected = selectedTab == AppTab.HISTORY,
                            onClick = { appViewModel.setSelectedTab(AppTab.HISTORY) },
                            modifier = Modifier.weight(1f)
                        )
                        TabBarItem(
                            icon = Icons.Default.Person,
                            label = "Profile",
                            selected = showProfileSheet,
                            onClick = { appViewModel.setShowProfileSheet(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(BWSColors.Background)
            ) {
                when (selectedTab) {
                    AppTab.TRAIN -> TrainScreen(
                        isSyncing = isSyncing,
                        weeklyCompleted = weeklyCompleted,
                        weeklyGoal = weeklyGoal,
                        nextWorkout = nextWorkout,
                        nextProgram = nextProgram,
                        habits = habits,
                        userFirstName = currentUser?.firstName ?: "",
                        onStartWorkout = { workout, program -> appViewModel.startWorkout(workout, program) },
                        onUpdateHabit = appViewModel::updateHabit,
                        onSeeAllHabits = { appViewModel.setShowProfileSheet(true) }
                    )
                    AppTab.PROGRAMS -> ProgramsScreen(
                        programs = programs,
                        programWeeks = programWeeks,
                        onStartWorkout = { workout, program -> appViewModel.startWorkout(workout, program) }
                    )
                    AppTab.LEARN -> LearnScreen(
                        forms = forms,
                        content = content,
                        userId = currentUser?.id,
                        onFormTap = { selectedForm = it },
                        onContentTap = { selectedContent = it }
                    )
                    AppTab.HISTORY -> HistoryScreen(
                        workoutLogs = workoutLogs,
                        personalRecords = personalRecords,
                        onLogTap = { }
                    )
                    AppTab.PROFILE -> { /* Profile is handled via bottom sheet */ }
                }
            }
        }

        selectedContent?.let { item ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BWSColors.Background)
            ) {
                ContentDetailScreen(
                    content = item,
                    modifier = Modifier.padding(top = 48.dp)
                )
                Text(
                    "← Back",
                    style = BWSTypography.Caption,
                    color = BWSColors.Accent,
                    modifier = Modifier
                        .padding(20.dp)
                        .clickable { selectedContent = null }
                )
            }
        }

        selectedForm?.let { form ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BWSColors.Background)
            ) {
                FormFillScreen(
                    form = form,
                    onSubmit = { f, answers -> appViewModel.submitForm(f, answers) },
                    onDismiss = { selectedForm = null }
                )
            }
        }

        if (showProfileSheet) {
            ModalBottomSheet(
                onDismissRequest = { appViewModel.setShowProfileSheet(false) },
                sheetState = sheetState,
                containerColor = BWSColors.Background,
                dragHandle = null
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.92f)
                ) {
                    ProfileNavHost(
                        appViewModel = appViewModel,
                        onDismiss = { appViewModel.setShowProfileSheet(false) },
                        onFormTap = { form ->
                            appViewModel.setShowProfileSheet(false)
                            selectedForm = form
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TabBarItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) BWSColors.Accent else BWSColors.TextTertiary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (selected) BWSColors.Accent else BWSColors.TextTertiary
        )
    }
}

