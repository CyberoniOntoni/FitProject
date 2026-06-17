using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class MeasurementsPage : Page
{
    private static readonly (string Name, string Unit)[] Types =
    [
        ("Weight", "kg"),
        ("Body Fat", "%"),
        ("Chest", "cm"),
        ("Waist", "cm"),
        ("Hips", "cm"),
        ("Arms", "cm"),
        ("Thighs", "cm")
    ];

    public MainViewModel ViewModel { get; } = App.ViewModel;

    public MeasurementsPage()
    {
        InitializeComponent();
        foreach (var (name, _) in Types)
            TypeCombo.Items.Add(name);
        TypeCombo.SelectedIndex = 0;
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void TypeCombo_SelectionChanged(object sender, SelectionChangedEventArgs e)
    {
        if (TypeCombo.SelectedIndex >= 0 && TypeCombo.SelectedIndex < Types.Length)
            UnitLabel.Text = $"Unit: {Types[TypeCombo.SelectedIndex].Unit}";
    }

    private async void Save_Click(object sender, RoutedEventArgs e)
    {
        if (TypeCombo.SelectedIndex < 0 || ValueBox.Value <= 0) return;

        var (name, unit) = Types[TypeCombo.SelectedIndex];
        var measurement = new FPMeasurement
        {
            Id = Guid.NewGuid().ToString(),
            Name = name,
            Unit = unit,
            Value = ValueBox.Value,
            Date = DateTime.UtcNow,
            Notes = string.IsNullOrWhiteSpace(NotesBox.Text) ? null : NotesBox.Text.Trim()
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
        HistoryList.Children.Clear();
        EmptyHistory.Visibility = measurements.Count == 0 ? Visibility.Visible : Visibility.Collapsed;

        foreach (var m in measurements)
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
                Text = $"{m.Value:0.#} {m.Unit}",
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

        BuildWeightTrend(measurements);
    }

    private void BuildWeightTrend(List<FPMeasurement> measurements)
    {
        var weights = measurements
            .Where(m => m.Name.Contains("Weight", StringComparison.OrdinalIgnoreCase))
            .OrderBy(m => m.Date)
            .TakeLast(10)
            .ToList();

        TrendChart.Children.Clear();
        if (weights.Count < 2)
        {
            TrendCard.Visibility = Visibility.Collapsed;
            return;
        }

        TrendCard.Visibility = Visibility.Visible;
        var min = weights.Min(w => w.Value);
        var max = weights.Max(w => w.Value);
        var range = Math.Max(max - min, 0.1);

        foreach (var w in weights)
        {
            var height = (w.Value - min) / range * 70 + 20;
            TrendChart.Children.Add(new StackPanel
            {
                Spacing = 4,
                VerticalAlignment = VerticalAlignment.Bottom,
                Children =
                {
                    new Border
                    {
                        Width = 24,
                        Height = height,
                        CornerRadius = new CornerRadius(4),
                        Background = (Brush)Application.Current.Resources["AccentBrush"]
                    },
                    new TextBlock
                    {
                        Text = w.Date.ToLocalTime().ToString("M/d"),
                        FontSize = 9,
                        Foreground = (Brush)Application.Current.Resources["TextTertiaryBrush"],
                        HorizontalAlignment = HorizontalAlignment.Center
                    }
                }
            });
        }
    }

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}