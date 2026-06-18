namespace FitProjectWin.Helpers;

internal static class WorkoutMetricFormat
{
    private static readonly HashSet<string> HighlightedMetrics = ["Reps", "Rest", "RPE", "Tempo"];

    public static bool IsHighlighted(string name) => HighlightedMetrics.Contains(name);

    public static string FormatTempoDisplay(string? value)
    {
        if (string.IsNullOrWhiteSpace(value)) return "";
        var digits = new string(value.Where(char.IsDigit).ToArray());
        if (digits.Length == 0) return "";
        var normalized = digits.Length >= 3 ? digits[..3] : digits.PadLeft(3, '0');
        var trimmed = normalized.TrimStart('0');
        return trimmed.Length == 0 ? "0" : trimmed;
    }

    public static string SanitizeTempoInput(string input) =>
        new(input.Where(char.IsDigit).Take(3).ToArray());

    public static string SanitizeMetricInput(string name, string input) => name switch
    {
        "Tempo" => SanitizeTempoInput(input),
        "Reps" or "Rest" or "RPE" => new string(input.Where(c => char.IsDigit(c) || c == '.').Take(6).ToArray()),
        _ => input
    };
}