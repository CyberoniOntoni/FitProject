using FitProjectWin.Models;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Shapes;
using Windows.Foundation;
using Windows.UI;

namespace FitProjectWin.Controls;

public sealed class WeightTrendChartRenderer
{
    private static readonly Color ChartColor = Color.FromArgb(255, 0xC9, 0x34, 0x77);

    private readonly Canvas _canvas;
    private readonly TextBlock _valueText;
    private readonly TextBlock _unitText;
    private readonly TextBlock _dateText;
    private readonly TextBlock _deltaText;

    private readonly List<WeightTrendEntry> _entries = [];
    private int _selectedIndex;
    private string _unitLabel = "kg";

    private const double LeftPad = 36;
    private const double RightPad = 12;
    private const double TopPad = 14;
    private const double BottomPad = 28;

    public WeightTrendChartRenderer(
        Canvas canvas,
        TextBlock valueText,
        TextBlock unitText,
        TextBlock dateText,
        TextBlock deltaText)
    {
        _canvas = canvas;
        _valueText = valueText;
        _unitText = unitText;
        _dateText = dateText;
        _deltaText = deltaText;

        _canvas.PointerPressed += OnPointerPressed;
        _canvas.PointerMoved += OnPointerMoved;
        _canvas.SizeChanged += (_, _) => SafeRedraw();
    }

    public bool Update(IReadOnlyList<FPMeasurement> measurements, FPUnitPreferences prefs)
    {
        _entries.Clear();
        var bodyweightType = MeasurementCatalog.FindById("DfqsrFQBGi04aHWAPA7I");
        if (bodyweightType is null)
            return false;

        var weightLogs = measurements
            .Where(m =>
                m.TypeId == "DfqsrFQBGi04aHWAPA7I" ||
                m.Name.Contains("bodyweight", StringComparison.OrdinalIgnoreCase) ||
                (m.Name.Contains("weight", StringComparison.OrdinalIgnoreCase) &&
                 !m.Name.Contains("body fat", StringComparison.OrdinalIgnoreCase)))
            .OrderBy(m => m.Date)
            .TakeLast(24)
            .ToList();

        if (weightLogs.Count < 2)
            return false;

        _unitLabel = bodyweightType.Category switch
        {
            "Circumference" => UnitConversionHelper.CircumferenceAbbreviation(prefs.Circumference),
            "Body Composition" when bodyweightType.UnitType == "MASS" =>
                UnitConversionHelper.MassAbbreviation(prefs.Mass),
            _ => bodyweightType.DisplayUnit
        };

        _entries.AddRange(weightLogs.Select(m => new WeightTrendEntry
        {
            Date = m.Date,
            DisplayValue = UnitConversionHelper.ConvertMassForDisplay(m.Value, prefs.Mass),
            FormattedValue = FormatDisplayValue(m.Value, bodyweightType, prefs)
        }));
        _selectedIndex = _entries.Count - 1;

        SafeUpdateSummary();
        SafeRedraw();
        return true;
    }

    private void OnPointerPressed(object sender, PointerRoutedEventArgs e) =>
        UpdateSelectionAt(e.GetCurrentPoint(_canvas).Position.X);

    private void OnPointerMoved(object sender, PointerRoutedEventArgs e)
    {
        if (e.Pointer.IsInContact)
            UpdateSelectionAt(e.GetCurrentPoint(_canvas).Position.X);
    }

    private void UpdateSelectionAt(double x)
    {
        if (_entries.Count == 0) return;
        _selectedIndex = IndexAt(x, _canvas.ActualWidth);
        SafeUpdateSummary();
        SafeRedraw();
    }

    private int IndexAt(double x, double width)
    {
        if (_entries.Count <= 1) return 0;
        var chartWidth = Math.Max(width - LeftPad - RightPad, 1);
        var step = chartWidth / (_entries.Count - 1);
        return Math.Clamp((int)Math.Round((x - LeftPad) / step), 0, _entries.Count - 1);
    }

    private void SafeUpdateSummary()
    {
        try { UpdateSummary(); }
        catch { /* keep page usable */ }
    }

    private void SafeRedraw()
    {
        try { Redraw(); }
        catch { _canvas.Children.Clear(); }
    }

    private void UpdateSummary()
    {
        if (_entries.Count == 0) return;

        var selected = _entries[_selectedIndex];
        _valueText.Text = selected.FormattedValue;
        _valueText.Foreground = new SolidColorBrush(ChartColor);
        _unitText.Text = _unitLabel;
        _dateText.Text = selected.Date.ToLocalTime().ToString("MMMM d, yyyy 'at' h:mm tt");

        if (_selectedIndex > 0)
        {
            var delta = selected.DisplayValue - _entries[_selectedIndex - 1].DisplayValue;
            var sign = delta >= 0 ? "+" : "";
            _deltaText.Text = Math.Abs(delta % 1) < 0.05
                ? $"{sign}{delta:0} {_unitLabel}"
                : $"{sign}{delta:0.0} {_unitLabel}";
            _deltaText.Foreground = Brush(delta switch
            {
                > 0 => "WarningBrush",
                < 0 => "SuccessBrush",
                _ => "TextTertiaryBrush"
            });
            _deltaText.Visibility = Visibility.Visible;
        }
        else
        {
            _deltaText.Visibility = Visibility.Collapsed;
        }
    }

