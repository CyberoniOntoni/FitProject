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

        var completed = habits.Count(h => h.IsComplete);
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
            HorizontalAlignment = HorizontalAlignment.Stretch,
            Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"],
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        };

        var statusText = new TextBlock
        {
            Text = habit.IsComplete ? "✓ Complete" : habit.ProgressText,
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            Foreground = habit.IsComplete
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
            MinWidth = 72,
            Background = (Brush)Application.Current.Resources["AccentBrush"],
            Foreground = new SolidColorBrush(Microsoft.UI.Colors.White),
            VerticalAlignment = VerticalAlignment.Center
        };

        saveBtn.Click += async (_, _) =>
        {
            saveBtn.IsEnabled = false;
            await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value);
            saveBtn.IsEnabled = true;
            Refresh();
        };

        var quickGrid = new Grid { ColumnSpacing = 8, Margin = new Thickness(0, 4, 0, 0) };
        quickGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        quickGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        quickGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        var minusBtn = CreateQuickButton("−1");
        minusBtn.Click += async (_, _) =>
        {
            valueBox.Value = Math.Max(0, valueBox.Value - 1);
            await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value);
            Refresh();
        };
        Grid.SetColumn(minusBtn, 0);
        quickGrid.Children.Add(minusBtn);

        var plusBtn = CreateQuickButton("+1");
        plusBtn.Click += async (_, _) =>
        {
            valueBox.Value += 1;
            await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value);
            Refresh();
        };
        Grid.SetColumn(plusBtn, 1);
        quickGrid.Children.Add(plusBtn);

        var targetBtn = CreateQuickButton("Hit target");
        targetBtn.Click += async (_, _) =>
        {
            valueBox.Value = habit.TargetType == "RANGE" ? habit.TargetMax : habit.TargetMin;
            await ViewModel.Data.UpdateHabitAsync(habit.Id, valueBox.Value);
            Refresh();
        };
        Grid.SetColumn(targetBtn, 2);
        quickGrid.Children.Add(targetBtn);

        var inputGrid = new Grid { ColumnSpacing = 12, Margin = new Thickness(0, 8, 0, 0) };
        inputGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        inputGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        inputGrid.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

        var label = new TextBlock
        {
            Text = "Log value",
            VerticalAlignment = VerticalAlignment.Center,
            Foreground = (Brush)Application.Current.Resources["TextSecondaryBrush"]
        };
        Grid.SetColumn(label, 0);
        inputGrid.Children.Add(label);

        Grid.SetColumn(valueBox, 1);
        inputGrid.Children.Add(valueBox);

        Grid.SetColumn(saveBtn, 2);
        inputGrid.Children.Add(saveBtn);

        var panel = new StackPanel { Spacing = 8 };
        var header = new Grid();
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

        var title = new TextBlock
        {
            Text = habit.Name,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            FontSize = 16,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        };
        Grid.SetColumn(title, 0);
        header.Children.Add(title);

        if (habit.IsComplete)
        {
            var complete = new TextBlock
            {
                Text = "On target",
                Foreground = (Brush)Application.Current.Resources["SuccessBrush"],
                FontSize = 12,
                HorizontalAlignment = HorizontalAlignment.Right
            };
            Grid.SetColumn(complete, 1);
            header.Children.Add(complete);
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
        panel.Children.Add(inputGrid);
        panel.Children.Add(quickGrid);

        return new Border
        {
            Style = (Style)Application.Current.Resources["CardBorderStyle"],
            Child = panel
        };
    }

    private static Button CreateQuickButton(string label) => new()
    {
        Content = label,
        HorizontalAlignment = HorizontalAlignment.Stretch,
        HorizontalContentAlignment = HorizontalAlignment.Center,
        Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"],
        Padding = new Thickness(8, 10, 8, 10)
    };

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}