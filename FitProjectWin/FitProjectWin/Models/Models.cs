using System.Text.Json;

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
    public FPUnitPreferences UnitPreferences { get; set; } = new();

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
    public string HabitId { get; set; } = "";
    public string Name { get; set; } = "";
    public string? Description { get; set; }
    public string Unit { get; set; } = "";
    public string Frequency { get; set; } = "Per Day";
    public string TargetType { get; set; } = "FLOOR";
    public double TargetMin { get; set; }
    public double TargetMax { get; set; }
    public string? Icon { get; set; }
    public string? Color { get; set; }
    public int Index { get; set; }
    public string CoachId { get; set; } = "";
    public double CurrentValue { get; set; }
    public bool TargetMet { get; set; }
    public DateTime? LogDateCreated { get; set; }

    public double TargetValue => TargetType == "RANGE" ? TargetMax : TargetMin;

    public bool IsComplete => HabitSyncHelper.IsTargetMet(TargetType, TargetMin, TargetMax, CurrentValue);

    public double Progress => HabitSyncHelper.Progress(TargetType, TargetMin, TargetMax, CurrentValue);

    public string ProgressText => HabitSyncHelper.ProgressText(TargetType, TargetMin, TargetMax, CurrentValue, Unit);
}

public sealed class FPHabitLog
{
    public string Id { get; set; } = "";
    public string UserHabitId { get; set; } = "";
    public string UserId { get; set; } = "";
    public DateTime Date { get; set; }
    public double Value { get; set; }
    public bool TargetMet { get; set; }
    public DateTime? DateCreated { get; set; }
}

public static class HabitSyncHelper
{
    public static string TargetTypeFromValues(IReadOnlyList<double> values, string? explicitType = null)
    {
        if (!string.IsNullOrEmpty(explicitType)) return explicitType;
        return values.Count >= 2 ? "RANGE" : "FLOOR";
    }

    public static bool IsTargetMet(string targetType, double min, double max, double value) => targetType switch
    {
        "CEILING" => value <= min,
        "RANGE" => value >= min && value <= max,
        _ => value >= min
    };

    public static double Progress(string targetType, double min, double max, double value)
    {
        if (IsTargetMet(targetType, min, max, value)) return 1;
        return targetType switch
        {
            "CEILING" => min > 0 ? Math.Min(value / min, 1) : 0,
            "RANGE" when value < min && min > 0 => value / min,
            "RANGE" when value > max && value > 0 => max / value,
            _ => min > 0 ? Math.Min(value / min, 1) : 0
        };
    }

    public static string ProgressText(string targetType, double min, double max, double value, string unit)
    {
        var suffix = string.IsNullOrEmpty(unit) ? "" : $" {unit}";
        var current = value % 1 == 0 ? value.ToString("0") : value.ToString("0.#");
        var target = targetType == "RANGE"
            ? $"{min:0.#}-{max:0.#}"
            : (min % 1 == 0 ? min.ToString("0") : min.ToString("0.#"));
        return $"{current} / {target}{suffix}";
    }

    public static long StartOfDayUnix(DateTime date)
    {
        var local = date.Date;
        return new DateTimeOffset(local).ToUnixTimeSeconds();
    }
}

public sealed class FPProgressPicture
{
    public string Id { get; set; } = "";
    public string UserId { get; set; } = "";
    public string SessionId { get; set; } = "";
    public string PoseType { get; set; } = "front";
    public string ImageUrl { get; set; } = "";
    public DateTime DateCreated { get; set; }
    public string? Notes { get; set; }
}

public sealed class FPProgressSession
{
    public string SessionId { get; set; } = "";
    public DateTime DateCreated { get; set; }
    public List<FPProgressPicture> Pictures { get; set; } = [];
    public string? Notes { get; set; }
}

public sealed class FPUnitPreferences
{
    public string Weight { get; set; } = "KILOGRAM";
    public string Mass { get; set; } = "KILOGRAM";
    public string Circumference { get; set; } = "CENTIMETER";
    public string Distance { get; set; } = "MILE";
    public string Time { get; set; } = "SECOND";

    public static FPUnitPreferences FromFirestore(JsonElement? fields)
    {
        if (fields is null || !fields.Value.TryGetProperty("mapValue", out var map) ||
            !map.TryGetProperty("fields", out var ff))
            return new();

        string Read(string key) =>
            ff.TryGetProperty(key, out var v) && v.TryGetProperty("stringValue", out var s)
                ? s.GetString() ?? ""
                : "";

        return new FPUnitPreferences
        {
            Weight = NullIfEmpty(Read("weight")) ?? "KILOGRAM",
            Mass = NullIfEmpty(Read("mass")) ?? "KILOGRAM",
            Circumference = NullIfEmpty(Read("circumference")) ?? "CENTIMETER",
            Distance = NullIfEmpty(Read("distance")) ?? "MILE",
            Time = NullIfEmpty(Read("time")) ?? "SECOND"
        };
    }

