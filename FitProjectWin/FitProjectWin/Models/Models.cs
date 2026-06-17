namespace FitProjectWin.Models;

public sealed class FPUser
{
    public string Id { get; set; } = "";
    public string Email { get; set; } = "";
    public string FirstName { get; set; } = "";
    public string LastName { get; set; } = "";
    public string? ProfilePictureUrl { get; set; }
    public string? Timezone { get; set; }
    public bool CoachHasProTools { get; set; }

    public string DisplayName
    {
        get
        {
            var name = $"{FirstName} {LastName}".Trim();
            return string.IsNullOrEmpty(name) ? Email : name;
        }
    }

    public string Initials
    {
        get
        {
            var parts = DisplayName.Split(' ', StringSplitOptions.RemoveEmptyEntries);
            var first = parts.Length > 0 ? parts[0][..1] : "";
            var last = parts.Length > 1 ? parts[^1][..1] : "";
            return $"{first}{last}".ToUpperInvariant();
        }
    }
}

public sealed class FPProgram
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string Description { get; set; } = "";
    public string? ImageUrl { get; set; }
    public int TotalWeekCount { get; set; }
    public int TotalWorkoutCount { get; set; }
    public bool Published { get; set; }
    public List<string> CreatorIds { get; set; } = [];
    public int CompletedWorkouts { get; set; }

    public double Progress => TotalWorkoutCount > 0 ? (double)CompletedWorkouts / TotalWorkoutCount : 0;
    public int ProgressPercent => (int)(Progress * 100);
}

public sealed class FPProgramWeek
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public int Index { get; set; }
    public string ProgramId { get; set; } = "";
    public List<FPWorkout> Workouts { get; set; } = [];
}

public sealed class FPWorkout
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string? Description { get; set; }
    public int Index { get; set; }
    public string ProgramId { get; set; } = "";
    public string ProgramWeekId { get; set; } = "";
    public string? Notes { get; set; }
    public List<FPExerciseGroup> ExerciseGroups { get; set; } = [];
    public List<FPWorkoutExercise> Exercises { get; set; } = [];
    public List<FPWorkoutMetric> Metrics { get; set; } = [];
    public List<FPMetricValue> MetricValues { get; set; } = [];
    public bool IsCompleted { get; set; }
    public DateTime? CompletedAt { get; set; }

    public int ExerciseCount => Exercises.Count;
}

public sealed class FPExerciseGroup
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public int Index { get; set; }
    public string? Type { get; set; }
}

public sealed class FPWorkoutExercise
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string ExerciseId { get; set; } = "";
    public string? YoutubeId { get; set; }
    public string? ThumbnailUrl { get; set; }
    public int Index { get; set; }
    public int Sets { get; set; }
    public string? CoachNotes { get; set; }
    public string? Header { get; set; }
    public bool HeaderVisible { get; set; }
    public string? GroupId { get; set; }
    public bool IsSuperset { get; set; }

    public string? VideoThumbnailUrl =>
        !string.IsNullOrEmpty(ThumbnailUrl) ? ThumbnailUrl :
        !string.IsNullOrEmpty(YoutubeId) ? $"https://img.youtube.com/vi/{YoutubeId}/mqdefault.jpg" : null;

    public string? YoutubeWatchUrl =>
        !string.IsNullOrEmpty(YoutubeId) ? $"https://www.youtube.com/watch?v={YoutubeId}" : null;
}

public sealed class FPWorkoutMetric
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public int Index { get; set; }
    public string Color { get; set; } = "#ffffff";
    public string ExerciseMetricId { get; set; } = "";
    public string WorkoutExerciseId { get; set; } = "";
    public string? UnitAbbreviation { get; set; }
}

public sealed class FPMetricValue
{
    public string Id { get; set; } = "";
    public string WorkoutExerciseId { get; set; } = "";
    public string WorkoutExerciseMetricId { get; set; } = "";
    public string ExerciseMetricId { get; set; } = "";
    public string ExerciseId { get; set; } = "";
    public int Set { get; set; }
    public int Index { get; set; }
    public string Value { get; set; } = "";
    public string? LoggedValue { get; set; }
    public bool IsCompleted { get; set; }
}

