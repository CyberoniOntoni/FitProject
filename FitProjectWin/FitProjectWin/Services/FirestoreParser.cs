using System.Text.Json;
using FitProjectWin.Models;

namespace FitProjectWin.Services;

internal static class FirestoreParser
{
    public static string? GetString(JsonElement fields, string key) =>
        fields.TryGetProperty(key, out var f) && f.TryGetProperty("stringValue", out var v) ? v.GetString() : null;

    public static int GetInt(JsonElement fields, string key) =>
        fields.TryGetProperty(key, out var f) && f.TryGetProperty("integerValue", out var v) && int.TryParse(v.GetString(), out var i) ? i : 0;

    public static double GetDouble(JsonElement fields, string key) =>
        fields.TryGetProperty(key, out var f) && f.TryGetProperty("doubleValue", out var v) ? v.GetDouble() : 0;

    public static bool GetBool(JsonElement fields, string key) =>
        fields.TryGetProperty(key, out var f) && f.TryGetProperty("booleanValue", out var v) && v.GetBoolean();

    public static DateTime? GetTimestamp(JsonElement fields, string key)
    {
        var raw = GetString(fields, key);
        return raw is null ? null : DateTime.Parse(raw.Replace("Z", "+00:00"));
    }

    public static List<string> GetStringArray(JsonElement fields, string key)
    {
        if (!fields.TryGetProperty(key, out var f) || !f.TryGetProperty("arrayValue", out var arr)) return [];
        if (!arr.TryGetProperty("values", out var values)) return [];
        return values.EnumerateArray()
            .Select(v => v.TryGetProperty("stringValue", out var s) ? s.GetString() : null)
            .Where(s => s is not null)
            .Select(s => s!)
            .ToList();
    }

    public static List<Dictionary<string, object?>> GetMapArray(JsonElement fields, string key)
    {
        if (!fields.TryGetProperty(key, out var f) || !f.TryGetProperty("arrayValue", out var arr)) return [];
        if (!arr.TryGetProperty("values", out var values)) return [];
        var result = new List<Dictionary<string, object?>>();
        foreach (var item in values.EnumerateArray())
        {
            if (!item.TryGetProperty("mapValue", out var map) || !map.TryGetProperty("fields", out var mapFields)) continue;
            result.Add(ParseMapFields(mapFields));
        }
        return result;
    }

    public static Dictionary<string, object?> ParseMapFields(JsonElement fields)
    {
        var dict = new Dictionary<string, object?>();
        foreach (var prop in fields.EnumerateObject())
        {
            var el = prop.Value;
            if (el.TryGetProperty("stringValue", out var s)) dict[prop.Name] = s.GetString();
            else if (el.TryGetProperty("integerValue", out var i)) dict[prop.Name] = int.TryParse(i.GetString(), out var iv) ? iv : 0;
            else if (el.TryGetProperty("doubleValue", out var d)) dict[prop.Name] = d.GetDouble();
            else if (el.TryGetProperty("booleanValue", out var b)) dict[prop.Name] = b.GetBoolean();
        }
        return dict;
    }

    public static FPProgram ParseProgram(JsonElement doc)
    {
        var fields = doc.GetProperty("fields");
        var name = doc.GetProperty("name").GetString() ?? "";
        var id = name.Split('/').Last();
        return new FPProgram
        {
            Id = id,
            Title = GetString(fields, "title") ?? "",
            Description = GetString(fields, "description") ?? "",
            ImageUrl = GetString(fields, "imageUrl"),
            TotalWeekCount = GetInt(fields, "totalWeekCount"),
            TotalWorkoutCount = GetInt(fields, "totalWorkoutCount"),
            Published = GetBool(fields, "published"),
            CreatorIds = GetStringArray(fields, "creatorIds")
        };
    }

