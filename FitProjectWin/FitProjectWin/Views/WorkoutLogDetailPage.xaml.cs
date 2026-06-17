using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class WorkoutLogDetailPage : Page
{
    public WorkoutLogDetailPage() => InitializeComponent();

    public void Bind(FPWorkoutLog log)
    {
        DetailPanel.Children.Clear();
        DetailPanel.Children.Add(new TextBlock
        {
            Text = log.WorkoutName,
            Style = (Style)Application.Current.Resources["HeadlineStyle"]
        });

        var meta = $"{log.CompletedAt?.ToLocalTime():MMM d, yyyy · h:mm tt}";
        if (log.DurationSeconds.HasValue)
            meta += $" · {log.DurationSeconds / 60} min";
        if (log.TotalVolume > 0)
            meta += $" · {log.TotalVolume:0} kg volume";
        DetailPanel.Children.Add(new TextBlock
        {
            Text = meta,
            Style = (Style)Application.Current.Resources["CaptionStyle"]
        });

        if (!string.IsNullOrEmpty(log.Notes))
            DetailPanel.Children.Add(new Border
            {
                Style = (Style)Application.Current.Resources["CardBorderStyle"],
                Child = new TextBlock
                {
                    Text = log.Notes,
                    Style = (Style)Application.Current.Resources["CaptionStyle"],
                    TextWrapping = TextWrapping.Wrap
                }
            });

        foreach (var exercise in log.Exercises)
        {
            var card = new StackPanel { Spacing = 6 };
            card.Children.Add(new TextBlock
            {
                Text = exercise.Name,
                FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
            });
            foreach (var set in exercise.Sets.Where(s => s.IsCompleted))
            {
                var parts = new List<string> { $"Set {set.SetNumber}" };
                if (!string.IsNullOrEmpty(set.Weight)) parts.Add($"{set.Weight} kg");
                if (!string.IsNullOrEmpty(set.Reps)) parts.Add($"{set.Reps} reps");
                if (!string.IsNullOrEmpty(set.Rpe)) parts.Add($"RPE {set.Rpe}");
                if (set.IsPr) parts.Add("🏆 PR");
                card.Children.Add(new TextBlock
                {
                    Text = string.Join(" · ", parts),
                    Style = (Style)Application.Current.Resources["CaptionStyle"]
                });
            }
            DetailPanel.Children.Add(new Border
            {
                Style = (Style)Application.Current.Resources["CardBorderStyle"],
                Child = card
            });
        }
    }

    private void Back_Click(object sender, RoutedEventArgs e) => App.ViewModel.CloseWorkoutLogDetail();
}