using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class LearnPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public LearnPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void Refresh()
    {
        ContentList.Children.Clear();

        if (ViewModel.Data.Forms.Count > 0)
        {
            ContentList.Children.Add(new TextBlock
            {
                Text = "Check-In Forms",
                Style = (Style)Application.Current.Resources["SectionTitleStyle"]
            });
            foreach (var form in ViewModel.Data.Forms)
            {
                ContentList.Children.Add(new Border
                {
                    Style = (Style)Application.Current.Resources["CardBorderStyle"],
                    Margin = new Thickness(0, 0, 0, 8),
                    Child = new Grid
                    {
                        Children =
                        {
                            new StackPanel
                            {
                                Spacing = 4,
                                Children =
                                {
                                    new TextBlock { Text = form.Title, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"] },
                                    new TextBlock { Text = form.Description ?? "", Style = (Style)Application.Current.Resources["CaptionStyle"], TextWrapping = TextWrapping.Wrap }
                                }
                            }
                        }
                    }
                });
            }
        }

        if (ViewModel.Data.Content.Count > 0)
        {
            ContentList.Children.Add(new TextBlock
            {
                Text = "Guides & Articles",
                Style = (Style)Application.Current.Resources["SectionTitleStyle"],
                Margin = new Thickness(0, 8, 0, 0)
            });
            foreach (var item in ViewModel.Data.Content)
            {
                ContentList.Children.Add(new Border
                {
                    Style = (Style)Application.Current.Resources["CardBorderStyle"],
                    Margin = new Thickness(0, 0, 0, 8),
                    Child = new StackPanel
                    {
                        Spacing = 4,
                        Children =
                        {
                            new TextBlock { Text = item.Title, FontWeight = Microsoft.UI.Text.FontWeights.SemiBold, Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"] },
                            new TextBlock { Text = item.Type, Foreground = (Brush)Application.Current.Resources["AccentBrush"], FontSize = 12 }
                        }
                    }
                });
            }
        }

        if (ViewModel.Data.Forms.Count == 0 && ViewModel.Data.Content.Count == 0)
        {
            ContentList.Children.Add(new TextBlock
            {
                Text = "No content yet. Guides and forms from your coach on FitPros.io will appear here.",
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 40, 0, 0)
            });
        }
    }
}