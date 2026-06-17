using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class PersonalRecordsPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public PersonalRecordsPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void Refresh()
    {
        RecordsList.Children.Clear();
        EmptyText.Visibility = ViewModel.Data.PersonalRecords.Count == 0
            ? Visibility.Visible : Visibility.Collapsed;

        foreach (var pr in ViewModel.Data.PersonalRecords)
        {
            RecordsList.Children.Add(new Border
            {
                Style = (Style)Application.Current.Resources["CardBorderStyle"],
                Child = new Grid
                {
                    Children =
                    {
                        new StackPanel
                        {
                            Spacing = 4,
                            Children =
                            {
                                new TextBlock
                                {
                                    Text = pr.ExerciseName,
                                    FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                                    Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
                                },
                                new TextBlock
                                {
                                    Text = pr.Date.ToLocalTime().ToString("MMM d, yyyy"),
                                    Style = (Style)Application.Current.Resources["CaptionStyle"]
                                }
                            }
                        },
                        new TextBlock
                        {
                            Text = $"{pr.Value} {pr.Metric}",
                            FontSize = 22,
                            FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                            Foreground = (Brush)Application.Current.Resources["AccentBrush"]!,
                            HorizontalAlignment = HorizontalAlignment.Right,
                            VerticalAlignment = VerticalAlignment.Center
                        }
                    }
                }
            });
        }
    }

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Profile");
}