    public static FPWorkout ParseWorkout(JsonElement doc, string programId, string weekId)
    {
        var fields = doc.GetProperty("fields");
        var name = doc.GetProperty("name").GetString() ?? "";
        var id = name.Split('/').Last();

        var groups = GetMapArray(fields, "workoutExerciseGroups").Select(g => new FPExerciseGroup
        {
            Id = g.GetValueOrDefault("id")?.ToString() ?? Guid.NewGuid().ToString(),
            Name = g.GetValueOrDefault("name")?.ToString() ?? "",
            Index = g.GetValueOrDefault("index") is int gi ? gi : 0,
            Type = g.GetValueOrDefault("type")?.ToString()
        }).ToList();

        var exercises = GetMapArray(fields, "workoutExercises").Select(e => new FPWorkoutExercise
        {
            Id = e.GetValueOrDefault("id")?.ToString() ?? Guid.NewGuid().ToString(),
            Name = e.GetValueOrDefault("name")?.ToString() ?? "",
            ExerciseId = e.GetValueOrDefault("exerciseId")?.ToString() ?? "",
            YoutubeId = e.GetValueOrDefault("youtubeId")?.ToString(),
            ThumbnailUrl = e.GetValueOrDefault("thumbnailUrl")?.ToString(),
            Index = e.GetValueOrDefault("index") is int ei ? ei : 0,
            Sets = e.GetValueOrDefault("sets") is int si ? si : 0,
            CoachNotes = e.GetValueOrDefault("coachNotes")?.ToString(),
            Header = e.GetValueOrDefault("header")?.ToString(),
            HeaderVisible = e.GetValueOrDefault("headerVisible") is bool hv && hv,
            GroupId = e.GetValueOrDefault("workoutExerciseGroupId")?.ToString(),
            IsSuperset = e.GetValueOrDefault("isSuperset") is bool ss && ss
        }).ToList();

        var metrics = GetMapArray(fields, "workoutExerciseMetrics").Select(m =>
        {
            var unit = m.GetValueOrDefault("unitOfMeasurement") as Dictionary<string, object?>;
            return new FPWorkoutMetric
            {
                Id = m.GetValueOrDefault("id")?.ToString() ?? Guid.NewGuid().ToString(),
                Name = m.GetValueOrDefault("name")?.ToString() ?? "",
                Index = m.GetValueOrDefault("index") is int mi ? mi : 0,
                Color = m.GetValueOrDefault("color")?.ToString() ?? "#ffffff",
                ExerciseMetricId = m.GetValueOrDefault("exerciseMetricId")?.ToString() ?? "",
                WorkoutExerciseId = m.GetValueOrDefault("workoutExerciseId")?.ToString() ?? "",
                UnitAbbreviation = unit?.GetValueOrDefault("abbreviation")?.ToString()
            };
        }).ToList();

        var values = GetMapArray(fields, "workoutExerciseMetricValues").Select(v => new FPMetricValue
        {
            Id = v.GetValueOrDefault("id")?.ToString() ?? Guid.NewGuid().ToString(),
            WorkoutExerciseId = v.GetValueOrDefault("workoutExerciseId")?.ToString() ?? "",
            WorkoutExerciseMetricId = v.GetValueOrDefault("workoutExerciseMetricId")?.ToString() ?? "",
            ExerciseMetricId = v.GetValueOrDefault("exerciseMetricId")?.ToString() ?? "",
            ExerciseId = v.GetValueOrDefault("exerciseId")?.ToString() ?? "",
            Set = v.GetValueOrDefault("set") is int sv ? sv : 1,
            Index = v.GetValueOrDefault("index") is int iv ? iv : 0,
            Value = v.GetValueOrDefault("value")?.ToString() ?? "",
            LoggedValue = v.GetValueOrDefault("loggedValue")?.ToString(),
            IsCompleted = v.GetValueOrDefault("isCompleted") is bool cv && cv
        }).ToList();

        return new FPWorkout
        {
            Id = id,
            Name = GetString(fields, "name") ?? "",
            Description = GetString(fields, "description"),
            Index = GetInt(fields, "index"),
            ProgramId = programId,
            ProgramWeekId = weekId,
            Notes = GetString(fields, "notes"),
            ExerciseGroups = groups,
            Exercises = exercises,
            Metrics = metrics,
            MetricValues = values
        };
    }

