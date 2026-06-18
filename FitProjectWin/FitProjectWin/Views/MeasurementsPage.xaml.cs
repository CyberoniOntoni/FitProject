using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class MeasurementsPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public MeasurementsPage()
    {
        InitializeComponent();
        foreach (var type in MeasurementCatalog.Types)
            TypeCombo.Items.Add(type.Name);
        TypeCombo.SelectedIndex = 0;
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void TypeCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (TypeCombo.SelectedIndex >= 0 && TypeCombo.SelectedIndex < MeasurementCatalog.Types.Length)
            UnitLabel.Text = $"Unit: {DisplayUnitFor(MeasurementCatalog.Types[TypeCombo.SelectedIndex])}";
    }

    private string DisplayUnitFor(MeasurementTypeDef type)
    {
        var prefs = ViewModel.Data.UnitPreferences;
        return type.Category switch
        {
            "Circumference" => UnitConversionHelper.CircumferenceAbbreviation(prefs.Circumference),
            "Body Composition" when type.UnitType == "MASS" => UnitConversionHelper.MassAbbreviation(prefs.Mass),
            _ => type.DisplayUnit
        };
    }

    private double DisplayValueFor(FPMeasurement? measurement, MeasurementTypeDef type)
    {
        if (measurement is null) return 0;
        var prefs = ViewModel.Data.UnitPreferences;
        if (type.Category == "Circumference")
            return UnitConversionHelper.ConvertCircumferenceForDisplay(measurement.Value, prefs.Circumference);
        if (type.Category == "Body Composition" && type.UnitType == "MASS")
            return UnitConversionHelper.ConvertMassForDisplay(measurement.Value, prefs.Mass);
        return measurement.Value;
    }

    private async void Save_Click(object sender, RoutedEventArgs e)
    {
        if (TypeCombo.SelectedIndex < 0 || ValueBox.Value <= 0) return;

        var type = MeasurementCatalog.Types[TypeCombo.SelectedIndex];
        var prefs = ViewModel.Data.UnitPreferences;
        var canonicalValue = type.Category switch
        {
            "Circumference" => UnitConversionHelper.CircumferenceToCanonical(ValueBox.Value, prefs.Circumference),
            "Body Composition" when type.UnitType == "MASS" =>
                UnitConversionHelper.MassToCanonical(ValueBox.Value, prefs.Mass),
            _ => ValueBox.Value
        };
        var measurement = new FPMeasurement
        {
            Id = Guid.NewGuid().ToString(),
            TypeId = type.Id,
            Name = type.Name,
            Unit = DisplayUnitFor(type),
            Value = canonicalValue,
            Date = DateTime.UtcNow,
            Notes = string.IsNullOrWhiteSpace(NotesBox.Text) ? null : NotesBox.Text.Trim(),
            SessionId = Guid.NewGuid().ToString()
        };

        var btn = (Button)sender;
        btn.IsEnabled = false;
        await ViewModel.Data.SaveMeasurementAsync(measurement);
        btn.IsEnabled = true;

        ValueBox.Value = 0;
        NotesBox.Text = "";
        Refresh();
    }

    private void Refresh()
    {
        var measurements = ViewModel.Data.Measurements;
        BuildOverview(measurements);
        BuildHistory(measurements);
        BuildWeightTrend(measurements);
    }

    private void BuildOverview(List<FPMeasurement> measurements)
    {
        OverviewGrid.Children.Clear();
        OverviewGrid.RowDefinitions.Clear();
        OverviewGrid.ColumnDefinitions.Clear();
        OverviewGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        OverviewGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        var row = 0;
        var col = 0;
        string? lastCategory = null;

        foreach (var category in MeasurementCatalog.Categories)
        {
            foreach (var type in MeasurementCatalog.InCategory(category))
            {
                if (category != lastCategory)
                {
                    if (col != 0)
                    {
                        row++;
                        col = 0;
                    }

                    if (OverviewGrid.RowDefinitions.Count <= row)
                        OverviewGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

                    var header = new TextBlock
                    {
                        Text = category.ToUpperInvariant(),
                        FontSize = 11,
                        FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                        Foreground = (Brush)Application.Current.Resources["AccentBrush"],
                        CharacterSpacing = 120,
                        Margin = new Thickness(0, row == 0 ? 0 : 8, 0, 4)
                    };
                    Grid.SetRow(header, row);
                    Grid.SetColumnSpan(header, 2);
                    OverviewGrid.Children.Add(header);
                    row++;
                    col = 0;
                    lastCategory = category;
                }

                if (OverviewGrid.RowDefinitions.Count <= row)
                    OverviewGrid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

                var latest = LatestForType(measurements, type);
                var hasValue = latest is not null;
                var displayUnit = DisplayUnitFor(type);
                var displayValue = DisplayValueFor(latest, type);
                var cell = BuildOverviewCell(type.Name, displayUnit, displayValue, latest, hasValue, () => SelectType(type.Name));
                OverviewGrid.Children.Add(cell);
                Grid.SetRow(cell, row);
                Grid.SetColumn(cell, col);

                col++;
                if (col >= 2)
                {
                    col = 0;
                    row++;
                }
            }
        }
    }

    private static FPMeasurement? LatestForType(List<FPMeasurement> measurements, MeasurementTypeDef type) =>
        measurements
            .Where(m => m.TypeId == type.Id || m.Name.Equals(type.Name, StringComparison.OrdinalIgnoreCase))
            .OrderByDescending(m => m.Date)
            .FirstOrDefault();

    private static Button BuildOverviewCell(string name, string unit, double displayValue, FPMeasurement? latest, bool hasValue, Action onTap)
    {
        var valueBrush = hasValue
            ? (Brush)Application.Current.Resources["AccentBrush"]!
            : (Brush)Application.Current.Resources["TextTertiaryBrush"]!;

        var valueText = new TextBlock
        {
            Text = hasValue ? $"{displayValue:0.#}" : "—",
            FontSize = 26,
            FontWeight = Microsoft.UI.Text.FontWeights.Bold,
            Foreground = valueBrush
        };

        var unitText = new TextBlock
        {
            Text = unit,
            FontSize = 13,
            Foreground = hasValue
                ? (Brush)Application.Current.Resources["TextSecondaryBrush"]!
                : (Brush)Application.Current.Resources["TextTertiaryBrush"]!,
            Margin = new Thickness(4, 6, 0, 0)
        };

        var valueRow = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 2 };
        valueRow.Children.Add(valueText);
        valueRow.Children.Add(unitText);

        var panel = new StackPanel { Spacing = 4 };
        panel.Children.Add(new TextBlock
        {
            Text = name,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            FontSize = 14,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
        });
        panel.Children.Add(valueRow);
        panel.Children.Add(new TextBlock
        {
            Text = hasValue
                ? $"Last: {latest!.Date.ToLocalTime():MMM d, yyyy}"
                : "Not recorded",
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            Foreground = hasValue
                ? (Brush)Application.Current.Resources["TextSecondaryBrush"]!
                : (Brush)Application.Current.Resources["TextTertiaryBrush"]!
        });

        var btn = new Button
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            HorizontalContentAlignment = HorizontalAlignment.Stretch,
            Background = (Brush)Application.Current.Resources["SurfaceBrush"]!,
            BorderBrush = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!,
            BorderThickness = new Thickness(1),
            CornerRadius = new CornerRadius(10),
            Padding = new Thickness(12),
            Content = panel
        };
        btn.Click += (_, _) => onTap();
        return btn;
    }

    private void SelectType(string name)
    {
        for (var i = 0; i < MeasurementCatalog.Types.Length; i++)
        {
            if (MeasurementCatalog.Types[i].Name.Equals(name, StringComparison.OrdinalIgnoreCase))
            {
                TypeCombo.SelectedIndex = i;
                ValueBox.Focus(FocusState.Programmatic);
                break;
            }
        }
    }

    private void BuildHistory(List<FPMeasurement> measurements)
    {
        HistoryList.Children.Clear();
        var recent = measurements.OrderByDescending(m => m.Date).Take(30).ToList();
        EmptyHistory.Visibility = recent.Count == 0 ? Visibility.Visible : Visibility.Collapsed;

        foreach (var m in recent)
        {
            var info = new StackPanel { Spacing = 4 };
            info.Children.Add(new TextBlock
            {
                Text = m.Name,
                FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
            });
            info.Children.Add(new TextBlock
            {
                Text = m.Date.ToLocalTime().ToString("MMM d, yyyy · h:mm tt"),
                Style = (Style)Application.Current.Resources["CaptionStyle"]
            });
            if (!string.IsNullOrEmpty(m.Notes))
                info.Children.Add(new TextBlock
                {
                    Text = m.Notes,
                    Style = (Style)Application.Current.Resources["CaptionStyle"],
                    TextWrapping = TextWrapping.Wrap
                });

            var grid = new Grid();
            grid.Children.Add(info);
            grid.Children.Add(new TextBlock
            {
                Text = UnitConversionHelper.FormatMeasurementValue(m, ViewModel.Data.UnitPreferences),
                FontSize = 22,
                FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                Foreground = (Brush)Application.Current.Resources["AccentBrush"],
                HorizontalAlignment = HorizontalAlignment.Right,
                VerticalAlignment = VerticalAlignment.Center
            });

            HistoryList.Children.Add(new Border
            {
                Style = (Style)Application.Current.Resources["CardBorderStyle"],
                Child = grid
            });
        }
    }

    private void BuildWeightTrend(List<FPMeasurement> measurements)
    {
        WeightTrendChart.UpdateMeasurements(measurements, ViewModel.Data.UnitPreferences);
        TrendCard.Visibility = WeightTrendChart.Visibility;
    }

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}