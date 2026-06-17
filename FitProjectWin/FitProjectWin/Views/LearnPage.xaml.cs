using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class LearnPage : Page
{
    public MainViewModel ViewModel { get; } = App.ViewModel;

    public LearnPage()
    {
        InitializeComponent();
        ViewModel.Data.DataChanged += Refresh;
        Refresh();
    }

    private void Refresh()
    {
        ContentList.Children.Clear();

        if (ViewModel.Data.Forms.Count > 0)
        {
            ContentList.Children.Add(new TextBlock
            {
                Text = "Check-In Forms",
                Style = (Style)Application.Current.Resources["SectionTitleStyle"]
            });
            foreach (var form in ViewModel.Data.Forms)
                ContentList.Children.Add(BuildFormCard(form));
        }

        if (ViewModel.Data.Content.Count > 0)
        {
            ContentList.Children.Add(new TextBlock
            {
                Text = "Guides & Articles",
                Style = (Style)Application.Current.Resources["SectionTitleStyle"],
                Margin = new Thickness(0, 8, 0, 0)
            });
            foreach (var item in ViewModel.Data.Content)
                ContentList.Children.Add(BuildContentCard(item));
        }

        if (ViewModel.Data.Forms.Count == 0 && ViewModel.Data.Content.Count == 0)
        {
            ContentList.Children.Add(new TextBlock
            {
                Text = "No content yet. Guides and forms from your coach on FitPros.io will appear here.",
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap,
                Margin = new Thickness(0, 40, 0, 0)
            });
        }
    }

    private Border BuildFormCard(FPForm form)
    {
        var completed = form.IsCompletedFor(ViewModel.Auth.CurrentUser?.Id ?? "");
        var panel = new StackPanel { Spacing = 4 };
        panel.Children.Add(new TextBlock
        {
            Text = form.Title,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
        });
        if (!string.IsNullOrEmpty(form.Description))
            panel.Children.Add(new TextBlock
            {
                Text = form.Description,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap
            });
        panel.Children.Add(new TextBlock
        {
            Text = completed ? "✓ Completed" : "Tap to fill out",
            Foreground = completed
                ? (Brush)Application.Current.Resources["SuccessBrush"]!
                : (Brush)Application.Current.Resources["WarningBrush"]!,
            FontSize = 12
        });

        var btn = new Button
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            HorizontalContentAlignment = HorizontalAlignment.Stretch,
            Background = (Brush)Application.Current.Resources["SurfaceBrush"]!,
            BorderBrush = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!,
            BorderThickness = new Thickness(1),
            CornerRadius = new CornerRadius(12),
            Padding = new Thickness(16),
            Content = panel,
            Margin = new Thickness(0, 0, 0, 8)
        };
        btn.Click += (_, _) => App.ViewModel.OpenForm(form);
        return new Border { Child = btn };
    }

    private Border BuildContentCard(FPContent item)
    {
        var panel = new StackPanel { Spacing = 4 };
        panel.Children.Add(new TextBlock
        {
            Text = item.Title,
            FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
            Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
        });
        panel.Children.Add(new TextBlock
        {
            Text = item.Type,
            Foreground = (Brush)Application.Current.Resources["AccentBrush"]!,
            FontSize = 12
        });

        var btn = new Button
        {
            HorizontalAlignment = HorizontalAlignment.Stretch,
            HorizontalContentAlignment = HorizontalAlignment.Stretch,
            Background = (Brush)Application.Current.Resources["SurfaceBrush"]!,
            BorderBrush = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!,
            BorderThickness = new Thickness(1),
            CornerRadius = new CornerRadius(12),
            Padding = new Thickness(16),
            Content = panel,
            Margin = new Thickness(0, 0, 0, 8)
        };
        btn.Click += (_, _) => App.ViewModel.OpenContent(item);
        return new Border { Child = btn };
    }
}