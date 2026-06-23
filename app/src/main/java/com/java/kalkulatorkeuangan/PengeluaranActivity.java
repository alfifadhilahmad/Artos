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

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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

        // Bersihkan container dulu buat jaga-jaga
        containerKategori.removeAllViews();

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        double totalPengeluaranBulanIni = calculateCurrentMonthExpense(dbHelper);

        // Set teks total pengeluaran header untuk bulan berjalan.
        tvTotalPengeluaran.setText(formatRupiah(totalPengeluaranBulanIni));
        tvPengeluaranMonth.setText(formatCurrentMonthLabel());
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

        // ==========================================
        // LOGIKA NAVIGASI BAWAH
        // ==========================================
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_pengeluaran);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_pengeluaran) {
                return true; // Diam saja
            } else if (itemId == R.id.nav_budget) {
                startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_riwayat) {
                startActivity(new Intent(getApplicationContext(), RiwayatActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TambahTransaksiActivity.class));
        });
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
            tvTotal.setText(formatRupiah(summary.total));

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
            Date parsedDate = dbDateFormat.parse(dateText.trim());
            Calendar transactionCalendar = Calendar.getInstance();
            transactionCalendar.setTime(parsedDate);

            return transactionCalendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR)
                    && transactionCalendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH);
        } catch (ParseException e) {
            return false;
        }
    }

    private String formatRupiah(double amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return "Rp " + numberFormat.format(amount);
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

    private static class CategorySummary {
        private final String category;
        private final double total;

        private CategorySummary(String category, double total) {
            this.category = category;
            this.total = total;
        }
    }
}