    private static string? NullIfEmpty(string value) =>
        string.IsNullOrWhiteSpace(value) ? null : value;
}

public sealed class FPProgressPhotoDraft
{
    public string SessionId { get; set; } = Guid.NewGuid().ToString();
    public DateTime DateCreated { get; set; } = DateTime.Today;
    public string Notes { get; set; } = "";
    public Dictionary<string, string> PoseImageUrls { get; } = new(StringComparer.OrdinalIgnoreCase);
    public Dictionary<string, string> PoseLocalPaths { get; } = new(StringComparer.OrdinalIgnoreCase);
}

public static class UnitConversionHelper
{
    public static string MassAbbreviation(string unit) => unit switch
    {
        "POUND" => "lb",
        _ => "kg"
    };

    public static string CircumferenceAbbreviation(string unit) => unit switch
    {
        "INCH" => "in",
        _ => "cm"
    };

    public static double ConvertMassForDisplay(double kg, string unit) =>
        unit == "POUND" ? Math.Round(kg * 2.20462, 2) : Math.Round(kg, 2);

    public static double ConvertCircumferenceForDisplay(double cm, string unit) =>
        unit == "INCH" ? Math.Round(cm / 2.54, 2) : Math.Round(cm, 2);

    public static double MassToCanonical(double value, string unit) =>
        unit == "POUND" ? value / 2.20462 : value;

    public static double CircumferenceToCanonical(double value, string unit) =>
        unit == "INCH" ? value * 2.54 : value;

    public static string FormatDisplayNumber(double value) =>
        value % 1 == 0 ? $"{value:0}" : $"{value:0.0}";

    public static string FormatMeasurementValue(FPMeasurement m, FPUnitPreferences prefs)
    {
        var type = MeasurementCatalog.FindById(m.TypeId) ?? MeasurementCatalog.FindByName(m.Name);
        if (type?.Category == "Circumference")
        {
            var display = ConvertCircumferenceForDisplay(m.Value, prefs.Circumference);
            return $"{FormatDisplayNumber(display)} {CircumferenceAbbreviation(prefs.Circumference)}";
        }
        if (type?.Category == "Body Composition" && type.UnitType == "MASS")
        {
            var display = ConvertMassForDisplay(m.Value, prefs.Mass);
            return $"{FormatDisplayNumber(display)} {MassAbbreviation(prefs.Mass)}";
        }
        return $"{FormatDisplayNumber(m.Value)} {m.Unit}";
    }
}

public sealed class FPMeasurement
{
    public string Id { get; set; } = "";
    public string? TypeId { get; set; }
    public string Name { get; set; } = "";
    public string Unit { get; set; } = "";
    public double Value { get; set; }
    public DateTime Date { get; set; }
    public string? Notes { get; set; }
    public string? SessionId { get; set; }
    public string Source { get; set; } = "measurementLogs";

    public string MatchKey => TypeId ?? Name.ToLowerInvariant();
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

public sealed class FPFormField
{
    public string Id { get; set; } = "";
    public string Type { get; set; } = "";
    public string Question { get; set; } = "";
    public bool Required { get; set; }
    public int Index { get; set; }
    public int MaxRating { get; set; } = 5;
    public int ScaleMin { get; set; } = 1;
    public int ScaleMax { get; set; } = 10;
    public string? ScaleMinLabel { get; set; }
    public string? ScaleMaxLabel { get; set; }
    public List<string> Options { get; set; } = [];
}

public sealed class FPFormAnswer
{
    public string FieldId { get; set; } = "";
    public string Question { get; set; } = "";
    public string Type { get; set; } = "";
    public string Value { get; set; } = "";
}

public sealed class FPFormSubmission
{
    public string ClientId { get; set; } = "";
    public DateTime SubmittedAt { get; set; }
    public List<FPFormAnswer> Answers { get; set; } = [];
}

public sealed class FPForm
{
    public string Id { get; set; } = "";
    public string Title { get; set; } = "";
    public string? Description { get; set; }
    public string? CreatorId { get; set; }
    public DateTime? DueDate { get; set; }
    public List<FPFormField> Fields { get; set; } = [];
    public List<FPFormSubmission> Submissions { get; set; } = [];
    public int NewResponses { get; set; }

    public bool IsCompletedFor(string userId) =>
        Submissions.Any(s => s.ClientId == userId);
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