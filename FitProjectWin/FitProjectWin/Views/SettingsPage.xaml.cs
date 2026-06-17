using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class SettingsPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;
    private bool _loading;

    public SettingsPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        BuildOptionGroups();
        Refresh();
    }

    private void BuildOptionGroups()
    {
        BuildGroup(MassOptions, [("kg", "KILOGRAM"), ("lb", "POUND")], "mass");
        BuildGroup(CircumferenceOptions, [("cm", "CENTIMETER"), ("in", "INCH")], "circumference");
        BuildGroup(DistanceOptions, [("km", "KILOMETER"), ("mi", "MILE")], "distance");
        BuildGroup(TimeOptions, [("sec", "SECOND"), ("min", "MINUTE")], "time");
    }

    private void BuildGroup(StackPanel panel, (string Label, string Value)[] options, string key)
    {
        panel.Children.Clear();
        panel.Tag = key;
        foreach (var (label, value) in options)
        {
            var btn = new Button
            {
                Content = label,
                Tag = value,
                MinWidth = 64,
                Padding = new Thickness(16, 8, 16, 8)
            };
            btn.Click += async (_, _) =>
            {
                if (_loading) return;
                _loading = true;
                await ViewModel.Data.UpdateUnitPreferenceAsync(key, value);
                _loading = false;
                Refresh();
            };
            panel.Children.Add(btn);
        }
    }

    private void Refresh()
    {
        var prefs = ViewModel.Data.UnitPreferences;
        HighlightGroup(MassOptions, prefs.Mass);
        HighlightGroup(CircumferenceOptions, prefs.Circumference);
        HighlightGroup(DistanceOptions, prefs.Distance);
        HighlightGroup(TimeOptions, prefs.Time);
    }

    private static void HighlightGroup(StackPanel panel, string activeValue)
    {
        foreach (var child in panel.Children)
        {
            if (child is not Button btn) continue;
            var selected = (string?)btn.Tag == activeValue;
            btn.Background = (Brush)Application.Current.Resources[
                selected ? "AccentBrush" : "SurfaceHighlightBrush"];
            btn.Foreground = selected
                ? new Microsoft.UI.Xaml.Media.SolidColorBrush(Microsoft.UI.Colors.White)
                : (Brush)Application.Current.Resources["TextPrimaryBrush"];
        }
    }

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}