using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using FitProjectWin.Models;
using FitProjectWin.Services;

namespace FitProjectWin.ViewModels;

public partial class MainViewModel : ObservableObject
{
    public AuthService Auth { get; }
    public AppDataService Data { get; }

    [ObservableProperty] private bool _isAuthenticated;
    [ObservableProperty] private string _loginEmail = "";
    [ObservableProperty] private string _loginPassword = "";
    [ObservableProperty] private bool _isLoading;
    [ObservableProperty] private bool _showWorkoutSession;
    [ObservableProperty] private WorkoutSessionViewModel? _workoutSession;
    [ObservableProperty] private bool _showFormFill;
    [ObservableProperty] private FPForm? _activeForm;
    [ObservableProperty] private FPContent? _activeContent;
    [ObservableProperty] private bool _showContentDetail;
    [ObservableProperty] private FPWorkoutLog? _activeWorkoutLog;
    [ObservableProperty] private bool _showWorkoutLogDetail;

    public MainViewModel()
    {
        Auth = new AuthService();
        Data = new AppDataService(Auth);
        Auth.AuthStateChanged += OnAuthChanged;
        Data.DataChanged += OnDataChanged;
        IsAuthenticated = Auth.IsAuthenticated;
    }

    private void OnAuthChanged()
    {
        IsAuthenticated = Auth.IsAuthenticated;
        if (!IsAuthenticated) Data.Clear();
    }

    private void OnDataChanged()
    {
        OnPropertyChanged(nameof(Greeting));
        OnPropertyChanged(nameof(TodayDate));
        OnPropertyChanged(nameof(WeeklyProgress));
        OnPropertyChanged(nameof(WeeklyProgressPercent));
        RefreshCommand.NotifyCanExecuteChanged();
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

    [RelayCommand(CanExecute = nameof(CanRefresh))]
    private async Task RefreshAsync()
    {
        await Data.FullSyncAsync();
        RefreshCommand.NotifyCanExecuteChanged();
    }

    private bool CanRefresh() => !Data.IsSyncing;

    [RelayCommand]
    private void StartWorkout(FPWorkout? workout)
    {
        if (workout is null) return;
        var program = Data.NextProgram ?? Data.Programs.FirstOrDefault(p => p.Id == workout.ProgramId);
        WorkoutSession = new WorkoutSessionViewModel(Data, Auth, workout, program);
        ShowWorkoutSession = true;
    }

    public void OpenForm(FPForm form)
    {
        ActiveForm = form;
        ShowFormFill = true;
    }

    public void CloseFormFill()
    {
        ShowFormFill = false;
        ActiveForm = null;
    }

    public void OpenContent(FPContent content)
    {
        ActiveContent = content;
        ShowContentDetail = true;
    }

    public void CloseContentDetail()
    {
        ShowContentDetail = false;
        ActiveContent = null;
    }

    public void OpenWorkoutLog(FPWorkoutLog log)
    {
        ActiveWorkoutLog = log;
        ShowWorkoutLogDetail = true;
    }

    public void CloseWorkoutLogDetail()
    {
        ShowWorkoutLogDetail = false;
        ActiveWorkoutLog = null;
    }

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