    public static FPWorkoutLog? ParseWorkoutLog(JsonElement doc)
    {
        var fields = doc.GetProperty("fields");
        var startedAt = GetTimestamp(fields, "startedAt");
        if (!startedAt.HasValue) return null;

        var name = doc.GetProperty("name").GetString() ?? "";
        var exercises = new List<FPLoggedExercise>();
        if (fields.TryGetProperty("exercises", out var exArr) && exArr.TryGetProperty("arrayValue", out var av))
        {
            foreach (var item in av.GetProperty("values").EnumerateArray())
            {
                if (!item.TryGetProperty("mapValue", out var mv) || !mv.TryGetProperty("fields", out var ef)) continue;
                var sets = new List<FPLoggedSet>();
                if (ef.TryGetProperty("sets", out var setsArr) && setsArr.TryGetProperty("arrayValue", out var sav))
                {
                    foreach (var setItem in sav.GetProperty("values").EnumerateArray())
                    {
                        if (!setItem.TryGetProperty("mapValue", out var smv) || !smv.TryGetProperty("fields", out var sf)) continue;
                        sets.Add(new FPLoggedSet
                        {
                            Id = GetString(sf, "id") ?? Guid.NewGuid().ToString(),
                            SetNumber = GetInt(sf, "setNumber"),
                            Reps = GetString(sf, "reps"),
                            Weight = GetString(sf, "weight"),
                            Rpe = GetString(sf, "rpe"),
                            Rest = GetString(sf, "rest"),
                            Tempo = GetString(sf, "tempo"),
                            Time = GetString(sf, "time"),
                            IsCompleted = GetBool(sf, "isCompleted"),
                            IsPr = GetBool(sf, "isPr")
                        });
                    }
                }
                exercises.Add(new FPLoggedExercise
                {
                    Id = GetString(ef, "id") ?? Guid.NewGuid().ToString(),
                    ExerciseId = GetString(ef, "exerciseId") ?? "",
                    Name = GetString(ef, "name") ?? "",
                    Sets = sets
                });
            }
        }

        return new FPWorkoutLog
        {
            Id = name.Split('/').Last(),
            UserId = GetString(fields, "userId") ?? "",
            WorkoutId = GetString(fields, "workoutId") ?? "",
            ProgramId = GetString(fields, "programId"),
            WorkoutName = GetString(fields, "workoutName") ?? "",
            StartedAt = startedAt.Value,
            CompletedAt = GetTimestamp(fields, "completedAt"),
            DurationSeconds = GetInt(fields, "durationSeconds"),
            Exercises = exercises,
            Notes = GetString(fields, "notes"),
            TotalVolume = GetDouble(fields, "totalVolume"),
            PrCount = GetInt(fields, "prCount")
        };
    }

    public static object FirestoreValue(object? value) => value switch
    {
        null => new { nullValue = (object?)null },
        string s => new { stringValue = s },
        int i => new { integerValue = i.ToString() },
        long l => new { integerValue = l.ToString() },
        double d => new { doubleValue = d },
        bool b => new { booleanValue = b },
        DateTime dt => new { timestampValue = dt.ToUniversalTime().ToString("yyyy-MM-dd'T'HH:mm:ss.fff'Z'") },
        _ => new { stringValue = value.ToString() }
    };

    public static Dictionary<string, object> FirestoreFields(Dictionary<string, object?> data)
    {
        var fields = new Dictionary<string, object>();
        foreach (var (key, val) in data)
            if (val is not null) fields[key] = FirestoreValue(val);
        return fields;
    }
}