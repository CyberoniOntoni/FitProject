using FitProjectWin.ViewModels;
using Microsoft.UI.Xaml;

namespace FitProjectWin;

public partial class App : Application
{
    public static MainViewModel ViewModel { get; } = new();

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