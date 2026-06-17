namespace FitProjectWin.Models;

public sealed record MeasurementTypeDef(
    string Id,
    string Name,
    string Category,
    string Color,
    string UnitId,
    string UnitName,
    string UnitType,
    string UnitAbbreviation)
{
    public string DisplayUnit => UnitAbbreviation switch
    {
        "centimeter" => "cm",
        "inches" => "in",
        _ => UnitAbbreviation
    };
}

public static class MeasurementCatalog
{
    public static readonly MeasurementTypeDef[] Types =
    [
        // Body composition
        new("DfqsrFQBGi04aHWAPA7I", "Bodyweight", "Body Composition", "#c93477", "kg", "Kilograms", "MASS", "kg"),
        new("body_fat_percentage", "Body Fat %", "Body Composition", "#FDCB6E", "%", "PERCENT", "PERCENT", "%"),
        new("muscle_mass", "Muscle Mass", "Body Composition", "#E17055", "kg", "MASS", "MASS", "kg"),
        new("body_water_percentage", "Body Water %", "Body Composition", "#06b6d4", "%", "PERCENT", "PERCENT", "%"),
        new("bone_mass", "Bone Mass", "Body Composition", "#94a3b8", "bone_mass_unit", "kg", "MASS", "kg"),
        new("vo2_max", "VO2 Max", "Body Composition", "#7c3aed", "vo2_max_unit", "ml/kg/min", "NUMERIC", "ml/kg/min"),
        new("visceral_fat", "Visceral Fat", "Body Composition", "#f97316", "level", "NUMERIC", "NUMERIC", "level"),
        // Circumference
        new("waist", "Waist", "Circumference", "#4ECDC4", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        new("chest", "Chest", "Circumference", "#10b981", "chest_unit", "Circumference", "CIRCUMFERENCE", "cm"),
        new("arms", "Arms", "Circumference", "#FFA07A", "inches", "CIRCUMFERENCE", "CIRCUMFERENCE", "in"),
        new("thighs", "Thighs", "Circumference", "#8b5cf6", "thighs_unit", "cm", "CIRCUMFERENCE", "cm"),
        new("hips", "Hips", "Circumference", "#45B7D1", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        new("neck", "Neck", "Circumference", "#06b6d4", "neck_unit", "Circumference", "CIRCUMFERENCE", "in"),
        new("shoulders", "Shoulders", "Circumference", "#FD79A8", "inches", "CIRCUMFERENCE", "CIRCUMFERENCE", "in"),
        new("calves", "Calves", "Circumference", "#00B894", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
        new("forearms", "Forearms", "Circumference", "#74B9FF", "inches", "CIRCUMFERENCE", "CIRCUMFERENCE", "in"),
        new("wrist", "Wrist", "Circumference", "#a855f7", "wrist_unit", "cm", "CIRCUMFERENCE", "cm"),
        new("ankle", "Ankle", "Circumference", "#fbbf24", "cm", "CIRCUMFERENCE", "CIRCUMFERENCE", "cm"),
    ];

    public static readonly string[] Categories = ["Body Composition", "Circumference"];

    public static MeasurementTypeDef? FindById(string? id) =>
        string.IsNullOrEmpty(id) ? null : Types.FirstOrDefault(t => t.Id == id);

    public static MeasurementTypeDef? FindByName(string name)
    {
        var exact = Types.FirstOrDefault(t => t.Name.Equals(name, StringComparison.OrdinalIgnoreCase));
        if (exact is not null) return exact;

        return name.ToLowerInvariant() switch
        {
            "weight" or "body weight" => FindById("DfqsrFQBGi04aHWAPA7I"),
            "body fat" or "bodyfat" => FindById("body_fat_percentage"),
            _ => Types.FirstOrDefault(t => t.Name.Contains(name, StringComparison.OrdinalIgnoreCase))
        };
    }

    public static IEnumerable<MeasurementTypeDef> InCategory(string category) =>
        Types.Where(t => t.Category == category);
}