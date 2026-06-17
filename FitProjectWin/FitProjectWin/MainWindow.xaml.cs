using FitProjectWin.Models;
using FitProjectWin.Views;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;

namespace FitProjectWin;

public sealed partial class MainWindow : Window
{
    private readonly MainViewModel _vm = App.ViewModel;

    public MainWindow()
    {
        InitializeComponent();
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(null);
        AppWindow.Resize(new Windows.Graphics.SizeInt32(420, 800));

        _vm.Auth.AuthStateChanged += UpdateUI;
        _vm.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(MainViewModel.ShowWorkoutSession))
            {
                WorkoutOverlay.Visibility = _vm.ShowWorkoutSession ? Visibility.Visible : Visibility.Collapsed;
                if (_vm.ShowWorkoutSession && _vm.WorkoutSession is not null)
                    WorkoutPage.Bind(_vm.WorkoutSession);
            }
        };

        UpdateUI();
    }

    private void UpdateUI()
    {
        if (_vm.IsAuthenticated)
        {
            LoginPanel.Visibility = Visibility.Collapsed;
            MainPanel.Visibility = Visibility.Visible;
            if (ContentFrame.Content is null)
                NavigateTo("Train");
        }
        else
        {
            LoginPanel.Visibility = Visibility.Visible;
            MainPanel.Visibility = Visibility.Collapsed;
            WorkoutOverlay.Visibility = Visibility.Collapsed;
        }
    }

    private void NavView_SelectionChanged(NavigationView sender, NavigationViewSelectionChangedEventArgs args)
    {
        if (args.SelectedItem is NavigationViewItem item && item.Tag is string tag)
            NavigateTo(tag);
    }

    private void NavigateTo(string tag)
    {
        Page? page = tag switch
        {
            "Train" => new TrainPage(),
            "Programs" => new ProgramsPage(),
            "Learn" => new LearnPage(),
            "History" => new HistoryPage(),
            "Profile" => new ProfilePage(),
            _ => null
        };
        if (page is not null) ContentFrame.Content = page;
    }
}