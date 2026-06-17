using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class HabitsPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public HabitsPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void Refresh()
    {
        var habits = ViewModel.Data.Habits;
        HabitsList.Children.Clear();
        EmptyText.Visibility = habits.Count == 0 ? Visibility.Visible : Visibility.Collapsed;

        var completed = habits.Count(h => h.Progress >= 1);
        SummaryText.Text = habits.Count == 0
            ? "No habits configured"
            : $"{completed} of {habits.Count} habits complete today";
        OverallProgress.Value = habits.Count == 0 ? 0 : (double)completed / habits.Count;

        foreach (var habit in habits)
            HabitsList.Children.Add(BuildHabitCard(habit));
    }

    private Border BuildHabitCard(FPHabit habit)
    {
        var valueBox = new NumberBox
        {
            Value = habit.CurrentValue,
            Minimum = 0,
            Maximum = Math.Max(habit.TargetValue * 2, habit.TargetValue + 10),
            SmallChange = habit.TargetValue >= 1000 ? 500 : 1,
            LargeChange = habit.TargetValue >= 1000 ? 1000 : 5,
            SpinButtonPlacementMode = NumberBoxSpinButtonPlacementMode.Inline,
            Width = 120,
            Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"],
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        };

        var statusText = new TextBlock
        {
            Text = habit.Progress >= 1 ? "✓ Complete" : habit.ProgressText,
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            Foreground = habit.Progress >= 1
                ? (Brush)Application.Current.Resources["SuccessBrush"]
                : (Brush)Application.Current.Resources["TextSecondaryBrush"]
        };

        var progress = new ProgressBar
        {
            Value = habit.Progress,
            Maximum = 1,
            Foreground = (Brush)Application.Current.Resources["AccentBrush"],
            Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]
        };

        var saveBtn = new Button
        {
            Content = "Save",
            Background = (Brush)Application.Current.Resources["AccentBrush"],
            Foreground = new SolidColorBrush(Microsoft.UI.Colors.White),
            HorizontalAlignment = HorizontalAlignment.Stretch,
            Margin = new Thickness(0, 8, 0, 0)
        };

        saveBtn.Click += async (_, _) =>
        {
            saveBtn.IsEnabled = false;
            await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value);
            saveBtn.IsEnabled = true;
            Refresh();
        };

        valueBox.ValueChanged += async (_, args) =>
        {
            if (args.NewValue < 0) return;
            await ViewModel.Data.UpdateHabitAsync(habit.Id, args.NewValue);
            statusText.Text = args.NewValue >= habit.TargetValue ? "✓ Complete" : $"{args.NewValue:0.#} / {habit.TargetValue:0.#}{habit.Unit}";
            progress.Value = habit.TargetValue > 0 ? Math.Min(args.NewValue / habit.TargetValue, 1) : 0;
        };

        var quickRow = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8, Margin = new Thickness(0, 8, 0, 0) };

        var minusBtn = new Button { Content = "−1", Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"], Padding = new Thickness(12, 6, 12, 6) };
        minusBtn.Click += async (_, _) => { valueBox.Value = Math.Max(0, valueBox.Value - 1); await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value); Refresh(); };
        quickRow.Children.Add(minusBtn);

        var plusBtn = new Button { Content = "+1", Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"], Padding = new Thickness(12, 6, 12, 6) };
        plusBtn.Click += async (_, _) => { valueBox.Value += 1; await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value); Refresh(); };
        quickRow.Children.Add(plusBtn);

        var targetBtn = new Button { Content = "Hit target", Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"], Padding = new Thickness(12, 6, 12, 6) };
        targetBtn.Click += async (_, _) => { valueBox.Value = habit.TargetValue; await ViewModel.Data.UpdateHabitAsync(habit.Id, habit.TargetValue); Refresh(); };
        quickRow.Children.Add(targetBtn);

        var panel = new StackPanel { Spacing = 6 };
        var header = new Grid();
        header.Children.Add(new TextBlock
        {
            Text = habit.Name,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            FontSize = 16,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        });
        if (habit.Streak > 0)
        {
            var streak = new TextBlock
            {
                Text = $"🔥 {habit.Streak} day streak",
                HorizontalAlignment = HorizontalAlignment.Right,
                Foreground = (Brush)Application.Current.Resources["WarningBrush"],
                FontSize = 12
            };
            header.Children.Add(streak);
        }
        panel.Children.Add(header);

        if (!string.IsNullOrEmpty(habit.Description))
            panel.Children.Add(new TextBlock
            {
                Text = habit.Description,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap
            });

        panel.Children.Add(progress);
        panel.Children.Add(statusText);

        var inputRow = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 12, Margin = new Thickness(0, 4, 0, 0) };
        inputRow.Children.Add(new TextBlock
        {
            Text = "Log value:",
            VerticalAlignment = VerticalAlignment.Center,
            Foreground = (Brush)Application.Current.Resources["TextSecondaryBrush"]
        });
        inputRow.Children.Add(valueBox);
        panel.Children.Add(inputRow);
        panel.Children.Add(quickRow);
        panel.Children.Add(saveBtn);

        return new Border
        {
            Style = (Style)Application.Current.Resources["CardBorderStyle"],
            Child = panel
        };
    }

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}