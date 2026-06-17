using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;
using Microsoft.UI.Xaml.Shapes;

namespace FitProjectWin.Views;

public sealed partial class ProgressPhotosPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;
    private string _activeTab = "sessions";
    private string? _compareBeforeId;
    private string? _compareAfterId;

    public ProgressPhotosPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        SetActiveTab("sessions");
        Refresh();
    }

    private void Tab_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button btn && btn.Tag is string tab)
            SetActiveTab(tab);
    }

    private void SetActiveTab(string tab)
    {
        _activeTab = tab;
        var accent = (Brush)Application.Current.Resources["AccentBrush"];
        var muted = (Brush)Application.Current.Resources["SurfaceHighlightBrush"];
        var accentFg = new SolidColorBrush(Microsoft.UI.Colors.White);
        var mutedFg = (Brush)Application.Current.Resources["TextPrimaryBrush"];

        SessionsTabBtn.Background = tab == "sessions" ? accent : muted;
        SessionsTabBtn.Foreground = tab == "sessions" ? accentFg : mutedFg;
        CompareTabBtn.Background = tab == "compare" ? accent : muted;
        CompareTabBtn.Foreground = tab == "compare" ? accentFg : mutedFg;

        SessionsPanel.Visibility = tab == "sessions" ? Visibility.Visible : Visibility.Collapsed;
        ComparePanel.Visibility = tab == "compare" ? Visibility.Visible : Visibility.Collapsed;
        Refresh();
    }

    private void Refresh()
    {
        var pictures = ViewModel.Data.AllProgressPictures;
        var sessions = ViewModel.Data.ProgressSessions;
        var hasData = pictures.Count > 0;
        EmptyText.Visibility = hasData ? Visibility.Collapsed : Visibility.Visible;

        if (_activeTab == "sessions")
            BuildSessions(sessions);
        else
            BuildCompare(pictures);
    }

    private void BuildSessions(List<FPProgressSession> sessions)
    {
        SessionsPanel.Children.Clear();
        foreach (var session in sessions)
            SessionsPanel.Children.Add(BuildSessionCard(session));
    }

    private Border BuildSessionCard(FPProgressSession session)
    {
        var panel = new StackPanel { Spacing = 12 };
        var header = new Grid();
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        header.ColumnDefinitions.Add(new ColumnDefinition { Width = GridLength.Auto });

        var dateText = new TextBlock
        {
            Text = session.DateCreated.ToString("MMMM d, yyyy"),
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            FontSize = 16,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]
        };
        Grid.SetColumn(dateText, 0);
        header.Children.Add(dateText);

        var countText = new TextBlock
        {
            Text = $"{session.Pictures.Count} photo{(session.Pictures.Count == 1 ? "" : "s")}",
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            HorizontalAlignment = HorizontalAlignment.Right
        };
        Grid.SetColumn(countText, 1);
        header.Children.Add(countText);
        panel.Children.Add(header);

        if (!string.IsNullOrWhiteSpace(session.Notes))
        {
            panel.Children.Add(new TextBlock
            {
                Text = session.Notes,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap
            });
        }

        var photos = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 8 };
        foreach (var picture in session.Pictures)
        {
            var column = new StackPanel { Spacing = 6, Width = 110 };
            column.Children.Add(new Image
            {
                Height = 140,
                Stretch = Stretch.UniformToFill,
                Source = new BitmapImage(new Uri(picture.ImageUrl))
            });
            column.Children.Add(new TextBlock
            {
                Text = char.ToUpper(picture.PoseType[0]) + picture.PoseType[1..],
                FontSize = 12,
                HorizontalAlignment = HorizontalAlignment.Center,
                Foreground = (Brush)Application.Current.Resources["TextSecondaryBrush"]
            });
            photos.Children.Add(column);
        }
        panel.Children.Add(photos);

        return new Border
        {
            Style = (Style)Application.Current.Resources["CardBorderStyle"],
            Child = panel
        };
    }

    private void BuildCompare(List<FPProgressPicture> pictures)
    {
        ComparePanel.Children.Clear();
        var sorted = pictures.OrderByDescending(p => p.DateCreated).ToList();

        if (sorted.Count < 2)
        {
            ComparePanel.Children.Add(new TextBlock
            {
                Text = "Add at least two photos to compare your progress.",
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap,
                TextAlignment = TextAlignment.Center,
                Margin = new Thickness(0, 40, 0, 0)
            });
            return;
        }

        var before = _compareBeforeId is not null
            ? sorted.FirstOrDefault(p => p.Id == _compareBeforeId)
            : null;
        var after = _compareAfterId is not null
            ? sorted.FirstOrDefault(p => p.Id == _compareAfterId)
            : null;

        ComparePanel.Children.Add(new TextBlock
        {
            Text = before is null
                ? "Select your \"before\" photo"
                : after is null
                    ? "Now select your \"after\" photo"
                    : "Drag the slider to compare",
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            TextAlignment = TextAlignment.Center,
            TextWrapping = TextWrapping.Wrap
        });

        if (before is not null && after is not null)
        {
            ComparePanel.Children.Add(BuildCompareSlider(before, after));
            var resetBtn = new Button
            {
                Content = "Reset selection",
                HorizontalAlignment = HorizontalAlignment.Right
            };
            resetBtn.Click += (_, _) =>
            {
                _compareBeforeId = null;
                _compareAfterId = null;
                Refresh();
            };
            ComparePanel.Children.Add(resetBtn);
        }

        var grid = new Grid { ColumnSpacing = 8 };
        for (var i = 0; i < 3; i++)
            grid.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });

        for (var i = 0; i < sorted.Count; i++)
        {
            var pic = sorted[i];
            var isBefore = pic.Id == _compareBeforeId;
            var isAfter = pic.Id == _compareAfterId;
            var col = i % 3;
            var row = i / 3;
            while (grid.RowDefinitions.Count <= row)
                grid.RowDefinitions.Add(new RowDefinition { Height = GridLength.Auto });

            var btn = new Button
            {
                Padding = new Thickness(0),
                BorderThickness = new Thickness(isBefore || isAfter ? 2 : 1),
                BorderBrush = isBefore
                    ? (Brush)Application.Current.Resources["TextSecondaryBrush"]
                    : isAfter
                        ? (Brush)Application.Current.Resources["AccentBrush"]
                        : (Brush)Application.Current.Resources["SurfaceHighlightBrush"],
                HorizontalAlignment = HorizontalAlignment.Stretch,
                HorizontalContentAlignment = HorizontalAlignment.Stretch,
                Tag = pic
            };
            btn.Click += (_, _) => OnComparePhotoSelected(pic);

            var stack = new StackPanel { Spacing = 4 };
            stack.Children.Add(new Image
            {
                Height = 100,
                Stretch = Stretch.UniformToFill,
                Source = new BitmapImage(new Uri(pic.ImageUrl))
            });
            stack.Children.Add(new TextBlock
            {
                Text = pic.DateCreated.ToString("MMM d, yyyy"),
                FontSize = 11,
                HorizontalAlignment = HorizontalAlignment.Center,
                Foreground = (Brush)Application.Current.Resources["TextSecondaryBrush"]
            });
            if (isBefore || isAfter)
            {
                stack.Children.Add(new TextBlock
                {
                    Text = isBefore ? "Before" : "After",
                    FontSize = 11,
                    FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                    HorizontalAlignment = HorizontalAlignment.Center,
                    Foreground = isBefore
                        ? (Brush)Application.Current.Resources["TextSecondaryBrush"]
                        : (Brush)Application.Current.Resources["AccentBrush"]
                });
            }
            btn.Content = stack;
            Grid.SetColumn(btn, col);
            Grid.SetRow(btn, row);
            grid.Children.Add(btn);
        }
        ComparePanel.Children.Add(grid);
    }

    private void OnComparePhotoSelected(FPProgressPicture pic)
    {
        if (_compareBeforeId is null)
            _compareBeforeId = pic.Id;
        else if (_compareAfterId is null && pic.Id != _compareBeforeId)
            _compareAfterId = pic.Id;
        else
        {
            _compareBeforeId = pic.Id;
            _compareAfterId = null;
        }
        Refresh();
    }

    private UIElement BuildCompareSlider(FPProgressPicture before, FPProgressPicture after)
    {
        var container = new Grid { Height = 360 };
        var beforeImg = new Image
        {
            Source = new BitmapImage(new Uri(before.ImageUrl)),
            Stretch = Stretch.UniformToFill,
            HorizontalAlignment = HorizontalAlignment.Stretch,
            VerticalAlignment = VerticalAlignment.Stretch
        };
        var afterHost = new Grid { Clip = new RectangleGeometry() };
        var afterImg = new Image
        {
            Source = new BitmapImage(new Uri(after.ImageUrl)),
            Stretch = Stretch.UniformToFill,
            HorizontalAlignment = HorizontalAlignment.Stretch,
            VerticalAlignment = VerticalAlignment.Stretch
        };
        afterHost.Children.Add(afterImg);
        container.Children.Add(beforeImg);
        container.Children.Add(afterHost);

        var slider = new Slider
        {
            Minimum = 0,
            Maximum = 100,
            Value = 50,
            Margin = new Thickness(0, 12, 0, 0)
        };

        void UpdateClip(double width, double value)
        {
            var clipWidth = width * value / 100.0;
            ((RectangleGeometry)afterHost.Clip).Rect = new Windows.Foundation.Rect(0, 0, clipWidth, 360);
        }

        container.Loaded += (_, _) => UpdateClip(container.ActualWidth, slider.Value);
        container.SizeChanged += (_, _) => UpdateClip(container.ActualWidth, slider.Value);
        slider.ValueChanged += (_, e) => UpdateClip(container.ActualWidth, e.NewValue);

        var dates = new Grid { Margin = new Thickness(0, 8, 0, 0) };
        dates.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        dates.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        var beforeDate = new TextBlock
        {
            Text = $"Before · {before.DateCreated:MMM d, yyyy}",
            Style = (Style)Application.Current.Resources["CaptionStyle"]
        };
        var afterDate = new TextBlock
        {
            Text = $"After · {after.DateCreated:MMM d, yyyy}",
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            HorizontalAlignment = HorizontalAlignment.Right,
            Foreground = (Brush)Application.Current.Resources["AccentBrush"]
        };
        Grid.SetColumn(beforeDate, 0);
        Grid.SetColumn(afterDate, 1);
        dates.Children.Add(beforeDate);
        dates.Children.Add(afterDate);

        var wrapper = new StackPanel { Spacing = 4 };
        wrapper.Children.Add(container);
        wrapper.Children.Add(slider);
        wrapper.Children.Add(dates);
        return wrapper;
    }

    private void AddPhoto_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("AddProgressPhoto");

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}