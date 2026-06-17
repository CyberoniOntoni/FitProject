using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class ContentDetailPage : Page
{
    public ContentDetailPage() => InitializeComponent();

    public void Bind(FPContent content)
    {
        ContentPanel.Children.Clear();
        ContentPanel.Children.Add(new TextBlock
        {
            Text = content.Title,
            Style = (Style)Application.Current.Resources["HeadlineStyle"]
        });
        ContentPanel.Children.Add(new TextBlock
        {
            Text = content.Type,
            Foreground = (Brush)Application.Current.Resources["AccentBrush"]!,
            FontSize = 13
        });
        if (!string.IsNullOrEmpty(content.Body))
            ContentPanel.Children.Add(new TextBlock
            {
                Text = content.Body,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap,
                LineHeight = 22,
                Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
            });
    }

    private void Back_Click(object sender, RoutedEventArgs e) => App.ViewModel.CloseContentDetail();
}