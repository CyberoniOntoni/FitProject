# FitProject

Native client apps for [FitPros.io](https://fitpros.io) — science-backed coaching, programs, and check-ins synced to your coach’s dashboard.

This repository also contains Windows and Apple clients. **This README focuses on the Android app** (`FitProjectDroid`).

## Android app

**FitProject** on Android lets coached clients follow assigned programs, log workouts, complete check-in forms, and track habits and body metrics — all tied to the same Firebase backend as FitPros.io.

Built with **Kotlin**, **Jetpack Compose**, and **Material 3**, with a dark UI inspired by Built With Science–style training apps.

### Requirements

- Android 8.0+ (API 26)
- A FitPros.io client account (Firebase Auth)
- **Health Connect** (optional, for steps and distance on the Summary tab)

### Main tabs

| Tab | What you can do |
|-----|-----------------|
| **Summary** | Daily greeting, steps and walking/running distance (via Health Connect), habit progress with quick +/- controls, and shortcuts to the full habits list |
| **Train** | Weekly workout goal ring, next assigned workout, and one-tap **Start Workout** |
| **Programs** | Browse coach-assigned programs and start any workout in the program |
| **Learn** | Coach-assigned **check-in forms** (e.g. Movement Assessment) and **guides/articles** with images |
| **History** | Past workout logs with per-exercise set detail, plus personal record highlights |
| **Profile** | Account hub (bottom sheet): habits, progress photos, measurements, PRs, forms, settings, sync status |

### Workout session

When you start a workout, a full-screen session opens with:

- Elapsed timer, exercise progress bar, and embedded **YouTube** demo when available
- Per-exercise **set logging** — reps, weight, RPE, rest, tempo, time (whatever metrics your coach configured)
- Set completion toggles, add set, per-exercise notes
- **Rest timer** overlay with skip and +30s
- **PR detection** toast when you beat a previous best
- Previous / next exercise navigation and **Complete Workout** sync to FitPros.io

### Check-in forms

Forms from your coach appear under **Learn** (and **Profile → Forms**).

- Supports text, numbers, ratings, linear scales, multiple choice, and checkboxes
- **Retake completed forms** — tap a finished form (e.g. Movement Assessment), edit your last answers, and resubmit; the latest submission replaces the previous one for your coach
- Pending forms show a badge count on Profile

### Profile & tracking

- **Habits** — daily targets with progress rings and quick logging
- **Progress photos** — multi-pose photo sessions uploaded to Firebase Storage
- **Body measurements** — weight and other metrics with trend chart
- **Personal records** — automatic PR tracking from logged workouts
- **Settings** — unit system (metric / imperial), theme, account sign-out

### Onboarding (experimental)

New clients can run a multi-step onboarding wizard that collects goals, experience, equipment, schedule, and body stats, then generates a starter program and optionally submits an intake form to the coach.

### Health Connect

The Summary tab reads **steps** and **distance** from Health Connect (not legacy Google Fit APIs). If Health Connect is missing or permissions are denied, the app shows install/connect prompts instead of failing silently.

### Sync & data

- Firebase **Authentication**, **Firestore**, and **Storage**
- Real-time listeners for habits and workout logs where applicable
- Full sync on login and background refresh; client-side filtering of assigned programs
- Unit preferences (e.g. km vs mi) stored per user

## Project structure (Android)

```
FitProjectDroid/
├── app/src/main/java/com/fitproject/droid/
│   ├── data/              # Models, Firestore, sync, Health Connect, onboarding
│   ├── ui/screens/        # Summary, Train, Programs, Learn, History, workout session, etc.
│   ├── ui/components/     # Shared Compose UI
│   ├── ui/navigation/     # Profile sheet navigation
│   └── viewmodel/         # AppViewModel, WorkoutSessionViewModel, OnboardingViewModel
└── app/google-services.json   # Firebase config (not committed — add your own)
```

## Build & run (Android)

1. Clone the repo and open `FitProjectDroid` in Android Studio.
2. Add `app/google-services.json` from your Firebase project (same project as FitPros.io).
3. Sync Gradle and run on a device or emulator (API 26+).

```bash
cd FitProjectDroid
./gradlew assembleDebug
```

Install the APK from `app/build/outputs/apk/debug/`, or use **Run** in Android Studio.

For Health Connect on emulator/device, install the [Health Connect app](https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata) and grant permissions when prompted on the Summary tab.

## Branches

- **`main`** — stable line
- **`experimental`** — active development (Summary tab, Health Connect, onboarding, form retake, UI fixes, etc.)

## Other platforms in this repo

| Directory | Platform |
|-----------|----------|
| `FitProjectDroid/` | Android (this README) |
| `FitProjectWin/` | Windows (WinUI 3 / .NET 8) |
| `FitProjectApple/` | iOS / macOS (SwiftUI) |

Android, Windows, and Apple share the FitPros.io backend but are maintained as **separate clients** with platform-specific UI.

## License

GNU General Public License v3.0 — see [LICENSE](LICENSE).