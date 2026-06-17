using FitProjectWin.Services;
using FitProjectWin.Views;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin;

public sealed partial class MainWindow : Window
{
    private readonly MainViewModel _vm = App.ViewModel;
    private string _activeNav = "Train";

    public MainWindow()
    {
        InitializeComponent();
        ExtendsContentIntoTitleBar = true;
        SetTitleBar(null);
        AppWindow.Resize(new Windows.Graphics.SizeInt32(420, 800));
        App.NavigationService = new NavigationService(NavigateTo);

        _vm.Auth.AuthStateChanged += UpdateUI;
        _vm.PropertyChanged += (_, e) =>
        {
            if (e.PropertyName is nameof(MainViewModel.ShowWorkoutSession))
            {
                WorkoutOverlay.Visibility = _vm.ShowWorkoutSession ? Visibility.Visible : Visibility.Collapsed;
                if (_vm.ShowWorkoutSession && _vm.WorkoutSession is not null)
                    WorkoutPage.Bind(_vm.WorkoutSession);
            }
            if (e.PropertyName is nameof(MainViewModel.ShowFormFill))
            {
                FormOverlay.Visibility = _vm.ShowFormFill ? Visibility.Visible : Visibility.Collapsed;
                if (_vm.ShowFormFill && _vm.ActiveForm is not null)
                    FormPage.Bind(_vm.ActiveForm);
            }
            if (e.PropertyName is nameof(MainViewModel.ShowContentDetail))
            {
                ContentOverlay.Visibility = _vm.ShowContentDetail ? Visibility.Visible : Visibility.Collapsed;
                if (_vm.ShowContentDetail && _vm.ActiveContent is not null)
                    ContentPage.Bind(_vm.ActiveContent);
            }
            if (e.PropertyName is nameof(MainViewModel.ShowWorkoutLogDetail))
            {
                LogOverlay.Visibility = _vm.ShowWorkoutLogDetail ? Visibility.Visible : Visibility.Collapsed;
                if (_vm.ShowWorkoutLogDetail && _vm.ActiveWorkoutLog is not null)
                    LogPage.Bind(_vm.ActiveWorkoutLog);
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

    private void NavButton_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button btn && btn.Tag is string tag)
            NavigateTo(tag);
    }

    private void NavigateTo(string tag)
    {
        _activeNav = tag;
        UpdateNavHighlight();

        Page? page = tag switch
        {
            "Train" => new TrainPage(),
            "Programs" => new ProgramsPage(),
            "Learn" => new LearnPage(),
            "History" => new HistoryPage(),
            "Profile" => new ProfilePage(),
            "Habits" => new HabitsPage(),
            "Measurements" => new MeasurementsPage(),
            "PersonalRecords" => new PersonalRecordsPage(),
            _ => null
        };
        if (page is not null) ContentFrame.Content = page;
    }

    private void UpdateNavHighlight()
    {
        var accent = (Brush)Application.Current.Resources["AccentBrush"];
        var muted = (Brush)Application.Current.Resources["TextTertiaryBrush"];

        HighlightNavButton(NavTrain, _activeNav == "Train", accent, muted);
        HighlightNavButton(NavPrograms, _activeNav == "Programs", accent, muted);
        HighlightNavButton(NavLearn, _activeNav == "Learn", accent, muted);
        HighlightNavButton(NavHistory, _activeNav == "History", accent, muted);
        HighlightNavButton(NavProfile, _activeNav == "Profile", accent, muted);
    }

    private static void HighlightNavButton(Button button, bool active, Brush accent, Brush muted)
    {
        if (button.Content is StackPanel panel)
        {
            foreach (var child in panel.Children)
            {
                if (child is FontIcon icon)
                    icon.Foreground = active ? accent : muted;
                if (child is TextBlock text)
                    text.Foreground = active ? accent : muted;
            }
        }
    }
}