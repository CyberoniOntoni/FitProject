namespace FitProjectWin.Services;

public sealed class NavigationService(Action<string> navigate)
{
    public void Navigate(string pageTag) => navigate(pageTag);
}