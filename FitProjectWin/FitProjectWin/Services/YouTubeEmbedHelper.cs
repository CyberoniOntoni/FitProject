using Microsoft.UI.Xaml.Controls;
using Microsoft.Web.WebView2.Core;

namespace FitProjectWin.Services;

public static class YouTubeEmbedHelper
{
    private const string HostName = "fitproject.local";

    private static string WebFolder =>
        Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "FitProjectWin",
            "webview");

    public static string BuildEmbedHtml(string youtubeId, bool autoplay = true)
    {
        var autoplayParam = autoplay ? "1" : "0";
        return $$"""
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="referrer" content="strict-origin-when-cross-origin">
<style>
html,body{margin:0;padding:0;width:100%;height:100%;background:#000;overflow:hidden}
iframe{position:absolute;inset:0;width:100%;height:100%;border:0}
</style>
</head>
<body>
<iframe
  src="https://www.youtube-nocookie.com/embed/{{youtubeId}}?autoplay={{autoplayParam}}&rel=0&modestbranding=1&playsinline=1&enablejsapi=1"
  title="Exercise video"
  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
  referrerpolicy="strict-origin-when-cross-origin"
  allowfullscreen>
</iframe>
</body>
</html>
""";
    }

    public static async Task LoadVideoAsync(WebView2 webView, string youtubeId)
    {
        await webView.EnsureCoreWebView2Async();
        var core = webView.CoreWebView2
            ?? throw new InvalidOperationException("WebView2 is not initialized.");

        var folder = WebFolder;
        Directory.CreateDirectory(folder);
        core.SetVirtualHostNameToFolderMapping(
            HostName, folder, CoreWebView2HostResourceAccessKind.Allow);

        var htmlPath = Path.Combine(folder, "player.html");
        await File.WriteAllTextAsync(htmlPath, BuildEmbedHtml(youtubeId));
        webView.Source = new Uri($"https://{HostName}/player.html");
    }

    public static void StopVideo(WebView2 webView)
    {
        try
        {
            if (webView.CoreWebView2 is not null)
                webView.CoreWebView2.Navigate("about:blank");
        }
        catch { }
    }
}