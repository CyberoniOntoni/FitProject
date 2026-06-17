using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace FitProjectWin.Views;

public sealed partial class ProfilePage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public ProfilePage() => InitializeComponent();

    private void OpenHabits_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Habits");

    private void OpenProgressPhotos_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("ProgressPhotos");

    private void OpenMeasurements_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Measurements");

    private void OpenPersonalRecords_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("PersonalRecords");
}