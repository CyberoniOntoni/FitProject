using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using FitProjectWin.Models;
using FitProjectWin.Services;

namespace FitProjectWin.ViewModels;

public partial class MainViewModel : ObservableObject
{
    public AuthService Auth { get; }
    public AppDataService Data { get; }

    [ObservableProperty] private AppTab _selectedTab = AppTab.Train;
    [ObservableProperty] private bool _isAuthenticated;
    [ObservableProperty] private string _loginEmail = "";
    [ObservableProperty] private string _loginPassword = "";
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private bool _showWorkoutSession;
    [ObservableProperty] private WorkoutSessionViewModel? _workoutSession;
    [ObservableProperty] private bool _showVideoPlayer;
    [ObservableProperty] private string _videoYoutubeId = "";
    [ObservableProperty] private string _videoTitle = "";

    public List<NavItem> NavItems { get; } =
    [
        new() { Tab = AppTab.Train, Label = "Train", Icon = "\uE7B3" },
        new() { Tab = AppTab.Programs, Label = "Programs", Icon = "\uE8FD" },
        new() { Tab = AppTab.Learn, Label = "Learn", Icon = "\uE82D" },
        new() { Tab = AppTab.History, Label = "History", Icon = "\uE81C" }
    ];

    public MainViewModel()
    {
        Auth = new AuthService();
        Data = new AppDataService(Auth);
        Auth.AuthStateChanged += OnAuthChanged;
        Data.DataChanged += () => OnPropertyChanged(string.Empty);
        IsAuthenticated = Auth.IsAuthenticated;
    }

    private void OnAuthChanged()
    {
        IsAuthenticated = Auth.IsAuthenticated;
        if (!IsAuthenticated) Data.Clear();
    }

    [RelayCommand]
    private async Task SignInAsync()
    {
        IsLoading = true;
        var ok = await Auth.SignInAsync(LoginEmail, LoginPassword);
        if (ok) await Data.FullSyncAsync();
        IsLoading = false;
    }

    [RelayCommand]
    private void SignOut()
    {
        Auth.SignOut();
        LoginPassword = "";
    }

    [RelayCommand]
    private async Task RefreshAsync() => await Data.FullSyncAsync();

    [RelayCommand]
    private void NavigateTo(AppTab tab) => SelectedTab = tab;

    [RelayCommand]
    private void StartWorkout(FPWorkout? workout)
    {
        if (workout is null) return;
        var program = Data.NextProgram ?? Data.Programs.FirstOrDefault(p => p.Id == workout.ProgramId);
        WorkoutSession = new WorkoutSessionViewModel(Data, Auth, workout, program);
        ShowWorkoutSession = true;
    }

    public void PlayExerciseVideo(string? youtubeId, string title)
    {
        if (string.IsNullOrWhiteSpace(youtubeId)) return;
        VideoYoutubeId = youtubeId;
        VideoTitle = title;
        ShowVideoPlayer = true;
    }

    [RelayCommand]
    private void CloseVideoPlayer() => ShowVideoPlayer = false;

    [RelayCommand]
    private async Task CloseWorkoutSessionAsync()
    {
        ShowWorkoutSession = false;
        WorkoutSession?.Dispose();
        WorkoutSession = null;
        await Data.FullSyncAsync();
    }

    public string Greeting
    {
        get
        {
            var hour = DateTime.Now.Hour;
            var baseGreeting = hour switch
            {
                >= 5 and < 12 => "Good morning",
                >= 12 and < 17 => "Good afternoon",
                _ => "Good evening"
            };
            var name = Auth.CurrentUser?.FirstName;
            return string.IsNullOrEmpty(name) ? baseGreeting : $"{baseGreeting}, {name}";
        }
    }

    public string TodayDate => DateTime.Now.ToString("dddd, MMMM d");
    public double WeeklyProgress => Data.WeeklyWorkoutGoal > 0
        ? (double)Data.WeeklyWorkoutsCompleted / Data.WeeklyWorkoutGoal : 0;

    public double WeeklyProgressPercent => WeeklyProgress * 100;
}