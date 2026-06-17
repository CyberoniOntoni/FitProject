using FitProjectWin.Services;
using FitProjectWin.ViewModels;
using Microsoft.UI.Xaml;

namespace FitProjectWin;

public partial class App : Application
{
    public static MainViewModel ViewModel { get; } = new();
    public static NavigationService? NavigationService { get; set; }
    public static MainWindow? MainWindow { get; set; }

    public App()
    {
        InitializeComponent();
    }

    protected override void OnLaunched(LaunchActivatedEventArgs args)
    {
        var window = new MainWindow();
        window.Activate();
    }
}