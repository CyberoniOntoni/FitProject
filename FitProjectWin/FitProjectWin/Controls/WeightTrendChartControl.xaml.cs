using FitProjectWin.Models;
using Microsoft.UI;
using Microsoft.UI.Xaml;
using Microsoft.UI.Xaml.Controls;
using Microsoft.UI.Xaml.Input;
using Microsoft.UI.Xaml.Media;
using Microsoft.UI.Xaml.Shapes;
using ShapePath = Microsoft.UI.Xaml.Shapes.Path;
using Windows.Foundation;
using Windows.UI;

namespace FitProjectWin.Controls;

public sealed partial class WeightTrendChartControl : UserControl
{
    private static readonly Color BodyweightChartColor = Color.FromArgb(255, 0xC9, 0x34, 0x77);
    private readonly List<WeightTrendEntry> _entries = [];
    private int _selectedIndex;
    private string _unitLabel = "kg";
    private bool _isLoaded;
    private IReadOnlyList<FPMeasurement>? _pendingMeasurements;
    private FPUnitPreferences? _pendingPrefs;

    private const float LeftPad = 36f;
    private const float RightPad = 12f;
    private const float TopPad = 14f;
    private const float BottomPad = 28f;

    public WeightTrendChartControl()
    {
        InitializeComponent();
        Loaded += OnLoaded;
        SizeChanged += (_, _) => RedrawChart();
    }

    private void OnLoaded(object sender, RoutedEventArgs e)
    {
        _isLoaded = true;
        _pendingMeasurements = null;
        _pendingPrefs = null;

        if (_entries.Count >= 2)
        {
            UpdateSummary();
            RedrawChart();
        }
    }

    public void UpdateMeasurements(IReadOnlyList<FPMeasurement> measurements, FPUnitPreferences prefs)
    {
        if (!TryPrepareEntries(measurements, prefs))
            return;

        if (!_isLoaded)
        {
            _pendingMeasurements = measurements;
            _pendingPrefs = prefs;
            return;
        }

        UpdateSummary();
        RedrawChart();
    }

