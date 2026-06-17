using System.Diagnostics;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using FitProjectWin.Models;
using FitProjectWin.Services;

namespace FitProjectWin.ViewModels;

public partial class WorkoutSessionViewModel : ObservableObject, IDisposable
{
    private readonly AppDataService _data;
    private readonly AuthService _auth;
    private readonly Stopwatch _stopwatch = new();
    private readonly Timer _timer;
    private Timer? _restTimer;

    public FPWorkout Workout { get; }
    public FPProgram? Program { get; }
    public Dictionary<string, List<FPLoggedSet>> LoggedSets { get; }

    [ObservableProperty] private int _currentExerciseIndex;
    [ObservableProperty] private string _notes = "";
    [ObservableProperty] private string _elapsedDisplay = "00:00";
    [ObservableProperty] private bool _showRestTimer;
    [ObservableProperty] private int _restSecondsRemaining;
    [ObservableProperty] private bool _showPrToast;
    [ObservableProperty] private string _prMessage = "";
    [ObservableProperty] private bool _isCompleting;

    private readonly List<FPPersonalRecord> _prs = [];

    public WorkoutSessionViewModel(AppDataService data, AuthService auth, FPWorkout workout, FPProgram? program)
    {
        _data = data;
        _auth = auth;
        Workout = workout;
        Program = program;
        LoggedSets = data.BuildLoggedSets(workout);

        _stopwatch.Start();
        _timer = new Timer(_ =>
        {
            var elapsed = _stopwatch.Elapsed;
            var display = $"{(int)elapsed.TotalMinutes:D2}:{elapsed.Seconds:D2}";
            Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread()?.TryEnqueue(() =>
                ElapsedDisplay = display);
        }, null, 0, 1000);
    }

    public List<FPWorkoutExercise> Exercises => Workout.Exercises.OrderBy(e => e.Index).ToList();

    public FPWorkoutExercise? CurrentExercise =>
        CurrentExerciseIndex < Exercises.Count ? Exercises[CurrentExerciseIndex] : null;

    public double ExerciseProgress => Exercises.Count > 0
        ? (double)(CurrentExerciseIndex + 1) / Exercises.Count : 0;

    public List<FPWorkoutMetric> CurrentMetrics
    {
        get
        {
            var ex = CurrentExercise;
            return ex is null ? [] : Workout.Metrics
                .Where(m => m.WorkoutExerciseId == ex.Id)
                .OrderBy(m => m.Index)
                .ToList();
        }
    }

    public List<FPLoggedSet> CurrentSets
    {
        get
        {
            var ex = CurrentExercise;
            return ex is not null && LoggedSets.TryGetValue(ex.Id, out var sets) ? sets : [];
        }
    }

    [RelayCommand]
    private void PreviousExercise()
    {
        if (CurrentExerciseIndex > 0) CurrentExerciseIndex--;
    }

    [RelayCommand]
    private void NextExercise()
    {
        if (CurrentExerciseIndex < Exercises.Count - 1) CurrentExerciseIndex++;
    }

    [RelayCommand]
    private void AddSet()
    {
        var ex = CurrentExercise;
        if (ex is null || !LoggedSets.TryGetValue(ex.Id, out var sets)) return;
        var last = sets.LastOrDefault();
        sets.Add(new FPLoggedSet
        {
            SetNumber = sets.Count + 1,
            Reps = last?.Reps,
            Weight = last?.Weight,
            Rpe = last?.Rpe,
            Rest = last?.Rest,
            Tempo = last?.Tempo,
            Time = last?.Time
        });
        OnPropertyChanged(nameof(CurrentSets));
    }

    [RelayCommand]
    private void ToggleSetComplete(FPLoggedSet? set)
    {
        if (set is null) return;
        set.IsCompleted = !set.IsCompleted;
        if (set.IsCompleted)
        {
            var ex = CurrentExercise;
            if (ex is not null)
            {
                var pr = _data.CheckForPR(ex, set);
                if (pr is not null)
                {
                    set.IsPr = true;
                    _prs.Add(pr);
                    PrMessage = $"{pr.ExerciseName} — {pr.Value} kg";
                    ShowPrToast = true;
                    Task.Delay(2500).ContinueWith(_ => ShowPrToast = false);
                }
            }
            if (int.TryParse(set.Rest, out var rest) && rest > 0)
                StartRestTimer(rest);
        }
        OnPropertyChanged(nameof(CurrentSets));
    }

    [RelayCommand]
    private void SkipRest() => ShowRestTimer = false;

    [RelayCommand]
    private void AddRest30() => RestSecondsRemaining += 30;

    [RelayCommand]
    private async Task CompleteWorkoutAsync()
    {
        IsCompleting = true;
        _stopwatch.Stop();
        await _data.CompleteWorkoutAsync(
            Workout, Program, LoggedSets, Notes,
            DateTime.UtcNow - _stopwatch.Elapsed,
            (int)_stopwatch.Elapsed.TotalSeconds, _prs);
        IsCompleting = false;
    }

    [RelayCommand]
    private void OpenVideo()
    {
        var exercise = CurrentExercise;
        if (exercise is not null && !string.IsNullOrEmpty(exercise.YoutubeId))
            App.ViewModel.PlayExerciseVideo(exercise.YoutubeId, exercise.Name);
    }

    private void StartRestTimer(int seconds)
    {
        _restTimer?.Dispose();
        RestSecondsRemaining = seconds;
        ShowRestTimer = true;
        _restTimer = new Timer(_ =>
        {
            var dq = Microsoft.UI.Dispatching.DispatcherQueue.GetForCurrentThread();
            dq?.TryEnqueue(() =>
            {
                if (RestSecondsRemaining > 0) RestSecondsRemaining--;
                else
                {
                    ShowRestTimer = false;
                    _restTimer?.Dispose();
                    _restTimer = null;
                }
            });
        }, null, 0, 1000);
    }

    public void Dispose()
    {
        _timer.Dispose();
        _restTimer?.Dispose();
    }
}