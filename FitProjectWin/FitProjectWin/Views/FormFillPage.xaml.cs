using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Media;

namespace FitProjectWin.Views;

public sealed partial class FormFillPage : Page
{
    private FPForm? _form;
    private readonly Dictionary<string, string> _answers = new();

    public FormFillPage() => InitializeComponent();

    public void Bind(FPForm form)
    {
        _form = form;
        _answers.Clear();
        TitleText.Text = form.Title;
        FieldsPanel.Children.Clear();

        if (!string.IsNullOrEmpty(form.Description))
            FieldsPanel.Children.Add(new TextBlock
            {
                Text = form.Description,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap
            });

        foreach (var field in form.Fields)
            FieldsPanel.Children.Add(BuildField(field));
    }

    private UIElement BuildField(FPFormField field)
    {
        return field.Type switch
        {
            "Header" => new TextBlock
            {
                Text = field.Question.ToUpperInvariant(),
                FontWeight = Microsoft.UI.Text.FontWeights.Bold,
                FontSize = 13,
                Foreground = (Brush)Application.Current.Resources["AccentBrush"]!,
                CharacterSpacing = 120,
                Margin = new Thickness(0, 8, 0, 0)
            },
            "Divider" => new Border
            {
                Height = 1,
                Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!,
                Margin = new Thickness(0, 4, 0, 4)
            },
            "Paragraph" => new TextBlock
            {
                Text = field.Question,
                Style = (Style)Application.Current.Resources["CaptionStyle"],
                TextWrapping = TextWrapping.Wrap
            },
            "Rating" => BuildRatingField(field),
            "Linear Scale" => BuildScaleField(field),
            "Multiple Choice" => BuildChoiceField(field, multi: false),
            "Checkboxes" => BuildChoiceField(field, multi: true),
            "Number" => BuildTextField(field, NumberBoxSpinButtonPlacementMode.Inline),
            "Date" => BuildTextField(field, null, "YYYY-MM-DD"),
            _ => BuildTextField(field, null)
        };
    }

    private StackPanel BuildRatingField(FPFormField field)
    {
        var panel = FieldHeader(field);
        var row = new StackPanel { Orientation = Orientation.Horizontal, Spacing = 6 };
        for (var i = 1; i <= field.MaxRating; i++)
        {
            var rating = i;
            var btn = new Button
            {
                Content = i.ToString(),
                Width = 40,
                Height = 40,
                Tag = field.Id,
                Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!
            };
            btn.Click += (_, _) =>
            {
                _answers[field.Id] = rating.ToString();
                foreach (var child in row.Children.OfType<Button>())
                    child.Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!;
                btn.Background = (Brush)Application.Current.Resources["AccentBrush"]!;
            };
            row.Children.Add(btn);
        }
        panel.Children.Add(row);
        return panel;
    }

    private StackPanel BuildScaleField(FPFormField field)
    {
        var panel = FieldHeader(field);
        var slider = new Slider
        {
            Minimum = field.ScaleMin,
            Maximum = field.ScaleMax,
            StepFrequency = 1,
            Value = (field.ScaleMin + field.ScaleMax) / 2.0
        };
        var valueText = new TextBlock
        {
            Text = slider.Value.ToString("0"),
            Foreground = (Brush)Application.Current.Resources["AccentBrush"]!,
            FontWeight = Microsoft.UI.Text.FontWeights.Bold
        };
        slider.ValueChanged += (_, e) =>
        {
            valueText.Text = e.NewValue.ToString("0");
            _answers[field.Id] = valueText.Text;
        };
        _answers[field.Id] = slider.Value.ToString("0");

        var labels = new Grid();
        labels.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        labels.ColumnDefinitions.Add(new ColumnDefinition { Width = new GridLength(1, GridUnitType.Star) });
        var minLabel = new TextBlock
        {
            Text = field.ScaleMinLabel ?? field.ScaleMin.ToString(),
            Style = (Style)Application.Current.Resources["CaptionStyle"]
        };
        var maxLabel = new TextBlock
        {
            Text = field.ScaleMaxLabel ?? field.ScaleMax.ToString(),
            Style = (Style)Application.Current.Resources["CaptionStyle"],
            HorizontalAlignment = HorizontalAlignment.Right
        };
        Grid.SetColumn(maxLabel, 1);
        labels.Children.Add(minLabel);
        labels.Children.Add(maxLabel);

        panel.Children.Add(slider);
        panel.Children.Add(valueText);
        panel.Children.Add(labels);
        return panel;
    }