    private bool TryPrepareEntries(IReadOnlyList<FPMeasurement> measurements, FPUnitPreferences prefs)
    {
        var bodyweightType = MeasurementCatalog.FindById("DfqsrFQBGi04aHWAPA7I");
        _entries.Clear();

        if (bodyweightType is null)
        {
            Visibility = Visibility.Collapsed;
            return false;
        }

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
        {
            Visibility = Visibility.Collapsed;
            return false;
        }

        Visibility = Visibility.Visible;
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
            CanonicalValue = m.Value,
            DisplayValue = UnitConversionHelper.ConvertMassForDisplay(m.Value, prefs.Mass),
            FormattedValue = FormatDisplayValue(m.Value, bodyweightType, prefs)
        }));
        _selectedIndex = _entries.Count - 1;
        return true;
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

    private void ChartCanvas_PointerPressed(object sender, PointerRoutedEventArgs e) =>
        UpdateSelectionAt(e.GetCurrentPoint(ChartCanvas).Position.X);

    private void ChartCanvas_PointerMoved(object sender, PointerRoutedEventArgs e)
    {
        if (e.Pointer.IsInContact)
            UpdateSelectionAt(e.GetCurrentPoint(ChartCanvas).Position.X);
    }

    private void UpdateSelectionAt(double x)
    {
        if (_entries.Count == 0) return;
        _selectedIndex = IndexAt(x, ChartCanvas.ActualWidth);
        UpdateSummary();
        RedrawChart();
    }

    private int IndexAt(double x, double width)
    {
        if (_entries.Count <= 1) return 0;
        var chartWidth = Math.Max(width - LeftPad - RightPad, 1);
        var step = chartWidth / (_entries.Count - 1);
        return Math.Clamp((int)Math.Round((x - LeftPad) / step), 0, _entries.Count - 1);
    }

    private void UpdateSummary()
    {
        if (_entries.Count == 0) return;

        var selected = _entries[_selectedIndex];
        SelectedValueText.Text = selected.FormattedValue;
        SelectedValueText.Foreground = new SolidColorBrush(BodyweightChartColor);
        SelectedUnitText.Text = _unitLabel;
        SelectedDateText.Text = selected.Date.ToLocalTime().ToString("MMMM d, yyyy 'at' h:mm tt");

        if (_selectedIndex > 0)
        {
            var delta = selected.DisplayValue - _entries[_selectedIndex - 1].DisplayValue;
            var sign = delta >= 0 ? "+" : "";
            var deltaText = Math.Abs(delta % 1) < 0.05
                ? $"{sign}{delta:0} {_unitLabel}"
                : $"{sign}{delta:0.0} {_unitLabel}";
            SelectedDeltaText.Text = deltaText;
            SelectedDeltaText.Foreground = ResourceBrush(delta switch
            {
                > 0 => "WarningBrush",
                < 0 => "SuccessBrush",
                _ => "TextTertiaryBrush"
            });
            SelectedDeltaText.Visibility = Visibility.Visible;
        }
        else
        {
            SelectedDeltaText.Visibility = Visibility.Collapsed;
        }
    }

    private void RedrawChart()
    {
        ChartCanvas.Children.Clear();
        if (_entries.Count < 2 || ChartCanvas.ActualWidth <= 0) return;

        var displayValues = _entries.Select(e => e.DisplayValue).ToList();
        var minDisplay = displayValues.Min();
        var maxDisplay = displayValues.Max();
        var displayRange = Math.Max(maxDisplay - minDisplay, 0.5);

        var width = ChartCanvas.ActualWidth;
        var height = ChartCanvas.ActualHeight;
        var chartWidth = width - LeftPad - RightPad;
        var chartHeight = height - TopPad - BottomPad;

        Point PointAt(int index)
        {
            var x = LeftPad + chartWidth * index / Math.Max(_entries.Count - 1, 1);
            var normalized = (displayValues[index] - minDisplay) / displayRange;
            var y = TopPad + chartHeight * (1 - normalized);
            return new Point(x, y);
        }

        var points = Enumerable.Range(0, _entries.Count).Select(PointAt).ToList();

        for (var i = 0; i <= 3; i++)
        {
            var y = TopPad + chartHeight * i / 3d;
            ChartCanvas.Children.Add(new Line
            {
                X1 = LeftPad,
                Y1 = y,
                X2 = width - RightPad,
                Y2 = y,
                Stroke = new SolidColorBrush(Color.FromArgb(15, 255, 255, 255)),
                StrokeThickness = 1
            });
        }

        var fillGeometry = BuildSmoothAreaGeometry(points, TopPad + chartHeight);
        ChartCanvas.Children.Add(new ShapePath
        {
            Data = fillGeometry,
            Fill = new LinearGradientBrush
            {
                StartPoint = new Point(0, 0),
                EndPoint = new Point(0, 1),
                GradientStops =
                {
                    new GradientStop { Color = Color.FromArgb(71, 0xC9, 0x34, 0x77), Offset = 0 },
                    new GradientStop { Color = Color.FromArgb(5, 0xC9, 0x34, 0x77), Offset = 1 }
                }
            }
        });

        var lineGeometry = BuildSmoothLineGeometry(points);
        ChartCanvas.Children.Add(new ShapePath
        {
            Data = lineGeometry,
            Stroke = new SolidColorBrush(Color.FromArgb(31, 255, 255, 255)),
            StrokeThickness = 5,
            StrokeLineJoin = PenLineJoin.Round,
            StrokeStartLineCap = PenLineCap.Round,
            StrokeEndLineCap = PenLineCap.Round
        });
        ChartCanvas.Children.Add(new ShapePath
        {
            Data = lineGeometry,
            Stroke = new LinearGradientBrush
            {
                StartPoint = new Point(0, 0.5),
                EndPoint = new Point(1, 0.5),
                GradientStops =
                {
                    new GradientStop { Color = Color.FromArgb(166, 0xC9, 0x34, 0x77), Offset = 0 },
                    new GradientStop { Color = BodyweightChartColor, Offset = 0.5 },
                    new GradientStop { Color = Color.FromArgb(217, 0x3B, 0x82, 0xF6), Offset = 1 }
                }
            },
            StrokeThickness = 2.5,
            StrokeLineJoin = PenLineJoin.Round,
            StrokeStartLineCap = PenLineCap.Round,
            StrokeEndLineCap = PenLineCap.Round
        });

        if (_selectedIndex < points.Count - 1)
        {
            ChartCanvas.Children.Add(new ShapePath
            {
                Data = BuildStraightLineGeometry(points.Skip(_selectedIndex).ToList()),
                Stroke = new SolidColorBrush(Color.FromArgb(26, 255, 255, 255)),
                StrokeThickness = 3,
                StrokeLineJoin = PenLineJoin.Round,
                StrokeStartLineCap = PenLineCap.Round,
                StrokeEndLineCap = PenLineCap.Round
            });
        }

        if (_selectedIndex > 0)
        {
            var highlightPoints = points.Take(_selectedIndex + 1).ToList();
            ChartCanvas.Children.Add(new ShapePath
            {
                Data = BuildStraightLineGeometry(highlightPoints),
                Stroke = new SolidColorBrush(Color.FromArgb(46, 0xC9, 0x34, 0x77)),
                StrokeThickness = 12,
                StrokeLineJoin = PenLineJoin.Round,
                StrokeStartLineCap = PenLineCap.Round,
                StrokeEndLineCap = PenLineCap.Round
            });
            ChartCanvas.Children.Add(new ShapePath
            {
                Data = BuildStraightLineGeometry(highlightPoints),
                Stroke = new LinearGradientBrush
                {
                    StartPoint = new Point(0, 0.5),
                    EndPoint = new Point(1, 0.5),
                    GradientStops =
                    {
                        new GradientStop { Color = Color.FromArgb(179, 0xC9, 0x34, 0x77), Offset = 0 },
                        new GradientStop { Color = BodyweightChartColor, Offset = 1 }
                    }
                },
                StrokeThickness = 4,
                StrokeLineJoin = PenLineJoin.Round,
                StrokeStartLineCap = PenLineCap.Round,
                StrokeEndLineCap = PenLineCap.Round
            });
        }

        var selectedPoint = points[_selectedIndex];
        ChartCanvas.Children.Add(new Line
        {
            X1 = selectedPoint.X,
            Y1 = TopPad,
            X2 = selectedPoint.X,
            Y2 = TopPad + chartHeight,
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

            if (isSelected)
            {
                ChartCanvas.Children.Add(new Ellipse
                {
                    Width = 28,
                    Height = 28,
                    Fill = new SolidColorBrush(Color.FromArgb(64, 0xC9, 0x34, 0x77))
                });
                Canvas.SetLeft(ChartCanvas.Children[^1], point.X - 14);
                Canvas.SetTop(ChartCanvas.Children[^1], point.Y - 14);
            }

            ChartCanvas.Children.Add(new Ellipse
            {
                Width = radius * 2,
                Height = radius * 2,
                Fill = new SolidColorBrush(Color.FromArgb(alpha, 0xC9, 0x34, 0x77))
            });
            Canvas.SetLeft(ChartCanvas.Children[^1], point.X - radius);
            Canvas.SetTop(ChartCanvas.Children[^1], point.Y - radius);

            ChartCanvas.Children.Add(new Ellipse
            {
                Width = radius * 0.9,
                Height = radius * 0.9,
                Fill = new SolidColorBrush(Color.FromArgb(isSelected ? (byte)242 : (byte)179, 255, 255, 255))
            });
            Canvas.SetLeft(ChartCanvas.Children[^1], point.X - radius * 0.45);
            Canvas.SetTop(ChartCanvas.Children[^1], point.Y - radius * 0.45);
        }

        var yLabels = new[] { maxDisplay, minDisplay + displayRange / 2, minDisplay };
        for (var i = 0; i < yLabels.Length; i++)
        {
            var y = TopPad + chartHeight * i / 2d;
            var label = Math.Abs(yLabels[i] % 1) < 0.05 ? $"{yLabels[i]:0}" : $"{yLabels[i]:0.0}";
            var text = new TextBlock
            {
                Text = label,
                FontSize = 10,
                Foreground = ResourceBrush("TextSecondaryBrush")
            };
            ChartCanvas.Children.Add(text);
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
                Foreground = ResourceBrush("TextTertiaryBrush")
            };
            ChartCanvas.Children.Add(text);
            text.Measure(new Size(double.PositiveInfinity, double.PositiveInfinity));
            var labelWidth = text.DesiredSize.Width > 0 ? text.DesiredSize.Width : label.Length * 5.5;
            var labelHeight = text.DesiredSize.Height > 0 ? text.DesiredSize.Height : 14;
            Canvas.SetLeft(text, point.X - labelWidth / 2);
            Canvas.SetTop(text, height - labelHeight - 2);
        }
    }

    private static Geometry BuildSmoothLineGeometry(IReadOnlyList<Point> points)
    {
        var figure = new PathFigure { StartPoint = points[0], IsClosed = false, IsFilled = false };
        var path = new PathGeometry();
        path.Figures.Add(figure);
        AddSmoothSegments(figure, points);
        return path;
    }

    private static Geometry BuildSmoothAreaGeometry(IReadOnlyList<Point> points, double baselineY)
    {
        var figure = new PathFigure { StartPoint = new Point(points[0].X, baselineY), IsClosed = true };
        var path = new PathGeometry();
        path.Figures.Add(figure);
        figure.Segments.Add(new LineSegment { Point = points[0] });
        AddSmoothSegments(figure, points);
        figure.Segments.Add(new LineSegment { Point = new Point(points[^1].X, baselineY) });
        return path;
    }

    private static Geometry BuildStraightLineGeometry(IReadOnlyList<Point> points)
    {
        var figure = new PathFigure { StartPoint = points[0], IsClosed = false, IsFilled = false };
        var path = new PathGeometry();
        path.Figures.Add(figure);
        for (var i = 1; i < points.Count; i++)
            figure.Segments.Add(new LineSegment { Point = points[i] });
        return path;
    }

    private static void AddSmoothSegments(PathFigure figure, IReadOnlyList<Point> points)
    {
        for (var i = 1; i < points.Count; i++)
        {
            var prev = points[i - 1];
            var curr = points[i];
            var midX = (prev.X + curr.X) / 2;
            figure.Segments.Add(new BezierSegment
            {
                Point1 = new Point(midX, prev.Y),
                Point2 = new Point(midX, curr.Y),
                Point3 = curr
            });
        }
    }

    private static Brush ResourceBrush(string key) =>
        (Brush)Application.Current.Resources[key]!;

    private sealed class WeightTrendEntry
    {
        public DateTime Date { get; init; }
        public double CanonicalValue { get; init; }
        public double DisplayValue { get; init; }
        public string FormattedValue { get; init; } = "";
    }
}