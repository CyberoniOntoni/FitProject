using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;
using FitProjectWin.Models;

namespace FitProjectWin.Services;

public sealed class FirestoreService
{
    private readonly HttpClient _http;
    private readonly string _token;

    public FirestoreService(string token)
    {
        _token = token;
        _http = new HttpClient();
        _http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    public async Task<FPUser> FetchUserProfileAsync(string userId)
    {
        var doc = await GetDocumentAsync($"users/{userId}");
        var fields = doc.GetProperty("fields");
        return new FPUser
        {
            Id = userId,
            Email = FirestoreParser.GetString(fields, "email") ?? "",
            FirstName = FirestoreParser.GetString(fields, "firstName") ?? "",
            LastName = FirestoreParser.GetString(fields, "lastName") ?? "",
            ProfilePictureUrl = FirestoreParser.GetString(fields, "profilePictureUrl"),
            Timezone = FirestoreParser.GetString(fields, "timezone"),
            CoachHasProTools = FirestoreParser.GetBool(fields, "coachHasProTools")
        };
    }

    public async Task<List<FPProgram>> FetchCreatorProgramsAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "programs" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "creatorIds" },
                        op = "ARRAY_CONTAINS",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results
            .Where(r => r.TryGetProperty("document", out _))
            .Select(r => FirestoreParser.ParseProgram(r.GetProperty("document")))
            .ToList();
    }

    public async Task<List<FPProgram>> FetchAssignedProgramsAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "programs" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "userIds" },
                        op = "ARRAY_CONTAINS",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results
            .Where(r => r.TryGetProperty("document", out _))
            .Select(r => FirestoreParser.ParseProgram(r.GetProperty("document")))
            .ToList();
    }

    public async Task<List<FPProgramWeek>> FetchProgramWeeksAsync(string programId)
    {
        var url = $"{FirebaseConfig.FirestoreBaseUrl}/programs/{programId}/programWeeks?pageSize=20";
        var json = await _http.GetFromJsonAsync<JsonElement>(url);
        if (!json.TryGetProperty("documents", out var docs)) return [];

        var weeks = new List<FPProgramWeek>();
        foreach (var doc in docs.EnumerateArray())
        {
            var fields = doc.GetProperty("fields");
            var weekId = doc.GetProperty("name").GetString()!.Split('/').Last();
            var week = new FPProgramWeek
            {
                Id = weekId,
                Name = FirestoreParser.GetString(fields, "name") ?? "Week",
                Index = FirestoreParser.GetInt(fields, "index"),
                ProgramId = programId
            };
            week.Workouts = await FetchWorkoutsAsync(programId, weekId);
            weeks.Add(week);
        }
        return weeks.OrderBy(w => w.Index).ToList();
    }

    public async Task<List<FPWorkout>> FetchWorkoutsAsync(string programId, string weekId)
    {
        var url = $"{FirebaseConfig.FirestoreBaseUrl}/programs/{programId}/programWeeks/{weekId}/workouts?pageSize=20";
        var json = await _http.GetFromJsonAsync<JsonElement>(url);
        if (!json.TryGetProperty("documents", out var docs)) return [];

        return docs.EnumerateArray()
            .Select(d => FirestoreParser.ParseWorkout(d, programId, weekId))
            .OrderBy(w => w.Index)
            .ToList();
    }

    public async Task<List<FPWorkoutLog>> FetchWorkoutLogsAsync(string userId, int limit = 50)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "workoutLogs" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "userId" },
                        op = "EQUAL",
                        value = new { stringValue = userId }
                    }
                },
                orderBy = new[] { new { field = new { fieldPath = "startedAt" }, direction = "DESCENDING" } },
                limit
            }
        });

        return results
            .Where(r => r.TryGetProperty("document", out _))
            .Select(r => FirestoreParser.ParseWorkoutLog(r.GetProperty("document")))
            .Where(l => l is not null)
            .Select(l => l!)
            .ToList();
    }

    public async Task SaveWorkoutLogAsync(FPWorkoutLog log)
    {
        var exercises = log.Exercises.Select(e => new Dictionary<string, object?>
        {
            ["id"] = e.Id,
            ["exerciseId"] = e.ExerciseId,
            ["name"] = e.Name,
            ["sets"] = e.Sets.Select(s =>
            {
                var d = new Dictionary<string, object?>
                {
                    ["id"] = s.Id,
                    ["setNumber"] = s.SetNumber,
                    ["isCompleted"] = s.IsCompleted,
                    ["isPR"] = s.IsPr
                };
                if (s.Reps is not null) d["reps"] = s.Reps;
                if (s.Weight is not null) d["weight"] = s.Weight;
                if (s.Rpe is not null) d["rpe"] = s.Rpe;
                if (s.Rest is not null) d["rest"] = s.Rest;
                if (s.Tempo is not null) d["tempo"] = s.Tempo;
                if (s.Time is not null) d["time"] = s.Time;
                return d;
            }).ToList()
        }).ToList();

        var data = new Dictionary<string, object?>
        {
            ["userId"] = log.UserId,
            ["workoutId"] = log.WorkoutId,
            ["workoutName"] = log.WorkoutName,
            ["startedAt"] = log.StartedAt,
            ["totalVolume"] = log.TotalVolume,
            ["prCount"] = log.PrCount,
            ["exercises"] = exercises
        };
        if (log.ProgramId is not null) data["programId"] = log.ProgramId;
        if (log.CompletedAt.HasValue) data["completedAt"] = log.CompletedAt.Value;
        if (log.DurationSeconds.HasValue) data["durationSeconds"] = log.DurationSeconds.Value;
        if (log.Notes is not null) data["notes"] = log.Notes;

        await PatchDocumentAsync($"workoutLogs/{log.Id}", data);
    }

    public async Task<List<FPHabit>> FetchHabitsAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "habits" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "userId" },
                        op = "EQUAL",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results.Where(r => r.TryGetProperty("document", out var d)).Select(r =>
        {
            var doc = r.GetProperty("document");
            var fields = doc.GetProperty("fields");
            return new FPHabit
            {
                Id = doc.GetProperty("name").GetString()!.Split('/').Last(),
                Name = FirestoreParser.GetString(fields, "name") ?? "",
                Description = FirestoreParser.GetString(fields, "description"),
                TargetValue = FirestoreParser.GetDouble(fields, "targetValue"),
                Unit = FirestoreParser.GetString(fields, "unit") ?? "",
                CurrentValue = FirestoreParser.GetDouble(fields, "currentValue"),
                Streak = FirestoreParser.GetInt(fields, "streak")
            };
        }).ToList();
    }

    public async Task UpdateHabitValueAsync(string habitId, double value)
    {
        await PatchDocumentAsync($"habits/{habitId}", new Dictionary<string, object?>
        {
            ["currentValue"] = value
        });
    }

    public async Task SaveHabitLogAsync(FPHabitLog log)
    {
        await PatchDocumentAsync($"habitLogs/{log.Id}", new Dictionary<string, object?>
        {
            ["habitId"] = log.HabitId,
            ["userId"] = log.UserId,
            ["date"] = log.Date,
            ["value"] = log.Value
        });
    }

    public async Task<List<FPMeasurement>> FetchMeasurementsAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "measurements" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "userId" },
                        op = "EQUAL",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results.Where(r => r.TryGetProperty("document", out _)).Select(r =>
        {
            var doc = r.GetProperty("document");
            var fields = doc.GetProperty("fields");
            return new FPMeasurement
            {
                Id = doc.GetProperty("name").GetString()!.Split('/').Last(),
                Name = FirestoreParser.GetString(fields, "name") ?? "",
                Unit = FirestoreParser.GetString(fields, "unit") ?? "",
                Value = FirestoreParser.GetDouble(fields, "value"),
                Date = FirestoreParser.GetTimestamp(fields, "date") ?? DateTime.UtcNow,
                Notes = FirestoreParser.GetString(fields, "notes")
            };
        }).OrderByDescending(m => m.Date).ToList();
    }

    public async Task SaveMeasurementAsync(FPMeasurement m, string userId)
    {
        await PatchDocumentAsync($"measurements/{m.Id}", new Dictionary<string, object?>
        {
            ["userId"] = userId,
            ["name"] = m.Name,
            ["unit"] = m.Unit,
            ["value"] = m.Value,
            ["date"] = m.Date,
            ["notes"] = m.Notes
        });
    }

    public async Task<List<FPContent>> FetchContentAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "content" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "userIds" },
                        op = "ARRAY_CONTAINS",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results.Where(r => r.TryGetProperty("document", out _)).Select(r =>
        {
            var doc = r.GetProperty("document");
            var fields = doc.GetProperty("fields");
            return new FPContent
            {
                Id = doc.GetProperty("name").GetString()!.Split('/').Last(),
                Title = FirestoreParser.GetString(fields, "title") ?? "",
                Body = FirestoreParser.GetString(fields, "body"),
                ImageUrl = FirestoreParser.GetString(fields, "imageUrl"),
                Type = FirestoreParser.GetString(fields, "type") ?? "article",
                DateCreated = FirestoreParser.GetTimestamp(fields, "dateCreated")
            };
        }).ToList();
    }

    public async Task<List<FPPersonalRecord>> FetchPersonalRecordsAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "personalRecords" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "userId" },
                        op = "EQUAL",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results.Where(r => r.TryGetProperty("document", out _)).Select(r =>
        {
            var doc = r.GetProperty("document");
            var fields = doc.GetProperty("fields");
            return new FPPersonalRecord
            {
                Id = doc.GetProperty("name").GetString()!.Split('/').Last(),
                ExerciseId = FirestoreParser.GetString(fields, "exerciseId") ?? "",
                ExerciseName = FirestoreParser.GetString(fields, "exerciseName") ?? "",
                Metric = FirestoreParser.GetString(fields, "metric") ?? "",
                Value = FirestoreParser.GetString(fields, "value") ?? "",
                Date = FirestoreParser.GetTimestamp(fields, "date") ?? DateTime.UtcNow,
                PreviousValue = FirestoreParser.GetString(fields, "previousValue")
            };
        }).OrderByDescending(r => r.Date).ToList();
    }

    public async Task SavePersonalRecordAsync(FPPersonalRecord record, string userId)
    {
        await PatchDocumentAsync($"personalRecords/{record.Id}", new Dictionary<string, object?>
        {
            ["userId"] = userId,
            ["exerciseId"] = record.ExerciseId,
            ["exerciseName"] = record.ExerciseName,
            ["metric"] = record.Metric,
            ["value"] = record.Value,
            ["date"] = record.Date,
            ["previousValue"] = record.PreviousValue
        });
    }

    public async Task<List<FPForm>> FetchFormsAsync(string userId)
    {
        var results = await RunQueryAsync(new
        {
            structuredQuery = new
            {
                from = new[] { new { collectionId = "forms" } },
                where = new
                {
                    fieldFilter = new
                    {
                        field = new { fieldPath = "clientIds" },
                        op = "ARRAY_CONTAINS",
                        value = new { stringValue = userId }
                    }
                },
                limit = 50
            }
        });

        return results
            .Where(r => r.TryGetProperty("document", out _))
            .Select(r => FirestoreParser.ParseForm(r.GetProperty("document")))
            .ToList();
    }

    public async Task SubmitFormAsync(string formId, string clientId, List<FPFormAnswer> answers)
    {
        var doc = await GetDocumentAsync($"forms/{formId}");
        var fields = doc.GetProperty("fields");
        var existing = FirestoreParser.ParseFormSubmissions(fields);

        existing.Add(new FPFormSubmission
        {
            ClientId = clientId,
            SubmittedAt = DateTime.UtcNow,
            Answers = answers
        });

        var newResponses = FirestoreParser.GetInt(fields, "newResponses") + 1;
        var url = $"{FirebaseConfig.FirestoreBaseUrl}/forms/{formId}?updateMask.fieldPaths=submissions&updateMask.fieldPaths=newResponses";
        var body = new
        {
            fields = new Dictionary<string, object>
            {
                ["submissions"] = FirestoreParser.FirestoreSubmissionArray(existing),
                ["newResponses"] = FirestoreParser.FirestoreValue(newResponses)
            }
        };
        var response = await _http.PatchAsJsonAsync(url, body);
        response.EnsureSuccessStatusCode();
    }

    private async Task<JsonElement> GetDocumentAsync(string path)
    {
        var url = $"{FirebaseConfig.FirestoreBaseUrl}/{path}";
        var response = await _http.GetAsync(url);
        response.EnsureSuccessStatusCode();
        var doc = await response.Content.ReadFromJsonAsync<JsonElement>();
        if (doc.ValueKind is JsonValueKind.Undefined or JsonValueKind.Null)
            throw new InvalidOperationException($"Document not found: {path}");
        return doc;
    }

    private async Task PatchDocumentAsync(string path, Dictionary<string, object?> data)
    {
        var url = $"{FirebaseConfig.FirestoreBaseUrl}/{path}";
        var body = new { fields = FirestoreParser.FirestoreFields(data) };
        var response = await _http.PatchAsJsonAsync(url, body);
        response.EnsureSuccessStatusCode();
    }

    private async Task<List<JsonElement>> RunQueryAsync(object query)
    {
        var url = $"{FirebaseConfig.FirestoreBaseUrl}:runQuery";
        var response = await _http.PostAsJsonAsync(url, query);
        if (!response.IsSuccessStatusCode) return [];
        var json = await response.Content.ReadFromJsonAsync<JsonElement>();
        if (json.ValueKind != JsonValueKind.Array) return [];
        return json.EnumerateArray().ToList();
    }
}