using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Media.Imaging;
using Windows.Storage.Pickers;
using WinRT.Interop;

namespace FitProjectWin.Views;

public sealed partial class AddProgressPhotoPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;
    private readonly FPProgressPhotoDraft _draft = new();
    private string _activePose = "front";

    public AddProgressPhotoPage()
    {
        InitializeComponent();
        SessionDate.Date = new DateTimeOffset(_draft.DateCreated);
        UpdatePoseHighlight();
        RefreshPreview();
    }

    private void Pose_Click(object sender, RoutedEventArgs e)
    {
        if (sender is Button btn && btn.Tag is string pose)
        {
            _activePose = pose;
            UpdatePoseHighlight();
            RefreshPreview();
        }
    }

    private void UpdatePoseHighlight()
    {
        HighlightPoseButton(FrontPoseBtn, _activePose == "front");
        HighlightPoseButton(SidePoseBtn, _activePose == "side");
        HighlightPoseButton(BackPoseBtn, _activePose == "back");
    }

    private static void HighlightPoseButton(Button btn, bool active)
    {
        btn.Background = (Brush)Application.Current.Resources[
            active ? "AccentBrush" : "SurfaceHighlightBrush"];
        btn.Foreground = active
            ? new Microsoft.UI.Xaml.Media.SolidColorBrush(Microsoft.UI.Colors.White)
            : (Brush)Application.Current.Resources["TextPrimaryBrush"];
    }

    private async void PickPhoto_Click(object sender, RoutedEventArgs e)
    {
        var picker = new FileOpenPicker
        {
            SuggestedStartLocation = PickerLocationId.PicturesLibrary
        };
        picker.FileTypeFilter.Add(".jpg");
        picker.FileTypeFilter.Add(".jpeg");
        picker.FileTypeFilter.Add(".png");
        picker.FileTypeFilter.Add(".webp");
        picker.FileTypeFilter.Add(".heic");

        if (App.MainWindow is not null)
        {
            var hwnd = WindowNative.GetWindowHandle(App.MainWindow);
            InitializeWithWindow.Initialize(picker, hwnd);
        }

        var file = await picker.PickSingleFileAsync();
        if (file is null) return;

        _draft.PoseLocalPaths[_activePose] = file.Path;
        RefreshPreview();
        UpdateCompletedPoses();
    }

    private void RefreshPreview()
    {
        if (_draft.PoseLocalPaths.TryGetValue(_activePose, out var path) && File.Exists(path))
        {
            PreviewImage.Source = new BitmapImage(new Uri(path));
            PreviewImage.Visibility = Visibility.Visible;
            PlaceholderText.Visibility = Visibility.Collapsed;
        }
        else
        {
            PreviewImage.Visibility = Visibility.Collapsed;
            PlaceholderText.Visibility = Visibility.Visible;
        }
    }

    private void UpdateCompletedPoses()
    {
        var completed = _draft.PoseLocalPaths.Count;
        StatusText.Text = completed == 0
            ? "Add at least one pose (front, side, or back)."
            : $"{completed} pose{(completed == 1 ? "" : "s")} ready — switch poses to add more.";
    }

    private async void Save_Click(object sender, RoutedEventArgs e)
    {
        if (_draft.PoseLocalPaths.Count == 0)
        {
            StatusText.Text = "Please add at least one photo before saving.";
            return;
        }

        _draft.DateCreated = SessionDate.Date.DateTime;
        _draft.Notes = NotesBox.Text.Trim();
        SaveBtn.IsEnabled = false;
        PickPhotoBtn.IsEnabled = false;
        StatusText.Text = "Uploading photos…";

        try
        {
            await ViewModel.Data.SaveProgressPhotoSessionAsync(_draft);
            App.NavigationService?.Navigate("ProgressPhotos");
        }
        catch (Exception ex)
        {
            StatusText.Text = $"Upload failed: {ex.Message}";
            SaveBtn.IsEnabled = true;
            PickPhotoBtn.IsEnabled = true;
        }
    }

    private void Back_Click(object sender, RoutedEventArgs e) =>
        App.NavigationService?.Navigate("ProgressPhotos");
}