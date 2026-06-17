using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class ProgramsPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public ProgramsPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += RefreshPrograms;
        RefreshPrograms();
    }

    private void RefreshPrograms()
    {
        ProgramsList.Children.Clear();
        foreach (var program in ViewModel.Data.Programs)
        {
            var card = BuildProgramCard(program);
            ProgramsList.Children.Add(card);
        }

        if (ViewModel.Data.Programs.Count == 0)
        {
            ProgramsList.Children.Add(new TextBlock
            {
                Text = "No programs yet. Programs from FitPros.io will appear here automatically.",
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 40, 0, 0)
            });
        }
    }

    private UIElement BuildProgramCard(FPProgram program)
    {
        var expander = new Expander
        {
            Header = program.Title,
            HorizontalAlignment = HorizontalAlignment.Stretch,
            Margin = new Thickness(0, 0, 0, 8),
            Background = (Brush)Application.Current.Resources["SurfaceBrush"]
        };

        var panel = new StackPanel { Spacing = 8, Padding = new Thickness(8) };

        if (!string.IsNullOrEmpty(program.Description))
            panel.Children.Add(new TextBlock
            {
                Text = program.Description,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap
            });

        panel.Children.Add(new TextBlock
        {
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            Text = $"{program.TotalWeekCount} weeks · {program.TotalWorkoutCount} workouts · {program.ProgressPercent}% complete"
        });

        if (ViewModel.Data.ProgramWeeks.TryGetValue(program.Id, out var weeks))
        {
            foreach (var week in weeks.OrderBy(w => w.Index))
            {
                panel.Children.Add(new TextBlock
                {
                    Text = week.Name,
                    FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                    Foreground = (Brush)Application.Current.Resources["AccentBrush"],
                    Margin = new Thickness(0, 8, 0, 4)
                });

                foreach (var workout in week.Workouts.OrderBy(w => w.Index))
                {
                    var row = new Grid { Margin = new Thickness(0, 4, 0, 4) };
                    row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
                    row.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

                    var info = new StackPanel();
                    info.Children.Add(new TextBlock
                    {
                        Text = workout.Name,
                        Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
                    });
                    info.Children.Add(new TextBlock
                    {
                        Text = $"{workout.ExerciseCount} exercises",
                        Style = (Style)Application.Current.Resources["CaptionStyle"]
                    });
                    Grid.SetColumn(info, 0);
                    row.Children.Add(info);

                    var startBtn = new Button
                    {
                        Content = "Start",
                        Tag = workout,
                        Background = (Brush)Application.Current.Resources["AccentBrush"],
                        Foreground = new SolidColorBrush(Microsoft.UI.Colors.White),
                        CornerRadius = new CornerRadius(12),
                        Padding = new Thickness(16, 6, 16, 6)
                    };
                    startBtn.Click += (_, _) => ViewModel.StartWorkoutCommand.Execute(workout);
                    Grid.SetColumn(startBtn, 1);
                    row.Children.Add(startBtn);

                    panel.Children.Add(row);
                }
            }
        }

        expander.Content = panel;
        return expander;
    }
}