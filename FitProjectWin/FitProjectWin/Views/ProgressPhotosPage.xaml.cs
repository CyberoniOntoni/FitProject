using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;

namespace FitProjectWin.Views;

public sealed partial class ProgressPhotosPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public ProgressPhotosPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void Refresh()
    {
        SessionsList.Children.Clear();
        var sessions = ViewModel.Data.ProgressSessions;
        EmptyText.Visibility = sessions.Count == 0 ? Visibility.Visible : Visibility.Collapsed;

        foreach (var session in sessions)
            SessionsList.Children.Add(BuildSessionCard(session));
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
            var image = new Image
            {
                Height = 140,
                Stretch = Stretch.UniformToFill,
                Source = new BitmapImage(new Uri(picture.ImageUrl))
            };
            column.Children.Add(image);
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

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}