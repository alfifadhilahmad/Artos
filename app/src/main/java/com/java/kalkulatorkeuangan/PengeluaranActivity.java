package com.java.kalkulatorkeuangan;

import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PengeluaranActivity extends AppCompatActivity {

    private static final String[] MONTH_NAMES = {
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private DatabaseHelper dbHelper;
    private TextView tvTotalPengeluaran;
    private TextView tvPengeluaranMonth;
    private LinearLayout containerKategori;
    private ExpenseBarChartView expenseBarChart;
    private Calendar selectedMonthCalendar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengeluaran);

        // Inisialisasi View Utama
        tvTotalPengeluaran = findViewById(R.id.tvTotalPengeluaran);
        tvPengeluaranMonth = findViewById(R.id.tvPengeluaranMonth);
        containerKategori = findViewById(R.id.containerKategori);
        expenseBarChart = findViewById(R.id.expenseBarChart);

        dbHelper = new DatabaseHelper(this);
        selectedMonthCalendar = Calendar.getInstance();
        selectedMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);

        setupMonthSelector();
        updatePengeluaranPage();

        // FUNGSI TOMBOL BACK DI HP BIAR SELALU BALIK KE HOME
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });

        setupCustomBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvTotalPengeluaran != null && selectedMonthCalendar != null) {
            updatePengeluaranPage();
        }
    }

    private void updatePengeluaranPage() {
        containerKategori.removeAllViews();

        double totalPengeluaranBulanTerpilih = calculateMonthlyExpense(dbHelper, selectedMonthCalendar);

        tvTotalPengeluaran.setText(formatRupiah(totalPengeluaranBulanTerpilih));
        tvPengeluaranMonth.setText(formatMonthLabel(selectedMonthCalendar));
        setupExpenseChart(dbHelper, expenseBarChart, selectedMonthCalendar);
        renderCategoryBreakdown(
                dbHelper,
                containerKategori,
                totalPengeluaranBulanTerpilih,
                selectedMonthCalendar
        );
    }

    private void setupMonthSelector() {
        findViewById(R.id.chipPengeluaranMonth).setOnClickListener(v -> showMonthYearPickerDialog());
    }

    private void showMonthYearPickerDialog() {
        int[] temporaryMonth = {selectedMonthCalendar.get(Calendar.MONTH)};
        int[] temporaryYear = {selectedMonthCalendar.get(Calendar.YEAR)};
        Dialog dialog = new Dialog(this);
        dialog.setCanceledOnTouchOutside(true);

        FrameLayout dialogContent = new FrameLayout(this);
        dialogContent.setMinimumWidth(dp(328));
        dialogContent.setMinimumHeight(dp(260));
        dialogContent.setBackground(createRoundedBackground("#FFFFFF", 16));
        dialogContent.setElevation(dp(8));

        TextView previousYearButton = createYearArrowButton("<");
        TextView yearText = new TextView(this);
        yearText.setText(String.valueOf(temporaryYear[0]));
        yearText.setTextColor(Color.parseColor("#306D29"));
        yearText.setTextSize(16);
        yearText.setGravity(Gravity.CENTER);
        yearText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        TextView nextYearButton = createYearArrowButton(">");

        FrameLayout.LayoutParams previousYearParams = new FrameLayout.LayoutParams(dp(32), dp(40));
        previousYearParams.gravity = Gravity.TOP | Gravity.START;
        previousYearParams.setMargins(dp(16), dp(16), 0, 0);
        dialogContent.addView(previousYearButton, previousYearParams);

        FrameLayout.LayoutParams yearTextParams = new FrameLayout.LayoutParams(dp(96), dp(40));
        yearTextParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        yearTextParams.setMargins(0, dp(16), 0, 0);
        dialogContent.addView(yearText, yearTextParams);

        FrameLayout.LayoutParams nextYearParams = new FrameLayout.LayoutParams(dp(32), dp(40));
        nextYearParams.gravity = Gravity.TOP | Gravity.END;
        nextYearParams.setMargins(0, dp(16), dp(16), 0);
        dialogContent.addView(nextYearButton, nextYearParams);

        GridLayout monthGrid = new GridLayout(this);
        monthGrid.setColumnCount(3);

        TextView[] monthButtons = new TextView[MONTH_NAMES.length];
        for (int i = 0; i < MONTH_NAMES.length; i++) {
            int monthIndex = i;
            TextView monthButton = createMonthButton(getShortMonthName(i));
            monthButton.setOnClickListener(v -> {
                temporaryMonth[0] = monthIndex;
                updateMonthButtonStates(monthButtons, temporaryMonth[0]);
                selectedMonthCalendar.set(Calendar.YEAR, temporaryYear[0]);
                selectedMonthCalendar.set(Calendar.MONTH, temporaryMonth[0]);
                selectedMonthCalendar.set(Calendar.DAY_OF_MONTH, 1);
                updatePengeluaranPage();
                dialog.dismiss();
            });

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = dp(80);
            params.height = dp(40);
            params.setMargins(dp(4), dp(4), dp(4), dp(4));
            monthGrid.addView(monthButton, params);
            monthButtons[i] = monthButton;
        }
        updateMonthButtonStates(monthButtons, temporaryMonth[0]);

        FrameLayout.LayoutParams monthGridParams = new FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        monthGridParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        monthGridParams.setMargins(0, dp(60), 0, 0);
        dialogContent.addView(monthGrid, monthGridParams);

        dialog.setContentView(dialogContent);

        previousYearButton.setOnClickListener(v -> {
            temporaryYear[0]--;
            yearText.setText(String.valueOf(temporaryYear[0]));
        });
        nextYearButton.setOnClickListener(v -> {
            temporaryYear[0]++;
            yearText.setText(String.valueOf(temporaryYear[0]));
        });

        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setLayout(dp(328), dp(260));
        }
    }

    private TextView createYearArrowButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextColor(Color.parseColor("#262A24"));
        button.setTextSize(10);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setBackground(createRoundedStrokeBackground("#FFFFFF", "#EDEDE8", 6));
        button.setClickable(true);
        button.setFocusable(true);
        button.setLayoutParams(new LinearLayout.LayoutParams(dp(32), dp(40)));
        return button;
    }

    private TextView createMonthButton(String text) {
        TextView button = new TextView(this);
        button.setText(text);
        button.setTextSize(12);
        button.setGravity(Gravity.CENTER);
        button.setTypeface(Typeface.DEFAULT, Typeface.NORMAL);
        button.setClickable(true);
        button.setFocusable(true);
        return button;
    }

    private void updateMonthButtonStates(TextView[] monthButtons, int selectedMonth) {
        for (int i = 0; i < monthButtons.length; i++) {
            boolean isSelected = i == selectedMonth;
            monthButtons[i].setTextColor(Color.parseColor(isSelected ? "#FFFFFF" : "#262A24"));
            monthButtons[i].setBackground(createRoundedBackground(isSelected ? "#306D29" : "#FFFFFF", 6));
        }
    }

    private GradientDrawable createRoundedBackground(String colorHex, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable createRoundedStrokeBackground(String fillColorHex, String strokeColorHex, int radiusDp) {
        GradientDrawable drawable = createRoundedBackground(fillColorHex, radiusDp);
        drawable.setStroke(dp(1), Color.parseColor(strokeColorHex));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void setupCustomBottomNavigation() {
        findViewById(R.id.navHomeButton).setOnClickListener(v ->
                openBottomNavActivity(HomeActivity.class));

        findViewById(R.id.navPengeluaranButton).setOnClickListener(v -> {
            // Already on Pengeluaran.
        });

        findViewById(R.id.navBudgetButton).setOnClickListener(v ->
                openBottomNavActivity(BudgetActivity.class));

        findViewById(R.id.navRiwayatButton).setOnClickListener(v ->
                openBottomNavActivity(RiwayatActivity.class));

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), TambahTransaksiActivity.class)));
    }

    private void openBottomNavActivity(Class<?> destinationActivity) {
        startActivity(new Intent(getApplicationContext(), destinationActivity));
        overridePendingTransition(0, 0);
        finish();
    }

    private double calculateMonthlyExpense(DatabaseHelper dbHelper, Calendar targetMonthCalendar) {
        double total = 0;
        Cursor cursor = dbHelper.getAllTransactions();

        if (cursor == null) {
            return total;
        }

        try {
            int typeIndex = cursor.getColumnIndex("type");
            int amountIndex = cursor.getColumnIndex("amount");
            int dateIndex = cursor.getColumnIndex("date");

            if (typeIndex == -1 || amountIndex == -1 || dateIndex == -1) {
                return total;
            }

            while (cursor.moveToNext()) {
                String type = cursor.getString(typeIndex);
                String dateText = cursor.getString(dateIndex);

                if ("Pengeluaran".equals(type) && isSameMonthYear(dateText, targetMonthCalendar)) {
                    total += cursor.getDouble(amountIndex);
                }
            }
        } finally {
            cursor.close();
        }

        return total;
    }

    private void setupExpenseChart(
            DatabaseHelper dbHelper,
            ExpenseBarChartView expenseBarChart,
            Calendar selectedMonthCalendar
    ) {
        ChartData chartData = calculateLastSixMonthExpenses(dbHelper, selectedMonthCalendar);
        expenseBarChart.setData(chartData.monthLabels, chartData.monthlyTotals, 5, chartData.averageValue);
    }

    private ChartData calculateLastSixMonthExpenses(DatabaseHelper dbHelper, Calendar selectedMonthCalendar) {
        String[] monthLabels = new String[6];
        double[] monthlyTotals = new double[6];
        int[] monthValues = new int[6];
        int[] yearValues = new int[6];

        Calendar calendar = (Calendar) selectedMonthCalendar.clone();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.add(Calendar.MONTH, -5);

        for (int i = 0; i < 6; i++) {
            monthValues[i] = calendar.get(Calendar.MONTH);
            yearValues[i] = calendar.get(Calendar.YEAR);
            monthLabels[i] = getShortMonthName(monthValues[i]) + " " + String.format(Locale.US, "%02d", yearValues[i] % 100);
            calendar.add(Calendar.MONTH, 1);
        }

        Cursor cursor = dbHelper.getAllTransactions();
        if (cursor != null) {
            try {
                int typeIndex = cursor.getColumnIndex("type");
                int amountIndex = cursor.getColumnIndex("amount");
                int dateIndex = cursor.getColumnIndex("date");

                if (typeIndex != -1 && amountIndex != -1 && dateIndex != -1) {
                    while (cursor.moveToNext()) {
                        String type = cursor.getString(typeIndex);
                        if (!"Pengeluaran".equals(type)) {
                            continue;
                        }

                        Calendar transactionCalendar = parseTransactionDate(cursor.getString(dateIndex));
                        if (transactionCalendar == null) {
                            continue;
                        }

                        for (int i = 0; i < 6; i++) {
                            if (transactionCalendar.get(Calendar.YEAR) == yearValues[i]
                                    && transactionCalendar.get(Calendar.MONTH) == monthValues[i]) {
                                monthlyTotals[i] += cursor.getDouble(amountIndex);
                                break;
                            }
                        }
                    }
                }
            } finally {
                cursor.close();
            }
        }

        double total = 0;
        for (double monthlyTotal : monthlyTotals) {
            total += monthlyTotal;
        }

        return new ChartData(monthLabels, monthlyTotals, total / 6d);
    }

    private void renderCategoryBreakdown(
            DatabaseHelper dbHelper,
            LinearLayout containerKategori,
            double totalPengeluaranBulanTerpilih,
            Calendar targetMonthCalendar
    ) {
        List<CategorySummary> summaries = getCategorySummaries(dbHelper, targetMonthCalendar);
        LayoutInflater inflater = LayoutInflater.from(this);

        if (summaries.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Belum ada pengeluaran bulan ini");
            emptyText.setTextColor(0xFF8A8F83);
            emptyText.setTextSize(13);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(4, 18, 4, 28);
            containerKategori.addView(emptyText);
            return;
        }

        for (CategorySummary summary : summaries) {
            double percentage = 0;
            if (totalPengeluaranBulanTerpilih > 0) {
                percentage = (summary.total / totalPengeluaranBulanTerpilih) * 100;
            }

            View itemView = inflater.inflate(R.layout.item_analisis, containerKategori, false);

            ImageView ivKategoriIcon = itemView.findViewById(R.id.ivKategoriIcon);
            TextView tvNama = itemView.findViewById(R.id.tvNamaKategori);
            TextView tvPersen = itemView.findViewById(R.id.tvPersenKategori);
            ProgressBar pbKategori = itemView.findViewById(R.id.pbKategori);
            TextView tvTotal = itemView.findViewById(R.id.tvTotalKategori);

            ivKategoriIcon.setImageResource(getCategoryIconRes(summary.category));
            tvNama.setText(summary.category);
            tvPersen.setText(formatPercentage(percentage));
            pbKategori.setProgress((int) Math.round(percentage));
            tvTotal.setText(formatExpenseRupiah(summary.total));

            containerKategori.addView(itemView);
        }
    }

    private List<CategorySummary> getCategorySummaries(DatabaseHelper dbHelper, Calendar targetMonthCalendar) {
        Map<String, Double> categoryTotals = new HashMap<>();
        Cursor cursor = dbHelper.getAllTransactions();

        if (cursor == null) {
            return new ArrayList<>();
        }

        try {
            int typeIndex = cursor.getColumnIndex("type");
            int amountIndex = cursor.getColumnIndex("amount");
            int categoryIndex = cursor.getColumnIndex("category");
            int dateIndex = cursor.getColumnIndex("date");

            if (typeIndex == -1 || amountIndex == -1 || categoryIndex == -1 || dateIndex == -1) {
                return new ArrayList<>();
            }

            while (cursor.moveToNext()) {
                String type = cursor.getString(typeIndex);
                String dateText = cursor.getString(dateIndex);

                if (!"Pengeluaran".equals(type) || !isSameMonthYear(dateText, targetMonthCalendar)) {
                    continue;
                }

                String category = normalizeCategory(cursor.getString(categoryIndex));
                double amount = cursor.getDouble(amountIndex);
                Double currentTotal = categoryTotals.get(category);
                if (currentTotal == null) {
                    currentTotal = 0.0;
                }
                categoryTotals.put(category, currentTotal + amount);
            }
        } finally {
            cursor.close();
        }

        List<CategorySummary> summaries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            summaries.add(new CategorySummary(entry.getKey(), entry.getValue()));
        }

        Collections.sort(summaries, (first, second) -> {
            int totalComparison = Double.compare(second.total, first.total);
            if (totalComparison != 0) {
                return totalComparison;
            }
            return first.category.compareToIgnoreCase(second.category);
        });

        return summaries;
    }

    private boolean isSameMonthYear(String dateText, Calendar targetMonthCalendar) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return false;
        }

        try {
            Calendar transactionCalendar = parseTransactionDate(dateText);
            if (transactionCalendar == null) {
                return false;
            }

            return transactionCalendar.get(Calendar.YEAR) == targetMonthCalendar.get(Calendar.YEAR)
                    && transactionCalendar.get(Calendar.MONTH) == targetMonthCalendar.get(Calendar.MONTH);
        } catch (Exception e) {
            return false;
        }
    }

    private Calendar parseTransactionDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

        SimpleDateFormat dbDateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        dbDateFormat.setLenient(false);

        try {
            Date parsedDate = dbDateFormat.parse(dateText.trim());
            Calendar transactionCalendar = Calendar.getInstance();
            transactionCalendar.setTime(parsedDate);
            return transactionCalendar;
        } catch (ParseException e) {
            return null;
        }
    }

    private String formatRupiah(double amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return "Rp " + numberFormat.format(amount);
    }

    private String formatExpenseRupiah(double amount) {
        return "- " + formatRupiah(amount);
    }

    private String formatPercentage(double percentage) {
        if (percentage <= 0) {
            return "0%";
        }

        long rounded = Math.round(percentage);
        if (percentage >= 1 && Math.abs(percentage - rounded) < 0.05) {
            return rounded + "%";
        }

        return String.format(Locale.US, "%.1f%%", percentage);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return "Lainnya";
        }
        return category.trim();
    }

    private int getCategoryIconRes(String category) {
        String normalizedCategory = normalizeCategory(category);

        switch (normalizedCategory) {
            case "Makanan":
                return R.drawable.ic_category_food_active;
            case "Camilan":
                return R.drawable.ic_category_snack_active;
            case "Belanja":
                return R.drawable.ic_category_shopping_active;
            case "Tagihan":
                return R.drawable.ic_category_bill_active;
            case "Kesehatan":
                return R.drawable.ic_category_health_active;
            case "Edukasi":
                return R.drawable.ic_category_education_active;
            case "Hiburan":
                return R.drawable.ic_category_entertainment_active;
            case "Tabungan":
                return R.drawable.ic_category_savings_active;
            case "Sewa Kos":
                return R.drawable.ic_category_house_active;
            case "Pemasukan":
                return R.drawable.ic_category_income_active;
            case "Lainnya":
            default:
                return R.drawable.ic_category_other_active;
        }
    }

    private String formatMonthLabel(Calendar calendar) {
        int monthIndex = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        return MONTH_NAMES[monthIndex] + " " + year;
    }

    private String getShortMonthName(int monthIndex) {
        switch (monthIndex) {
            case Calendar.JANUARY:
                return "Jan";
            case Calendar.FEBRUARY:
                return "Feb";
            case Calendar.MARCH:
                return "Mar";
            case Calendar.APRIL:
                return "Apr";
            case Calendar.MAY:
                return "Mei";
            case Calendar.JUNE:
                return "Jun";
            case Calendar.JULY:
                return "Jul";
            case Calendar.AUGUST:
                return "Agu";
            case Calendar.SEPTEMBER:
                return "Sep";
            case Calendar.OCTOBER:
                return "Okt";
            case Calendar.NOVEMBER:
                return "Nov";
            case Calendar.DECEMBER:
            default:
                return "Des";
        }
    }

    private static class CategorySummary {
        private final String category;
        private final double total;

        private CategorySummary(String category, double total) {
            this.category = category;
            this.total = total;
        }
    }

    private static class ChartData {
        private final String[] monthLabels;
        private final double[] monthlyTotals;
        private final double averageValue;

        private ChartData(String[] monthLabels, double[] monthlyTotals, double averageValue) {
            this.monthLabels = monthLabels;
            this.monthlyTotals = monthlyTotals;
            this.averageValue = averageValue;
        }
    }
}
