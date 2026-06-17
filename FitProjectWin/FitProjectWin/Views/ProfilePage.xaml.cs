using Microsoft.UI.Xaml.Controls;

namespace FitProjectWin.Views;

public sealed partial class ProfilePage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public ProfilePage() => InitializeComponent();
}