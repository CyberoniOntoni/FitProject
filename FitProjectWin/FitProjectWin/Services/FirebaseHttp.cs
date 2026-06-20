using System.Net.Http.Headers;
using System.Net.Http.Json;
using System.Text.Json;

namespace FitProjectWin.Services;

internal static class FirebaseHttp
{
    public static readonly HttpClient Client = new();

    public static async Task<HttpResponseMessage> GetAsync(string token, string url)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, url);
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return await Client.SendAsync(request);
    }

    public static async Task<JsonElement> GetFromJsonAsync(string token, string url)
    {
        var response = await GetAsync(token, url);
        response.EnsureSuccessStatusCode();
        var json = await response.Content.ReadFromJsonAsync<JsonElement>();
        if (json.ValueKind is JsonValueKind.Undefined or JsonValueKind.Null)
            throw new InvalidOperationException($"Empty response from {url}");
        return json;
    }

    public static async Task<HttpResponseMessage> PostAsJsonAsync(string token, string url, object body)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, url)
        {
            Content = JsonContent.Create(body)
        };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return await Client.SendAsync(request);
    }

    public static async Task<HttpResponseMessage> PatchAsJsonAsync(string token, string url, object body)
    {
        using var request = new HttpRequestMessage(HttpMethod.Patch, url)
        {
            Content = JsonContent.Create(body)
        };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return await Client.SendAsync(request);
    }

    public static async Task<HttpResponseMessage> PostAsync(string token, string url, HttpContent content)
    {
        using var request = new HttpRequestMessage(HttpMethod.Post, url) { Content = content };
        request.Headers.Authorization = new AuthenticationHeaderValue("Bearer", token);
        return await Client.SendAsync(request);
    }
}