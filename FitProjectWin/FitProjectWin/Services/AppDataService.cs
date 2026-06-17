using FitProjectWin.Models;

namespace FitProjectWin.Services;

public sealed class AppDataService
{
    private readonly AuthService _auth;

    public List<FPProgram> Programs { get; private set; } = [];
    public Dictionary<string, List<FPProgramWeek>> ProgramWeeks { get; private set; } = new();
    public List<FPWorkoutLog> WorkoutLogs { get; private set; } = [];
    public List<FPHabit> Habits { get; private set; } = [];
    public List<FPMeasurement> Measurements { get; private set; } = [];
    public List<FPContent> Content { get; private set; } = [];
    public List<FPPersonalRecord> PersonalRecords { get; private set; } = [];
    public List<FPForm> Forms { get; private set; } = [];

    public FPWorkout? NextWorkout { get; private set; }
    public FPProgram? NextProgram { get; private set; }
    public int WeeklyWorkoutsCompleted { get; private set; }
    public int WeeklyWorkoutGoal { get; set; } = 4;
    public DateTime? LastSyncDate { get; private set; }
    public bool IsSyncing { get; private set; }
    public string? SyncError { get; private set; }

    public event Action? DataChanged;

    public AppDataService(AuthService auth) => _auth = auth;

    public async Task FullSyncAsync()
    {
        if (_auth.CurrentUser is null || _auth.IdToken is null) return;

        IsSyncing = true;
        SyncError = null;
        DataChanged?.Invoke();

        try
        {
            var fs = new FirestoreService(_auth.IdToken);
            var userId = _auth.CurrentUser.Id;

            var creatorPrograms = await fs.FetchCreatorProgramsAsync(userId);
            var assignedPrograms = await fs.FetchAssignedProgramsAsync(userId);
            var programs = creatorPrograms.ToList();
            foreach (var assigned in assignedPrograms)
                if (!programs.Any(p => p.Id == assigned.Id))
                    programs.Add(assigned);

            var logs = await fs.FetchWorkoutLogsAsync(userId);
            var habits = await fs.FetchHabitsAsync(userId);
            var measurements = await fs.FetchAllMeasurementsAsync(userId);
            var content = await fs.FetchContentAsync(userId);
            var records = await fs.FetchPersonalRecordsAsync(userId);
            var forms = await fs.FetchFormsAsync(userId);

            var weeks = new Dictionary<string, List<FPProgramWeek>>();
            foreach (var program in programs)
                weeks[program.Id] = await fs.FetchProgramWeeksAsync(program.Id);

            Programs = programs;
            ProgramWeeks = weeks;
            WorkoutLogs = logs;
            Habits = habits;
            Measurements = measurements;
            Content = content;
            PersonalRecords = records;
            Forms = forms;
            LastSyncDate = DateTime.Now;

            UpdateWeeklyProgress();
            SelectNextWorkout();
        }
        catch (Exception ex)
        {
            SyncError = ex.Message;
        }
        finally
        {
            IsSyncing = false;
            DataChanged?.Invoke();
        }
    }

    public void UpdateWeeklyProgress()
    {
        var startOfWeek = DateTime.Today.AddDays(-(int)DateTime.Today.DayOfWeek);
        WeeklyWorkoutsCompleted = WorkoutLogs.Count(l =>
            l.IsCompleted && l.CompletedAt >= startOfWeek);
    }

    public void SelectNextWorkout()
    {
        var completedIds = WorkoutLogs.Where(l => l.IsCompleted).Select(l => l.WorkoutId).ToHashSet();

        foreach (var program in Programs)
        {
            if (!ProgramWeeks.TryGetValue(program.Id, out var weeks)) continue;
            foreach (var week in weeks.OrderBy(w => w.Index))
            {
                foreach (var workout in week.Workouts.OrderBy(w => w.Index))
                {
                    if (!completedIds.Contains(workout.Id))
                    {
                        NextWorkout = workout;
                        NextProgram = program;
                        return;
                    }
                }
            }
        }
        NextWorkout = null;
        NextProgram = Programs.FirstOrDefault();
    }

    public Dictionary<string, List<FPLoggedSet>> BuildLoggedSets(FPWorkout workout)
    {
        var result = new Dictionary<string, List<FPLoggedSet>>();
        foreach (var exercise in workout.Exercises)
        {
            var exerciseMetrics = workout.Metrics.Where(m => m.WorkoutExerciseId == exercise.Id).ToList();
            var setCount = exercise.Sets > 0 ? exercise.Sets : 3;
            var sets = new List<FPLoggedSet>();

            for (var i = 1; i <= setCount; i++)
            {
                var set = new FPLoggedSet { SetNumber = i };
                foreach (var metric in exerciseMetrics)
                {
                    var preset = workout.MetricValues.FirstOrDefault(v =>
                        v.WorkoutExerciseId == exercise.Id &&
                        v.WorkoutExerciseMetricId == metric.Id &&
                        v.Set == i);

                    if (preset is null) continue;
                    switch (metric.Name)
                    {
                        case "Reps": set.Reps = preset.Value; break;
                        case "Weight": set.Weight = preset.Value; break;
                        case "RPE": set.Rpe = preset.Value; break;
                        case "Rest": set.Rest = preset.Value; break;
                        case "Tempo": set.Tempo = preset.Value; break;
                        case "Time": set.Time = preset.Value; break;
                    }
                }
                sets.Add(set);
            }
            result[exercise.Id] = sets;
        }
        return result;
    }

