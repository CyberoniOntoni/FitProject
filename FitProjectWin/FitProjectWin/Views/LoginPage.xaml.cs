using Microsoft.UI.Xaml.Controls;

namespace FitProjectWin.Views;

public sealed partial class LoginPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public LoginPage() => InitializeComponent();

    private void PasswordBox_PasswordChanged(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        if (sender is PasswordBox box)
            ViewModel.LoginPassword = box.Password;
    }
}