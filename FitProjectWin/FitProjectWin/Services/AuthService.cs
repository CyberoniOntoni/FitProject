using System.Net.Http.Json;
using System.Text.Json;
using FitProjectWin.Models;

namespace FitProjectWin.Services;

public sealed class AuthService : System.ComponentModel.INotifyPropertyChanged
{
    private static readonly HttpClient Http = new();
    private string? _idToken;
    private string? _errorMessage;

    public FPUser? CurrentUser { get; private set; }
    public bool IsAuthenticated => CurrentUser is not null;
    public string? IdToken => _idToken;
    public string? ErrorMessage
    {
        get => _errorMessage;
        private set { _errorMessage = value; OnPropertyChanged(nameof(ErrorMessage)); }
    }

    public event Action? AuthStateChanged;
    public event System.ComponentModel.PropertyChangedEventHandler? PropertyChanged;
    private void OnPropertyChanged(string name) =>
        PropertyChanged?.Invoke(this, new System.ComponentModel.PropertyChangedEventArgs(name));

    public async Task<bool> SignInAsync(string email, string password)
    {
        ErrorMessage = null;
        try
        {
            var url = $"{FirebaseConfig.AuthBaseUrl}/accounts:signInWithPassword?key={FirebaseConfig.ApiKey}";
            var response = await Http.PostAsJsonAsync(url, new
            {
                email,
                password,
                returnSecureToken = true
            });

            if (!response.IsSuccessStatusCode)
            {
                var err = await response.Content.ReadAsStringAsync();
                ErrorMessage = ParseAuthError(err);
                return false;
            }

            var json = await response.Content.ReadFromJsonAsync<JsonElement>();
            _idToken = json.GetProperty("idToken").GetString();
            var uid = json.GetProperty("localId").GetString()!;

            var profile = await new FirestoreService(_idToken!).FetchUserProfileAsync(uid);
            CurrentUser = profile;
            AuthStateChanged?.Invoke();
            return true;
        }
        catch (Exception ex)
        {
            ErrorMessage = ex.Message;
            return false;
        }
    }

    public void SignOut()
    {
        _idToken = null;
        CurrentUser = null;
        AuthStateChanged?.Invoke();
    }

    private static string ParseAuthError(string json)
    {
        try
        {
            var el = JsonSerializer.Deserialize<JsonElement>(json);
            if (el.TryGetProperty("error", out var err) && err.TryGetProperty("message", out var msg))
            {
                return msg.GetString() switch
                {
                    "INVALID_PASSWORD" => "Incorrect password. Please try again.",
                    "EMAIL_NOT_FOUND" => "No account found with this email.",
                    "INVALID_EMAIL" => "Invalid email address.",
                    _ => msg.GetString() ?? "Authentication failed."
                };
            }
        }
        catch { }
        return "Authentication failed.";
    }
}