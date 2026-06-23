package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pengeluaran);

        // Inisialisasi View Utama
        TextView tvTotalPengeluaran = findViewById(R.id.tvTotalPengeluaran);
        TextView tvPengeluaranMonth = findViewById(R.id.tvPengeluaranMonth);
        LinearLayout containerKategori = findViewById(R.id.containerKategori);
        ExpenseBarChartView expenseBarChart = findViewById(R.id.expenseBarChart);

        // Bersihkan container dulu buat jaga-jaga
        containerKategori.removeAllViews();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        double totalPengeluaranBulanIni = calculateCurrentMonthExpense(dbHelper);

        // Set teks total pengeluaran header untuk bulan berjalan.
        tvTotalPengeluaran.setText(formatRupiah(totalPengeluaranBulanIni));
        tvPengeluaranMonth.setText(formatCurrentMonthLabel());
        setupExpenseChart(dbHelper, expenseBarChart);
        renderCurrentMonthCategoryBreakdown(dbHelper, containerKategori, totalPengeluaranBulanIni);

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

    private double calculateCurrentMonthExpense(DatabaseHelper dbHelper) {
        double total = 0;
        Cursor cursor = dbHelper.getAllTransactions();

        if (cursor == null) {
            return total;
        }

        Calendar currentCalendar = Calendar.getInstance();

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

                if ("Pengeluaran".equals(type) && isCurrentMonthDate(dateText, currentCalendar)) {
                    total += cursor.getDouble(amountIndex);
                }
            }
        } finally {
            cursor.close();
        }

        return total;
    }

    private void setupExpenseChart(DatabaseHelper dbHelper, ExpenseBarChartView expenseBarChart) {
        ChartData chartData = calculateLastSixMonthExpenses(dbHelper);
        expenseBarChart.setData(chartData.monthLabels, chartData.monthlyTotals, 5, chartData.averageValue);
    }

    private ChartData calculateLastSixMonthExpenses(DatabaseHelper dbHelper) {
        String[] monthLabels = new String[6];
        double[] monthlyTotals = new double[6];
        int[] monthValues = new int[6];
        int[] yearValues = new int[6];

        Calendar calendar = Calendar.getInstance();
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

    private void renderCurrentMonthCategoryBreakdown(
            DatabaseHelper dbHelper,
            LinearLayout containerKategori,
            double totalPengeluaranBulanIni
    ) {
        List<CategorySummary> summaries = getCurrentMonthCategorySummaries(dbHelper);
        LayoutInflater inflater = LayoutInflater.from(this);

        if (summaries.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("Belum ada pengeluaran bulan ini");
            emptyText.setTextColor(0xFF8A8F83);
            emptyText.setTextSize(12);
            emptyText.setPadding(4, 8, 4, 8);
            containerKategori.addView(emptyText);
            return;
        }

        for (CategorySummary summary : summaries) {
            double percentage = 0;
            if (totalPengeluaranBulanIni > 0) {
                percentage = (summary.total / totalPengeluaranBulanIni) * 100;
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

    private List<CategorySummary> getCurrentMonthCategorySummaries(DatabaseHelper dbHelper) {
        Map<String, Double> categoryTotals = new HashMap<>();
        Cursor cursor = dbHelper.getAllTransactions();

        if (cursor == null) {
            return new ArrayList<>();
        }

        Calendar currentCalendar = Calendar.getInstance();

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

                if (!"Pengeluaran".equals(type) || !isCurrentMonthDate(dateText, currentCalendar)) {
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

    private boolean isCurrentMonthDate(String dateText, Calendar currentCalendar) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return false;
        }

        SimpleDateFormat dbDateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        dbDateFormat.setLenient(false);

        try {
            Calendar transactionCalendar = parseTransactionDate(dateText);
            if (transactionCalendar == null) {
                return false;
            }

            return transactionCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
                    && transactionCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH);
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

    private String formatCurrentMonthLabel() {
        Calendar calendar = Calendar.getInstance();
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
