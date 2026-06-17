using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class HistoryPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public HistoryPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void Refresh()
    {
        HistoryList.Children.Clear();

        if (ViewModel.Data.PersonalRecords.Count > 0)
        {
            HistoryList.Children.Add(new TextBlock
            {
                Text = "Personal Records",
                Style = (Style)Application.Current.Resources["SectionTitleStyle"],
                Margin = new Thickness(0, 0, 0, 8)
            });

            var prPanel = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
            foreach (var pr in ViewModel.Data.PersonalRecords.Take(8))
            {
                prPanel.Children.Add(new Border
                {
                    Background = (Brush)Application.Current.Resources["SurfaceBrush"],
                    CornerRadius = new CornerRadius(12),
                    Padding = new Thickness(12),
                    Child = new StackPanel
                    {
                        Spacing = 4,
                        Children =
                        {
                            new TextBlock { Text = "🏆 PR", Foreground = (Brush)Application.Current.Resources["PrGoldBrush"], FontSize = 11, FontWeight = Microsoft.UI.Text.FontWeights.Bold },
                            new TextBlock { Text = pr.ExerciseName, Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"], FontSize = 13 },
                            new TextBlock { Text = $"{pr.Value} {pr.Metric}", Foreground = (Brush)Application.Current.Resources["AccentBrush"], FontSize = 16, FontWeight = Microsoft.UI.Text.FontWeights.Bold }
                        }
                    }
                });
            }
            HistoryList.Children.Add(prPanel);
        }

        foreach (var log in ViewModel.Data.WorkoutLogs)
            HistoryList.Children.Add(BuildLogCard(log));

        if (ViewModel.Data.WorkoutLogs.Count == 0)
        {
            HistoryList.Children.Add(new TextBlock
            {
                Text = "No workout history yet. Completed workouts sync with FitPros.io.",
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 40, 0, 0)
            });
        }
    }

    private Border BuildLogCard(FPWorkoutLog log)
    {
        var panel = new StackPanel { Spacing = 4 };
        panel.Children.Add(new TextBlock
        {
            Text = log.WorkoutName,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        });

        var meta = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 12 };
        if (log.CompletedAt.HasValue)
            meta.Children.Add(new TextBlock
            {
                Text = log.CompletedAt.Value.ToString("MMM d"),
                Style = (Style)Application.Current.Resources["CaptionStyle"]
            });
        if (log.DurationSeconds.HasValue)
            meta.Children.Add(new TextBlock
            {
                Text = $"{log.DurationSeconds / 60} min",
                Style = (Style)Application.Current.Resources["CaptionStyle"]
            });
        meta.Children.Add(new TextBlock
        {
            Text = $"{log.Exercises.Count} exercises",
            Style = (Style)Application.Current.Resources["CaptionStyle"]
        });
        panel.Children.Add(meta);

        if (log.PrCount > 0)
            panel.Children.Add(new TextBlock
            {
                Text = $"🏆 {log.PrCount} PR(s)",
                Foreground = (Brush)Application.Current.Resources["PrGoldBrush"],
                FontSize = 12
            });

        return new Border
        {
            Style = (Style)Application.Current.Resources["CardBorderStyle"],
            Margin = new Thickness(0, 0, 0, 8),
            Child = panel
        };
    }
}