    private StackPanel BuildChoiceField(FPFormField field, bool multi)
    {
        var panel = FieldHeader(field);
        if (multi)
        {
            var checks = new StackPanel { Spacing = 6 };
            foreach (var option in field.Options)
            {
                var cb = new CheckBox
                {
                    Content = option,
                    Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
                };
                cb.Checked += (_, _) => UpdateMultiAnswer(field.Id, checks);
                cb.Unchecked += (_, _) => UpdateMultiAnswer(field.Id, checks);
                checks.Children.Add(cb);
            }
            panel.Children.Add(checks);
        }
        else
        {
            var combo = new ComboBox
            {
                HorizontalAlignment = HorizontalAlignment.Stretch,
                PlaceholderText = "Select an option"
            };
            foreach (var option in field.Options)
                combo.Items.Add(option);
            combo.SelectionChanged += (_, _) =>
            {
                if (combo.SelectedItem is string s)
                    _answers[field.Id] = s;
            };
            panel.Children.Add(combo);
        }
        return panel;
    }

    private StackPanel BuildTextField(FPFormField field, NumberBoxSpinButtonPlacementMode? spin, string? placeholder = null)
    {
        var panel = FieldHeader(field);
        if (spin.HasValue)
        {
            var box = new NumberBox
            {
                PlaceholderText = placeholder ?? "0",
                SpinButtonPlacementMode = spin.Value,
                HorizontalAlignment = HorizontalAlignment.Stretch,
                Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!
            };
            box.ValueChanged += (_, e) => _answers[field.Id] = e.NewValue.ToString("0.##");
            panel.Children.Add(box);
        }
        else
        {
            var box = new TextBox
            {
                PlaceholderText = placeholder ?? "Your answer",
                AcceptsReturn = field.Type == "Text",
                TextWrapping = field.Type == "Text" ? TextWrapping.Wrap : TextWrapping.NoWrap,
                MinHeight = field.Type == "Text" ? 80 : 40,
                Background = (Brush)Application.Current.Resources["SurfaceHighlightBrush"]!,
                Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!
            };
            box.TextChanged += (_, _) => _answers[field.Id] = box.Text;
            panel.Children.Add(box);
        }
        return panel;
    }

    private static StackPanel FieldHeader(FPFormField field)
    {
        var panel = new StackPanel { Spacing = 8 };
        if (!string.IsNullOrWhiteSpace(field.Question))
            panel.Children.Add(new TextBlock
            {
                Text = field.Question + (field.Required ? " *" : ""),
                FontWeight = Microsoft.UI.Text.FontWeights.SemiBold,
                Foreground = (Brush)Application.Current.Resources["TextPrimaryBrush"]!,
                TextWrapping = TextWrapping.Wrap
            });
        return panel;
    }

    private void UpdateMultiAnswer(string fieldId, StackPanel checks)
    {
        var selected = checks.Children.OfType<CheckBox>()
            .Where(c => c.IsChecked == true)
            .Select(c => c.Content?.ToString() ?? "")
            .Where(s => !string.IsNullOrEmpty(s));
        _answers[fieldId] = string.Join(", ", selected);
    }

    private async void Submit_Click(object sender, RoutedEventArgs e)
    {
        if (_form is null) return;

        var missing = _form.Fields
            .Where(f => f.Required && IsInputField(f.Type))
            .Where(f => !_answers.TryGetValue(f.Id, out var v) || string.IsNullOrWhiteSpace(v))
            .Select(f => f.Question)
            .ToList();

        if (missing.Count > 0)
        {
            var dialog = new ContentDialog
            {
                Title = "Required fields",
                Content = "Please complete: " + string.Join(", ", missing),
                CloseButtonText = "OK",
                XamlRoot = XamlRoot
            };
            await dialog.ShowAsync();
            return;
        }

        var answers = _form.Fields
            .Where(f => IsInputField(f.Type))
            .Where(f => _answers.ContainsKey(f.Id) && !string.IsNullOrWhiteSpace(_answers[f.Id]))
            .Select(f => new FPFormAnswer
            {
                FieldId = f.Id,
                Question = f.Question,
                Type = f.Type,
                Value = _answers[f.Id]
            }).ToList();

        var btn = (Button)sender;
        btn.IsEnabled = false;
        await App.ViewModel.Data.SubmitFormAsync(_form.Id, answers);
        btn.IsEnabled = true;
        App.ViewModel.CloseFormFill();
    }

    private static bool IsInputField(string type) =>
        type is not "Header" and not "Divider" and not "Paragraph";

    private void Back_Click(object sender, RoutedEventArgs e) => App.ViewModel.CloseFormFill();
}