    private void Redraw()
    {
        _canvas.Children.Clear();
        if (_entries.Count < 2 || _canvas.ActualWidth <= 0) return;

        var values = _entries.Select(e => e.DisplayValue).ToList();
        var min = values.Min();
        var max = values.Max();
        var range = Math.Max(max - min, 0.5);

        var width = _canvas.ActualWidth;
        var height = _canvas.ActualHeight;
        var chartWidth = width - LeftPad - RightPad;
        var chartHeight = height - TopPad - BottomPad;

        Point PointAt(int index)
        {
            var x = LeftPad + chartWidth * index / Math.Max(_entries.Count - 1, 1);
            var normalized = (values[index] - min) / range;
            var y = TopPad + chartHeight * (1 - normalized);
            return new Point(x, y);
        }

        var points = Enumerable.Range(0, _entries.Count).Select(PointAt).ToList();
        var baseline = TopPad + chartHeight;

        for (var i = 0; i <= 3; i++)
        {
            var y = TopPad + chartHeight * i / 3d;
            _canvas.Children.Add(new Line
            {
                X1 = LeftPad,
                Y1 = y,
                X2 = width - RightPad,
                Y2 = y,
                Stroke = new SolidColorBrush(Color.FromArgb(15, 255, 255, 255)),
                StrokeThickness = 1
            });
        }

        var fillPoints = new PointCollection { new(LeftPad, baseline) };
        foreach (var p in points) fillPoints.Add(p);
        fillPoints.Add(new Point(points[^1].X, baseline));
        _canvas.Children.Add(new Polygon
        {
            Points = fillPoints,
            Fill = new SolidColorBrush(Color.FromArgb(40, 0xC9, 0x34, 0x77))
        });

        var linePoints = new PointCollection();
        foreach (var p in points) linePoints.Add(p);
        _canvas.Children.Add(new Polyline
        {
            Points = linePoints,
            Stroke = new SolidColorBrush(ChartColor),
            StrokeThickness = 2.5,
            StrokeLineJoin = PenLineJoin.Round
        });

        var selectedPoint = points[_selectedIndex];
        _canvas.Children.Add(new Line
        {
            X1 = selectedPoint.X,
            Y1 = TopPad,
            X2 = selectedPoint.X,
            Y2 = baseline,
            Stroke = new SolidColorBrush(Color.FromArgb(89, 0xC9, 0x34, 0x77)),
            StrokeThickness = 1.5,
            StrokeDashArray = new DoubleCollection { 4, 4 }
        });

        for (var index = 0; index < points.Count; index++)
        {
            var point = points[index];
            var isSelected = index == _selectedIndex;
            var radius = isSelected ? 7d : 4d;
            byte alpha = isSelected ? (byte)255 : index <= _selectedIndex ? (byte)217 : (byte)89;

            var dot = new Ellipse
            {
                Width = radius * 2,
                Height = radius * 2,
                Fill = new SolidColorBrush(Color.FromArgb(alpha, 0xC9, 0x34, 0x77))
            };
            _canvas.Children.Add(dot);
            Canvas.SetLeft(dot, point.X - radius);
            Canvas.SetTop(dot, point.Y - radius);
        }

        var yLabels = new[] { max, min + range / 2, min };
        for (var i = 0; i < yLabels.Length; i++)
        {
            var y = TopPad + chartHeight * i / 2d;
            var label = Math.Abs(yLabels[i] % 1) < 0.05 ? $"{yLabels[i]:0}" : $"{yLabels[i]:0.0}";
            var text = new TextBlock
            {
                Text = label,
                FontSize = 10,
                Foreground = Brush("TextSecondaryBrush")
            };
            _canvas.Children.Add(text);
            Canvas.SetLeft(text, 4);
            Canvas.SetTop(text, y - 7);
        }

        foreach (var index in new[] { 0, _entries.Count / 2, _entries.Count - 1 }.Distinct())
        {
            var point = points[index];
            var label = _entries[index].Date.ToLocalTime().ToString("MMM d");
            var text = new TextBlock
            {
                Text = label,
                FontSize = 10,
                Foreground = Brush("TextTertiaryBrush")
            };
            _canvas.Children.Add(text);
            Canvas.SetLeft(text, point.X - label.Length * 2.5);
            Canvas.SetTop(text, height - 16);
        }
    }

    private static string FormatDisplayValue(double canonical, MeasurementTypeDef type, FPUnitPreferences prefs)
    {
        var display = type.Category switch
        {
            "Circumference" => UnitConversionHelper.ConvertCircumferenceForDisplay(canonical, prefs.Circumference),
            "Body Composition" when type.UnitType == "MASS" =>
                UnitConversionHelper.ConvertMassForDisplay(canonical, prefs.Mass),
            _ => canonical
        };
        return UnitConversionHelper.FormatDisplayNumber(display);
    }

    private static Brush Brush(string key) =>
        (Brush)Application.Current.Resources[key]!;

    private sealed class WeightTrendEntry
    {
        public DateTime Date { get; init; }
        public double DisplayValue { get; init; }
        public string FormattedValue { get; init; } = "";
    }
}