    public FPPersonalRecord? CheckForPR(FPWorkoutExercise exercise, FPLoggedSet set)
    {
        if (!set.IsCompleted || !double.TryParse(set.Weight, out var weight)) return null;

        var existing = PersonalRecords.FirstOrDefault(r =>
            r.ExerciseId == exercise.ExerciseId && r.Metric == "Weight");

        if (existing is not null && double.TryParse(existing.Value, out var prev) && weight <= prev)
            return null;

        return new FPPersonalRecord
        {
            Id = Guid.NewGuid().ToString(),
            ExerciseId = exercise.ExerciseId,
            ExerciseName = exercise.Name,
            Metric = "Weight",
            Value = set.Weight!,
            Date = DateTime.UtcNow,
            PreviousValue = existing?.Value
        };
    }

    public async Task CompleteWorkoutAsync(
        FPWorkout workout,
        FPProgram? program,
        Dictionary<string, List<FPLoggedSet>> loggedSets,
        string notes,
        DateTime startedAt,
        int elapsedSeconds,
        List<FPPersonalRecord> prs)
    {
        if (_auth.CurrentUser is null || _auth.IdToken is null) return;

        var endDate = DateTime.UtcNow;
        double totalVolume = 0;
        var prCount = 0;
        var loggedExercises = new List<FPLoggedExercise>();

        foreach (var exercise in workout.Exercises)
        {
            if (!loggedSets.TryGetValue(exercise.Id, out var sets)) continue;
            foreach (var set in sets.Where(s => s.IsCompleted))
            {
                if (double.TryParse(set.Weight, out var w) && double.TryParse(set.Reps, out var r))
                    totalVolume += w * r;
                if (set.IsPr) prCount++;
            }
            loggedExercises.Add(new FPLoggedExercise
            {
                Id = exercise.Id,
                ExerciseId = exercise.ExerciseId,
                Name = exercise.Name,
                Sets = sets
            });
        }

        var log = new FPWorkoutLog
        {
            Id = Guid.NewGuid().ToString(),
            UserId = _auth.CurrentUser.Id,
            WorkoutId = workout.Id,
            ProgramId = program?.Id,
            WorkoutName = workout.Name,
            StartedAt = startedAt,
            CompletedAt = endDate,
            DurationSeconds = elapsedSeconds,
            Exercises = loggedExercises,
            Notes = string.IsNullOrWhiteSpace(notes) ? null : notes,
            TotalVolume = totalVolume,
            PrCount = prCount
        };

        var fs = new FirestoreService(_auth.IdToken);
        await fs.SaveWorkoutLogAsync(log);
        foreach (var pr in prs)
            await fs.SavePersonalRecordAsync(pr, _auth.CurrentUser.Id);

        WorkoutLogs.Insert(0, log);
        PersonalRecords.InsertRange(0, prs);
        UpdateWeeklyProgress();
        SelectNextWorkout();
        DataChanged?.Invoke();
    }

    public async Task UpdateHabitAsync(string habitId, double value)
    {
        if (_auth.IdToken is null || _auth.CurrentUser is null) return;
        var fs = new FirestoreService(_auth.IdToken);
        await fs.UpdateHabitValueAsync(habitId, value);
        await fs.SaveHabitLogAsync(new FPHabitLog
        {
            Id = Guid.NewGuid().ToString(),
            HabitId = habitId,
            UserId = _auth.CurrentUser.Id,
            Date = DateTime.UtcNow,
            Value = value
        });
        var habit = Habits.FirstOrDefault(h => h.Id == habitId);
        if (habit is not null) habit.CurrentValue = value;
        DataChanged?.Invoke();
    }

    public async Task SaveMeasurementAsync(FPMeasurement measurement)
    {
        if (_auth.CurrentUser is null || _auth.IdToken is null) return;
        var fs = new FirestoreService(_auth.IdToken);
        await fs.SaveMeasurementAsync(measurement, _auth.CurrentUser.Id);
        Measurements.Insert(0, measurement);
        DataChanged?.Invoke();
    }

    public async Task SubmitFormAsync(string formId, List<FPFormAnswer> answers)
    {
        if (_auth.CurrentUser is null || _auth.IdToken is null) return;
        var fs = new FirestoreService(_auth.IdToken);
        await fs.SubmitFormAsync(formId, _auth.CurrentUser.Id, answers);

        var form = Forms.FirstOrDefault(f => f.Id == formId);
        if (form is not null)
        {
            form.Submissions.Add(new FPFormSubmission
            {
                ClientId = _auth.CurrentUser.Id,
                SubmittedAt = DateTime.UtcNow,
                Answers = answers
            });
            form.NewResponses++;
        }
        DataChanged?.Invoke();
    }

    public void Clear()
    {
        Programs = [];
        ProgramWeeks = new();
        WorkoutLogs = [];
        Habits = [];
        Measurements = [];
        Content = [];
        PersonalRecords = [];
        Forms = [];
        NextWorkout = null;
        NextProgram = null;
        DataChanged?.Invoke();
    }
}