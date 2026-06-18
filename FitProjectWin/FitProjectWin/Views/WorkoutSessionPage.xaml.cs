using FitProjectWin.Models;
using FitProjectWin.Services;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;

namespace FitProjectWin.Views;

public sealed partial class WorkoutSessionPage : Page
{
    private WorkoutSessionViewModel? _vm;
    private readonly DispatcherQueue _dispatcher = DispatcherQueue.GetForCurrentThread();
    private string? _displayedYoutubeId;

    public WorkoutSessionPage() => InitializeComponent();

    public void Bind(WorkoutSessionViewModel vm)
    {
        _vm = vm;
        _vm.PropertyChanged += (_, e) =>
        {
            _dispatcher.TryEnqueue(() =>
            {
                if (e.PropertyName is nameof(WorkoutSessionViewModel.ElapsedDisplay))
                    ElapsedText.Text = _vm.ElapsedDisplay;
                if (e.PropertyName is nameof(WorkoutSessionViewModel.ShowRestTimer))
                    RestOverlay.Visibility = _vm.ShowRestTimer ? Visibility.Visible : Visibility.Collapsed;
                if (e.PropertyName is nameof(WorkoutSessionViewModel.RestSecondsRemaining))
                    RestTimerText.Text = $"{_vm.RestSecondsRemaining / 60}:{_vm.RestSecondsRemaining % 60:D2}";
                if (e.PropertyName is nameof(WorkoutSessionViewModel.ShowPrToast))
                    PrToast.Visibility = _vm.ShowPrToast ? Visibility.Visible : Visibility.Collapsed;
                if (e.PropertyName is nameof(WorkoutSessionViewModel.PrMessage))
                    PrToastText.Text = $"🏆 New PR! {_vm.PrMessage}";
                if (e.PropertyName is nameof(WorkoutSessionViewModel.CurrentExerciseIndex) or
                    nameof(WorkoutSessionViewModel.CurrentExercise))
                    RenderExercise();
            });
        };
        WorkoutTitle.Text = vm.Workout.Name;
        RenderExercise();
    }

    private void UpdateVideoSection(FPWorkoutExercise? exercise)
    {
        if (exercise is null || string.IsNullOrEmpty(exercise.YoutubeId) ||
            string.IsNullOrEmpty(exercise.VideoThumbnailUrl))
        {
            StopVideoPlayback();
            VideoSection.Visibility = Visibility.Collapsed;
            _displayedYoutubeId = null;
            return;
        }

        VideoSection.Visibility = Visibility.Visible;

        if (_displayedYoutubeId == exercise.YoutubeId)
            return;

        StopVideoPlayback();
        _displayedYoutubeId = exercise.YoutubeId;
        VideoThumbnail.Source = new BitmapImage(new Uri(exercise.VideoThumbnailUrl));
    }

    private void ShowVideoPreview()
    {
        VideoPreviewLayer.Visibility = Visibility.Visible;
        ExerciseVideoWebView.Visibility = Visibility.Collapsed;
        VideoLoadingRing.Visibility = Visibility.Collapsed;
        VideoLoadingRing.IsActive = false;
    }

    private void StopVideoPlayback()
    {
        YouTubeEmbedHelper.StopVideo(ExerciseVideoWebView);
        ShowVideoPreview();
    }

    private async void VideoPlay_Click(object sender, RoutedEventArgs e)
    {
        var youtubeId = YouTubeEmbedHelper.NormalizeYoutubeId(_vm?.CurrentExercise?.YoutubeId);
        if (string.IsNullOrEmpty(youtubeId)) return;

        VideoPreviewLayer.Visibility = Visibility.Collapsed;
        ExerciseVideoWebView.Visibility = Visibility.Visible;
        VideoLoadingRing.Visibility = Visibility.Visible;
        VideoLoadingRing.IsActive = true;

        void OnNavigationCompleted(object? _, Microsoft.Web.WebView2.Core.CoreWebView2NavigationCompletedEventArgs args)
        {
            ExerciseVideoWebView.NavigationCompleted -= OnNavigationCompleted;
            VideoLoadingRing.Visibility = Visibility.Collapsed;
            VideoLoadingRing.IsActive = false;
        }

        ExerciseVideoWebView.NavigationCompleted += OnNavigationCompleted;

        try
        {
            await YouTubeEmbedHelper.LoadVideoAsync(ExerciseVideoWebView, youtubeId);
        }
        catch
        {
            ExerciseVideoWebView.NavigationCompleted -= OnNavigationCompleted;
            ShowVideoPreview();
        }
    }