public sealed class FPWorkoutLog
{
    public string Id { get; set; } = "";
    public string UserId { get; set; } = "";
    public string WorkoutId { get; set; } = "";
    public string? ProgramId { get; set; }
    public string WorkoutName { get; set; } = "";
    public DateTime StartedAt { get; set; }
    public DateTime? CompletedAt { get; set; }
    public int? DurationSeconds { get; set; }
    public List<FPLoggedExercise> Exercises { get; set; } = [];
    public string? Notes { get; set; }
    public double TotalVolume { get; set; }
    public int PrCount { get; set; }

    public bool IsCompleted => CompletedAt.HasValue;
}

public sealed class FPLoggedExercise
{
    public string Id { get; set; } = "";
    public string ExerciseId { get; set; } = "";
    public string Name { get; set; } = "";
    public List<FPLoggedSet> Sets { get; set; } = [];
}

public sealed class FPLoggedSet : System.ComponentModel.INotifyPropertyChanged
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public int SetNumber { get; set; }
    private string? _reps;
    private string? _weight;
    private string? _rpe;
    private string? _rest;
    private string? _tempo;
    private string? _time;
    private bool _isCompleted;
    private bool _isPr;

    public string? Reps { get => _reps; set { _reps = value; OnChanged(nameof(Reps)); } }
    public string? Weight { get => _weight; set { _weight = value; OnChanged(nameof(Weight)); } }
    public string? Rpe { get => _rpe; set { _rpe = value; OnChanged(nameof(Rpe)); } }
    public string? Rest { get => _rest; set { _rest = value; OnChanged(nameof(Rest)); } }
    public string? Tempo { get => _tempo; set { _tempo = value; OnChanged(nameof(Tempo)); } }
    public string? Time { get => _time; set { _time = value; OnChanged(nameof(Time)); } }
    public bool IsCompleted { get => _isCompleted; set { _isCompleted = value; OnChanged(nameof(IsCompleted)); } }
    public bool IsPr { get => _isPr; set { _isPr = value; OnChanged(nameof(IsPr)); } }

    public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
    private void OnChanged(string name) => PropertyChanged?.Invoke(this, new(name));
}

public sealed class FPHabit
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string? Description { get; set; }
    public double TargetValue { get; set; }
    public string Unit { get; set; } = "";
    public string? Icon { get; set; }
    public string? Color { get; set; }
    public double CurrentValue { get; set; }
    public int Streak { get; set; }

    public double Progress => TargetValue > 0 ? Math.Min(CurrentValue / TargetValue, 1.0) : 0;
    public string ProgressText => $"{CurrentValue:0.#} / {TargetValue:0.#}{Unit}";
}

public sealed class FPHabitLog
{
    public string Id { get; set; } = "";
    public string HabitId { get; set; } = "";
    public string UserId { get; set; } = "";
    public DateTime Date { get; set; }
    public double Value { get; set; }
}

public sealed class FPMeasurement
{
    public string Id { get; set; } = "";
    public string Name { get; set; } = "";
    public string Unit { get; set; } = "";
    public double Value { get; set; }
    public DateTime Date { get; set; }
    public string? Notes { get; set; }
}

public sealed class FPContent
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string? Body { get; set; }
    public string? ImageUrl { get; set; }
    public string Type { get; set; } = "article";
    public DateTime? DateCreated { get; set; }
}

public sealed class FPPersonalRecord
{
    public string Id { get; set; } = "";
    public string ExerciseId { get; set; } = "";
    public string ExerciseName { get; set; } = "";
    public string Metric { get; set; } = "";
    public string Value { get; set; } = "";
    public DateTime Date { get; set; }
    public string? PreviousValue { get; set; }
}

public sealed class FPForm
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string? Description { get; set; }
    public bool IsCompleted { get; set; }
    public DateTime? DueDate { get; set; }
}

public sealed class FPAssignedProgram
{
    public string Id { get; set; } = "";
    public string ProgramId { get; set; } = "";
    public string UserId { get; set; } = "";
    public string? CoachId { get; set; }
    public FPProgram? Program { get; set; }
    public DateTime? StartDate { get; set; }
    public int CurrentWeek { get; set; } = 1;
    public int CompletedWorkouts { get; set; }
}

public enum AppTab
{
    Train,
    Programs,
    Learn,
    History
}

public sealed class NavItem
{
    public AppTab Tab { get; init; }
    public string Label { get; init; } = "";
    public string Icon { get; init; } = "";
}