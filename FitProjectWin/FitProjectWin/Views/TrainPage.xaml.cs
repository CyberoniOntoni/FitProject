using FitProjectWin.Models;
using Microsoft.UI.Xaml.Controls;

namespace FitProjectWin.Views;

public sealed partial class TrainPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public TrainPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += () => Bindings.Update();
    }

    private async void HabitMinus_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        if ((sender as Button)?.Tag is FPHabit habit)
            await ViewModel.Data.UpdateHabitAsync(habit.Id, Math.Max(0, habit.CurrentValue - 1));
    }

    private async void HabitPlus_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e)
    {
        if ((sender as Button)?.Tag is FPHabit habit)
            await ViewModel.Data.UpdateHabitAsync(habit.Id, habit.CurrentValue + 1);
    }

    private void OpenHabits_Click(object sender, Microsoft.UI.Xaml.RoutedEventArgs e) =>
        App.NavigationService?.Navigate("Habits");
}