    private void RenderExercise()
    {
        if (_vm is null) return;

        ExerciseProgress.Value = _vm.ExerciseProgress;
        ExercisePanel.Children.Clear();

        var exercise = _vm.CurrentExercise;
        if (exercise is null) return;

        UpdateVideoSection(exercise);

        if (exercise.HeaderVisible && !string.IsNullOrEmpty(exercise.Header))
            ExercisePanel.Children.Add(new TextBlock
            {
                Text = exercise.Header.ToUpperInvariant(),
                FontSize = 11, FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                Foreground = (Brush)Application.Current.Resources["AccentBrush"],
                CharacterSpacing = 150
            });

        ExercisePanel.Children.Add(new TextBlock
        {
            Text = exercise.Name,
            Style = (Style)Application.Current.Resources["SectionTitleStyle"]
        });

        if (!string.IsNullOrEmpty(exercise.CoachNotes))
            ExercisePanel.Children.Add(new Border
            {
                Background = (Brush)Application.Current.Resources["SurfaceBrush"],
                CornerRadius = new CornerRadius(8),
                Padding = new Thickness(12),
                Child = new TextBlock
                {
                    Text = exercise.CoachNotes,
                    Style = (Style)Application.Current.Resources["CaptionStyle"],
                    TextWrapping = TextWrapping.Wrap
                }
            });

        ExercisePanel.Children.Add(new TextBlock
        {
            Text = $"Exercise {_vm.CurrentExerciseIndex + 1} of {_vm.Exercises.Count}",
            Style = (Style)Application.Current.Resources["CaptionStyle"]
        });

        var header = new Grid { Margin = new Thickness(0, 8, 0, 4) };
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(30) });
        foreach (var metric in _vm.CurrentMetrics)
            header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(36) });

        header.Children.Add(new TextBlock { Text = "SET", FontSize = 10, Foreground = (Brush)Application.Current.Resources["TextTertiaryBrush"], VerticalAlignment = VerticalAlignment.Center });
        for (var i = 0; i < _vm.CurrentMetrics.Count; i++)
        {
            var tb = new TextBlock
            {
                Text = _vm.CurrentMetrics[i].Name.ToUpperInvariant(),
                FontSize = 10,
                Foreground = (Brush)Application.Current.Resources["TextTertiaryBrush"],
                HorizontalAlignment = HorizontalAlignment.Center
            };
            Grid.SetColumn(tb, i + 1);
            header.Children.Add(tb);
        }
        ExercisePanel.Children.Add(header);

        foreach (var set in _vm.CurrentSets)
            ExercisePanel.Children.Add(BuildSetRow(set, exercise));

        var addSetBtn = new Button
        {
            Content = "+ Add Set",
            HorizontalAlignment = HorizontalAlignment.Stretch,
            Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"],
            Foreground = (Brush)Application.Current.Resources["AccentBrush"],
            Margin = new Thickness(0, 8, 0, 0)
        };
        addSetBtn.Click += (_, _) => _vm.AddSetCommand.Execute(null);
        ExercisePanel.Children.Add(addSetBtn);

        var notes = new TextBox
        {
            PlaceholderText = "Add a note...",
            Text = _vm.Notes,
            TextWrapping = TextWrapping.Wrap,
            AcceptsReturn = true,
            MinHeight = 60,
            Background = (Brush)Application.Current.Resources["SurfaceBrush"],
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        };
        notes.TextChanged += (_, _) => _vm.Notes = notes.Text;
        ExercisePanel.Children.Add(notes);

        PrevBtn.IsEnabled = _vm.CurrentExerciseIndex > 0;
        NextBtn.Content = _vm.CurrentExerciseIndex < _vm.Exercises.Count - 1 ? "Next Exercise" : "Complete Workout";
    }

    private Grid BuildSetRow(FPLoggedSet set, FPWorkoutExercise exercise)
    {
        var row = new Grid { Margin = new Thickness(0, 4, 0, 4) };
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(30) });
        foreach (var _ in _vm!.CurrentMetrics)
            row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(36) });

        var setNum = new TextBlock
        {
            Text = set.SetNumber.ToString(),
            VerticalAlignment = VerticalAlignment.Center,
            Foreground = (Brush)Application.Current.Resources["TextTertiaryBrush"]
        };
        row.Children.Add(setNum);

        for (var i = 0; i < _vm.CurrentMetrics.Count; i++)
        {
            var metric = _vm.CurrentMetrics[i];
            var box = new TextBox
            {
                Text = GetMetricValue(set, metric.Name) ?? "",
                HorizontalAlignment = HorizontalAlignment.Stretch,
                Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"],
                Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
            };
            var capturedMetric = metric.Name;
            box.TextChanged += (_, _) => SetMetricValue(set, capturedMetric, box.Text);
            Grid.SetColumn(box, i + 1);
            row.Children.Add(box);
        }

        var check = new Button
        {
            Content = set.IsCompleted ? "✓" : "○",
            Tag = set,
            Width = 32, Height = 32,
            Background = set.IsCompleted
                ? (Brush)Application.Current.Resources["AccentBrush"]
                : (Brush)Application.Current.Resources["SurfaceHighlightBrush"]
        };
        check.Click += (_, _) => _vm.ToggleSetCompleteCommand.Execute(set);
        Grid.SetColumn(check, _vm.CurrentMetrics.Count + 1);
        row.Children.Add(check);

        return row;
    }

    private static string? GetMetricValue(FPLoggedSet set, string name) => name switch
    {
        "Reps" => set.Reps,
        "Weight" => set.Weight,
        "RPE" => set.Rpe,
        "Rest" => set.Rest,
        "Tempo" => set.Tempo,
        "Time" => set.Time,
        _ => null
    };

    private static void SetMetricValue(FPLoggedSet set, string name, string value)
    {
        switch (name)
        {
            case "Reps": set.Reps = value; break;
            case "Weight": set.Weight = value; break;
            case "RPE": set.Rpe = value; break;
            case "Rest": set.Rest = value; break;
            case "Tempo": set.Tempo = value; break;
            case "Time": set.Time = value; break;
        }
    }

    private void Close_Click(object sender, RoutedEventArgs e) => Close();

    private async void Finish_Click(object sender, RoutedEventArgs e)
    {
        if (_vm is null) return;
        await _vm.CompleteWorkoutCommand.ExecuteAsync(null);
        Close();
    }

    private void Prev_Click(object sender, RoutedEventArgs e) => _vm?.PreviousExerciseCommand.Execute(null);
    private void Next_Click(object sender, RoutedEventArgs e)
    {
        if (_vm is null) return;
        if (_vm.CurrentExerciseIndex < _vm.Exercises.Count - 1)
            _vm.NextExerciseCommand.Execute(null);
        else
            Finish_Click(sender, e);
    }
    private void Skip_Click(object sender, RoutedEventArgs e) => _vm?.NextExerciseCommand.Execute(null);
    private void SkipRest_Click(object sender, RoutedEventArgs e) => _vm?.SkipRestCommand.Execute(null);
    private void AddRest_Click(object sender, RoutedEventArgs e) => _vm?.AddRest30Command.Execute(null);

    private void Close()
    {
        StopVideoPlayback();
        _displayedYoutubeId = null;
        _vm?.Dispose();
        App.ViewModel.ShowWorkoutSession = false;
    }
}

