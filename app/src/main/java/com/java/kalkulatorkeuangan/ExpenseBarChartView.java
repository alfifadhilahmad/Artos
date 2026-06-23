package com.java.kalkulatorkeuangan;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class ExpenseBarChartView extends View {

    private static final int MONTH_COUNT = 6;
    private static final double DEFAULT_MAX_AXIS = 2_500_000;
    private static final double AXIS_STEP = 500_000;

    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint averagePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint averageLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint averageLabelTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private String[] monthLabels = new String[MONTH_COUNT];
    private double[] monthlyTotals = new double[MONTH_COUNT];
    private int currentMonthIndex = MONTH_COUNT - 1;
    private double averageValue = 0;
    private double maxAxisValue = DEFAULT_MAX_AXIS;

    public ExpenseBarChartView(Context context) {
        super(context);
        init();
    }

    public ExpenseBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExpenseBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);

        gridPaint.setColor(Color.parseColor("#EDEDE8"));
        gridPaint.setStrokeWidth(dp(1));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{dp(4), dp(4)}, 0));

        textPaint.setColor(Color.parseColor("#8A8F83"));
        textPaint.setTextSize(sp(8));

        averagePaint.setColor(Color.parseColor("#FFA43C"));
        averagePaint.setStrokeWidth(dp(1.5f));

        averageLabelPaint.setColor(Color.parseColor("#FFA43C"));
        averageLabelPaint.setStyle(Paint.Style.FILL);

        averageLabelTextPaint.setColor(Color.parseColor("#262A24"));
        averageLabelTextPaint.setTextSize(sp(8));
        averageLabelTextPaint.setFakeBoldText(true);

        for (int i = 0; i < MONTH_COUNT; i++) {
            monthLabels[i] = "";
            monthlyTotals[i] = 0;
        }
    }

    public void setData(String[] labels, double[] totals, int currentIndex, double average) {
        if (labels != null) {
            for (int i = 0; i < MONTH_COUNT && i < labels.length; i++) {
                monthLabels[i] = labels[i] == null ? "" : labels[i];
            }
        }

        double highestValue = average;
        if (totals != null) {
            for (int i = 0; i < MONTH_COUNT && i < totals.length; i++) {
                monthlyTotals[i] = Math.max(0, totals[i]);
                highestValue = Math.max(highestValue, monthlyTotals[i]);
            }
        }

        currentMonthIndex = Math.max(0, Math.min(MONTH_COUNT - 1, currentIndex));
        averageValue = Math.max(0, average);
        maxAxisValue = calculateMaxAxis(highestValue);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) {
            return;
        }

        float leftLabelWidth = dp(42);
        float topPadding = dp(28);
        float bottomLabelHeight = dp(24);
        float chartLeft = leftLabelWidth + dp(8);
        float chartRight = width - dp(4);
        float chartTop = topPadding;
        float chartBottom = height - bottomLabelHeight;
        float chartHeight = chartBottom - chartTop;

        drawGridAndYAxis(canvas, chartLeft, chartRight, chartTop, chartBottom, chartHeight);
        drawBarsAndXAxis(canvas, chartLeft, chartRight, chartTop, chartBottom, chartHeight, height);
        drawAverageLine(canvas, chartLeft, chartRight, chartTop, chartBottom, chartHeight);
    }

    private void drawGridAndYAxis(
            Canvas canvas,
            float chartLeft,
            float chartRight,
            float chartTop,
            float chartBottom,
            float chartHeight
    ) {
        textPaint.setTextAlign(Paint.Align.RIGHT);
        textPaint.setColor(Color.parseColor("#8A8F83"));

        for (int i = 0; i <= 5; i++) {
            float y = chartBottom - (chartHeight / 5f * i);
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint);

            double value = maxAxisValue / 5d * i;
            String label = formatAxisLabel(value);
            Paint.FontMetrics metrics = textPaint.getFontMetrics();
            canvas.drawText(label, chartLeft - dp(8), y - ((metrics.ascent + metrics.descent) / 2f), textPaint);
        }
    }

    private void drawAverageLine(
            Canvas canvas,
            float chartLeft,
            float chartRight,
            float chartTop,
            float chartBottom,
            float chartHeight
    ) {
        float y = valueToY(averageValue, chartTop, chartBottom, chartHeight);
        canvas.drawLine(chartLeft, y, chartRight, y, averagePaint);

        String label = "Avg 6 bulan terakhir " + formatCompactRupiah(averageValue);
        float labelPaddingH = dp(5);
        float labelPaddingV = dp(3);
        float textWidth = averageLabelTextPaint.measureText(label);
        Paint.FontMetrics metrics = averageLabelTextPaint.getFontMetrics();
        float labelHeight = (metrics.descent - metrics.ascent) + (labelPaddingV * 2);
        float maxLabelWidth = chartRight - chartLeft;
        float labelWidth = Math.min(textWidth + (labelPaddingH * 2), maxLabelWidth);
        float labelLeft = Math.max(chartLeft, Math.min(chartLeft, chartRight - labelWidth));
        float labelTop = Math.max(dp(2), y - labelHeight - dp(4));

        RectF labelRect = new RectF(labelLeft, labelTop, labelLeft + labelWidth, labelTop + labelHeight);
        canvas.drawRoundRect(labelRect, dp(6), dp(6), averageLabelPaint);
        canvas.save();
        canvas.clipRect(labelRect);
        canvas.drawText(
                label,
                labelLeft + labelPaddingH,
                labelTop + labelPaddingV - metrics.ascent,
                averageLabelTextPaint
        );
        canvas.restore();
    }

    private void drawBarsAndXAxis(
            Canvas canvas,
            float chartLeft,
            float chartRight,
            float chartTop,
            float chartBottom,
            float chartHeight,
            float fullHeight
    ) {
        float chartWidth = chartRight - chartLeft;
        float slotWidth = chartWidth / MONTH_COUNT;
        float barWidth = Math.min(dp(24), slotWidth * 0.48f);

        textPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < MONTH_COUNT; i++) {
            float centerX = chartLeft + (slotWidth * i) + (slotWidth / 2f);
            double value = monthlyTotals[i];
            float barTop = valueToY(value, chartTop, chartBottom, chartHeight);

            if (value <= 0) {
                barTop = chartBottom;
            } else {
                barTop = Math.min(chartBottom - dp(3), barTop);
            }

            barPaint.setColor(i == currentMonthIndex
                    ? Color.parseColor("#306D29")
                    : Color.parseColor("#CDE3C8"));

            RectF barRect = new RectF(
                    centerX - (barWidth / 2f),
                    barTop,
                    centerX + (barWidth / 2f),
                    chartBottom
            );
            canvas.drawRoundRect(barRect, dp(8), dp(8), barPaint);

            textPaint.setColor(i == currentMonthIndex
                    ? Color.parseColor("#262A24")
                    : Color.parseColor("#8A8F83"));
            canvas.drawText(monthLabels[i], centerX, fullHeight - dp(6), textPaint);
        }
    }

    private float valueToY(double value, float chartTop, float chartBottom, float chartHeight) {
        if (maxAxisValue <= 0) {
            return chartBottom;
        }

        double clampedValue = Math.max(0, Math.min(value, maxAxisValue));
        return (float) (chartBottom - ((clampedValue / maxAxisValue) * chartHeight));
    }

    private double calculateMaxAxis(double highestValue) {
        if (highestValue <= DEFAULT_MAX_AXIS) {
            return DEFAULT_MAX_AXIS;
        }
        return Math.ceil(highestValue / AXIS_STEP) * AXIS_STEP;
    }

    private String formatAxisLabel(double value) {
        if (value <= 0) {
            return "0rb";
        }

        if (value < 1_000_000) {
            return Math.round(value / 1_000) + "rb";
        }

        double million = value / 1_000_000d;
        return formatCompactNumber(million) + "Jt";
    }

    private String formatCompactRupiah(double value) {
        if (value <= 0) {
            return "Rp0";
        }

        if (value < 1_000_000) {
            return "Rp" + Math.round(value / 1_000) + "rb";
        }

        return "Rp" + formatCompactNumber(value / 1_000_000d) + "Jt";
    }

    private String formatCompactNumber(double value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
        symbols.setDecimalSeparator(',');
        DecimalFormat decimalFormat = new DecimalFormat("#.#", symbols);
        return decimalFormat.format(value);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private float sp(float value) {
        return value * getResources().getDisplayMetrics().scaledDensity;
    }
}
