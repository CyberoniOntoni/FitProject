using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace FitProjectWin.Views;

public sealed partial class VideoPlayerPage : Page
{
    public VideoPlayerPage() => InitializeComponent();

    public async void Play(string youtubeId, string title)
    {
        TitleText.Text = title;
        LoadingRing.IsActive = true;
        LoadingRing.Visibility = Visibility.Visible;

        try
        {
            await VideoWebView.EnsureCoreWebView2Async();
            var embedUrl = $"https://www.youtube.com/embed/{youtubeId}?autoplay=1&rel=0&modestbranding=1&playsinline=1";
            VideoWebView.Source = new Uri(embedUrl);
            VideoWebView.NavigationCompleted += (_, _) =>
            {
                LoadingRing.IsActive = false;
                LoadingRing.Visibility = Visibility.Collapsed;
            };
        }
        catch
        {
            LoadingRing.IsActive = false;
            LoadingRing.Visibility = Visibility.Collapsed;
        }
    }

    private void Close_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            if (VideoWebView.CoreWebView2 is not null)
                VideoWebView.CoreWebView2.Navigate("about:blank");
        }
        catch { }

        App.ViewModel.ShowVideoPlayer = false;
    }
}