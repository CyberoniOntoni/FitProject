using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace FitProjectWin.Services;

public sealed class FirebaseStorageService
{
    private readonly HttpClient _http;
    private readonly string _token;

    public FirebaseStorageService(string token)
    {
        _token = token;
        _http = new HttpClient();
        _http.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", token);
    }

    public async Task<string> UploadProgressPhotoAsync(string userId, string sessionId, string poseType, string filePath)
    {
        var bytes = await File.ReadAllBytesAsync(filePath);
        var contentType = GuessContentType(filePath);
        var extension = contentType switch
        {
            "image/png" => "png",
            "image/webp" => "webp",
            _ => "jpg"
        };

        var objectPath = $"progressPictures/{userId}/session_{sessionId}_{poseType}.{extension}";
        var encodedPath = Uri.EscapeDataString(objectPath);
        var url = $"{FirebaseConfig.StorageBaseUrl}?uploadType=media&name={encodedPath}";

        using var content = new ByteArrayContent(bytes);
        content.Headers.ContentType = new MediaTypeHeaderValue(contentType);
        var response = await _http.PostAsync(url, content);
        response.EnsureSuccessStatusCode();

        var json = await response.Content.ReadFromJsonAsync<JsonElement>();
        return BuildDownloadUrl(objectPath, json);
    }

    private static string GuessContentType(string path)
    {
        var ext = Path.GetExtension(path).ToLowerInvariant();
        return ext switch
        {
            ".png" => "image/png",
            ".webp" => "image/webp",
            ".heic" => "image/heic",
            _ => "image/jpeg"
        };
    }

    private static string BuildDownloadUrl(string objectPath, JsonElement uploadResponse)
    {
        if (uploadResponse.TryGetProperty("downloadTokens", out var tokens))
        {
            var token = tokens.GetString();
            if (!string.IsNullOrEmpty(token))
            {
                var encoded = Uri.EscapeDataString(objectPath);
                return $"{FirebaseConfig.StorageBaseUrl}/{encoded}?alt=media&token={token}";
            }
        }

        if (uploadResponse.TryGetProperty("mediaLink", out var mediaLink))
            return mediaLink.GetString() ?? "";

        var fallback = Uri.EscapeDataString(objectPath);
        return $"{FirebaseConfig.StorageBaseUrl}/{fallback}?alt=media";
    }
}