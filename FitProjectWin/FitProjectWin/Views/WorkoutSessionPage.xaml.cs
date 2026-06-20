using FitProjectWin.Helpers;
using FitProjectWin.Models;
using FitProjectWin.Services;
using Microsoft.UI.Dispatching;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;
using Windows.UI;

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

        var titleRow = new Grid();
        titleRow.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        titleRow.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });
        var title = new TextBlock
        {
            Text = exercise.Name,
            Style = (Style)Application.Current.Resources["SectionTitleStyle"]
        };
        Grid.SetColumn(title, 0);
        titleRow.Children.Add(title);
        var counter = new TextBlock
        {
            Text = $"{_vm.CurrentExerciseIndex + 1}/{_vm.Exercises.Count}",
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            VerticalAlignment = VerticalAlignment.Bottom
        };
        Grid.SetColumn(counter, 1);
        titleRow.Children.Add(counter);
        ExercisePanel.Children.Add(titleRow);

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

        var setsCard = new Border
        {
            Background = (Brush)Application.Current.Resources["SurfaceElevatedBrush"]!,
            CornerRadius = new CornerRadius(12),
            Padding = new Thickness(12),
            Margin = new Thickness(0, 8, 0, 0)
        };
        var setsPanel = new StackPanel { Spacing = 8 };

        var header = new Grid { Margin = new Thickness(0, 0, 0, 4), Height = WorkoutMetricFormat.RowHeight() };
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(WorkoutMetricFormat.SetColumnWidth()) });
        foreach (var metric in _vm.CurrentMetrics)
            header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(WorkoutMetricFormat.FieldWidth(metric.Name)) });
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(WorkoutMetricFormat.CompleteColumnWidth()) });

        header.Children.Add(new TextBlock
        {
            Text = "SET",
            FontSize = 11,
            FontWeight = Microsoft.UI.Text.FontWeights.Bold,
            Foreground = (Brush)Application.Current.Resources["TextTertiaryBrush"]!,
            VerticalAlignment = VerticalAlignment.Center,
            HorizontalAlignment = HorizontalAlignment.Center
        });
        for (var i = 0; i < _vm.CurrentMetrics.Count; i++)
        {
            var metricName = _vm.CurrentMetrics[i].Name;
            var tb = new TextBlock
            {
                Text = metricName.ToUpperInvariant(),
                FontSize = 10,
                FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                Foreground = new SolidColorBrush(MetricColor(metricName)),
                VerticalAlignment = VerticalAlignment.Center,
                HorizontalAlignment = HorizontalAlignment.Center
            };
            Grid.SetColumn(tb, i + 1);
            header.Children.Add(tb);
        }
        setsPanel.Children.Add(header);

        foreach (var set in _vm.CurrentSets)
            setsPanel.Children.Add(BuildSetRow(set));

        var addSetBtn = new Button
        {
            Content = "+ Add Set",
            HorizontalAlignment = HorizontalAlignment.Stretch,
            Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!,
            Foreground = (Brush)Application.Current.Resources["AccentBrush"]!,
            Margin = new Thickness(0, 4, 0, 0)
        };
        addSetBtn.Click += (_, _) => _vm.AddSetCommand.Execute(null);
        setsPanel.Children.Add(addSetBtn);
        setsCard.Child = setsPanel;
        ExercisePanel.Children.Add(setsCard);

        ExercisePanel.Children.Add(new TextBlock
        {
            Text = "Notes",
            FontSize = 14,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            Foreground = (Brush)Application.Current.Resources["TextSecondaryBrush"]!
        });

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

    private Grid BuildSetRow(FPLoggedSet set)
    {
        var rowHeight = WorkoutMetricFormat.RowHeight();
        var fieldHeight = WorkoutMetricFormat.FieldHeight("");
        var fieldStyle = (Style)Application.Current.Resources["WorkoutMetricFieldStyle"];
        var row = new Grid
        {
            Height = rowHeight,
            VerticalAlignment = VerticalAlignment.Center
        };
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(WorkoutMetricFormat.SetColumnWidth()) });
        foreach (var metric in _vm!.CurrentMetrics)
            row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(WorkoutMetricFormat.FieldWidth(metric.Name)) });
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(WorkoutMetricFormat.CompleteColumnWidth()) });

        var setBox = CreateMetricTextBox(
            fieldStyle,
            set.SetNumber.ToString(),
            WorkoutMetricFormat.SetColumnWidth(),
            fieldHeight,
            isReadOnly: true,
            foreground: set.IsCompleted
                ? (Brush)Application.Current.Resources["AccentBrush"]!
                : (Brush)Application.Current.Resources["TextSecondaryBrush"]!);
        Grid.SetColumn(setBox, 0);
        row.Children.Add(setBox);

        for (var i = 0; i < _vm.CurrentMetrics.Count; i++)
        {
            var metric = _vm.CurrentMetrics[i];
            var metricName = metric.Name;
            var fieldWidth = WorkoutMetricFormat.FieldWidth(metricName);
            var color = MetricColor(metricName);
            var highlighted = WorkoutMetricFormat.IsHighlighted(metricName);
            var displayValue = metricName == "Tempo"
                ? WorkoutMetricFormat.FormatTempoDisplay(GetMetricValue(set, metricName))
                : GetMetricValue(set, metricName) ?? "";

            var box = CreateMetricTextBox(
                fieldStyle,
                displayValue,
                fieldWidth,
                fieldHeight,
                placeholder: metricName == "Tempo" ? "301" : null,
                foreground: new SolidColorBrush(color));
            var capturedMetric = metricName;
            box.TextChanged += (_, _) =>
            {
                var sanitized = WorkoutMetricFormat.SanitizeMetricInput(capturedMetric, box.Text);
                if (box.Text != sanitized)
                    box.Text = capturedMetric == "Tempo"
                        ? WorkoutMetricFormat.FormatTempoDisplay(sanitized)
                        : sanitized;
                SetMetricValue(set, capturedMetric, sanitized);
            };

            var field = new Border
            {
                Width = fieldWidth,
                Height = fieldHeight,
                HorizontalAlignment = HorizontalAlignment.Center,
                VerticalAlignment = VerticalAlignment.Center,
                CornerRadius = new CornerRadius(0),
                Background = new SolidColorBrush(Color.FromArgb(highlighted ? (byte)46 : (byte)20, color.R, color.G, color.B)),
                BorderBrush = new SolidColorBrush(Color.FromArgb(highlighted ? (byte)90 : (byte)56, color.R, color.G, color.B)),
                BorderThickness = new Thickness(1),
                Child = box
            };
            Grid.SetColumn(field, i + 1);
            row.Children.Add(field);
        }

        var check = new Button
        {
            Content = set.IsCompleted ? "✓" : "○",
            Tag = set,
            Width = WorkoutMetricFormat.CompleteColumnWidth(),
            Height = WorkoutMetricFormat.CompleteColumnWidth(),
            Padding = new Thickness(0),
            HorizontalAlignment = HorizontalAlignment.Center,
            VerticalAlignment = VerticalAlignment.Center,
            CornerRadius = new CornerRadius(4),
            Background = set.IsCompleted
                ? (Brush)Application.Current.Resources["AccentBrush"]!
                : (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!
        };
        check.Click += (_, _) => _vm.ToggleSetCompleteCommand.Execute(set);
        Grid.SetColumn(check, _vm.CurrentMetrics.Count + 1);
        row.Children.Add(check);

        return row;
    }

    private static TextBox CreateMetricTextBox(
        Style style,
        string text,
        double width,
        double height,
        bool isReadOnly = false,
        string? placeholder = null,
        Brush? foreground = null)
    {
        var box = new TextBox
        {
            Style = style,
            Text = text,
            PlaceholderText = placeholder ?? "",
            Width = width,
            Height = height,
            MinWidth = width,
            MaxWidth = width,
            MinHeight = height,
            MaxHeight = height,
            Padding = WorkoutMetricFormat.FieldTextPadding(),
            IsReadOnly = isReadOnly,
            IsTabStop = !isReadOnly,
            FontSize = WorkoutMetricFormat.FieldFontSize(),
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            HorizontalAlignment = HorizontalAlignment.Stretch,
            VerticalAlignment = VerticalAlignment.Stretch
        };
        if (foreground != null)
            box.Foreground = foreground;
        return box;
    }

    private static Color MetricColor(string name) => name switch
    {
        "Reps" => Color.FromArgb(255, 0x51, 0x3B, 0xD1),
        "Weight" => Color.FromArgb(255, 0xFC, 0x47, 0x47),
        "RPE" => Color.FromArgb(255, 0xF5, 0xA6, 0x23),
        "Rest" => Color.FromArgb(255, 0x4B, 0xD6, 0x85),
        "Tempo" => Color.FromArgb(255, 0x9B, 0x59, 0xB6),
        "Time" => Color.FromArgb(255, 0x3B, 0x86, 0xD1),
        _ => Color.FromArgb(255, 0xA0, 0xA0, 0xA8)
    };

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
            case "Tempo": set.Tempo = string.IsNullOrEmpty(value) ? null : value; break;
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

