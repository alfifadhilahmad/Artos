package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.database.Cursor;
import android.graphics.Color;

import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        updateDashboardData();

        setupCustomBottomNavigation();

        View fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TambahTransaksiActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardData();
    }

    private void setupCustomBottomNavigation() {
        findViewById(R.id.navHomeButton).setOnClickListener(v -> {
            // Already on Home.
        });

        findViewById(R.id.navPengeluaranButton).setOnClickListener(v ->
                openBottomNavActivity(PengeluaranActivity.class));

        findViewById(R.id.navBudgetButton).setOnClickListener(v ->
                openBottomNavActivity(BudgetActivity.class));

        findViewById(R.id.navRiwayatButton).setOnClickListener(v ->
                openBottomNavActivity(RiwayatActivity.class));

        findViewById(R.id.btnRecentToRiwayat).setOnClickListener(v ->
                openBottomNavActivity(RiwayatActivity.class));
    }

    private void openBottomNavActivity(Class<?> destinationActivity) {
        startActivity(new Intent(getApplicationContext(), destinationActivity));
        overridePendingTransition(0, 0);
        finish();
    }

    private void updateDashboardData() {
        // --- BAGIAN 0: MENAMPILKAN NAMA USER DINAMIS ---
        TextView tvNamaUser = findViewById(R.id.tvNamaUser);

        SharedPreferences loginPrefs = getSharedPreferences("SesiLogin", MODE_PRIVATE);
        // Ngambil data "USERNAME", kalau kosong otomatis diisi kata "User"
        String namaAktif = loginPrefs.getString("USERNAME", "User");

        if (tvNamaUser != null) {
            tvNamaUser.setText("👋 Hai, " + namaAktif + "!");
        }

        // --- BAGIAN 1: KARTU TOTAL DUIT (SALDO) ---
        TextView tvSaldo = findViewById(R.id.tvSaldo);
        TextView tvPemasukan = findViewById(R.id.tvPemasukan);
        TextView tvPengeluaran = findViewById(R.id.tvPengeluaran);
        TextView tvBulanSaldoBulanIni = findViewById(R.id.tvBulanSaldoBulanIni);
        TextView tvStatusSaldoBulanIni = findViewById(R.id.tvStatusSaldoBulanIni);
        TextView tvSaldoBulanIni = findViewById(R.id.tvSaldoBulanIni);

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        double totalPemasukan = dbHelper.getTotalPemasukan();
        double totalPengeluaran = dbHelper.getTotalPengeluaran();
        double saldo = totalPemasukan - totalPengeluaran;

        Calendar currentDate = Calendar.getInstance();
        int currentMonth = currentDate.get(Calendar.MONTH) + 1;
        int currentYear = currentDate.get(Calendar.YEAR);
        double[] monthlyTotals = calculateCurrentMonthTotals(dbHelper, currentMonth, currentYear);
        double monthlyPemasukan = monthlyTotals[0];
        double monthlyPengeluaran = monthlyTotals[1];
        double monthlySaldo = monthlyPemasukan - monthlyPengeluaran;

        tvSaldo.setText(formatRupiah(saldo));
        tvPemasukan.setText(formatRupiah(monthlyPemasukan));
        tvPengeluaran.setText(formatRupiah(monthlyPengeluaran));
        tvBulanSaldoBulanIni.setText(formatIndonesianMonthYear(currentDate));
        tvSaldoBulanIni.setText(formatSignedRupiah(monthlySaldo));
        bindMonthlyStatus(tvStatusSaldoBulanIni, monthlySaldo);

        updateRecentTransactions(dbHelper);

        // --- BAGIAN 3: ANALISIS KEUANGAN BULAN INI (BUDGET) ---
        TextView tvBudgetBulanan = findViewById(R.id.tvBudgetBulanan);
        ProgressBar pbBudget = findViewById(R.id.pbBudget);
        TextView tvPersenBudget = findViewById(R.id.tvPersenBudget);
        TextView tvTerpakai = findViewById(R.id.tvTerpakai);
        TextView tvSisaBudget = findViewById(R.id.tvSisaBudget);

        SharedPreferences prefs = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);
        double targetBudget = prefs.getFloat("budget", 5000000);

        double sisaBudget = targetBudget - totalPengeluaran;
        int persenTerpakai = 0;

        if (targetBudget > 0) {
            persenTerpakai = (int) ((totalPengeluaran / targetBudget) * 100);
        }

        if (persenTerpakai > 100) {
            persenTerpakai = 100;
        }

        tvBudgetBulanan.setText("Budget");
        tvTerpakai.setText("- " + formatRupiah(totalPengeluaran));
        tvTerpakai.setTextColor(Color.parseColor("#B3261E"));
        tvPersenBudget.setText(persenTerpakai + "% digunakan");
        tvPersenBudget.setVisibility(View.GONE);

        ObjectAnimator animation = ObjectAnimator.ofInt(pbBudget, "progress", 0, persenTerpakai);
        animation.setDuration(1200);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();

        if (sisaBudget < 0) {
            tvSisaBudget.setText("Sisa " + formatRupiah(sisaBudget));
            tvSisaBudget.setTextColor(Color.parseColor("#B3261E"));
            pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.RED));
        } else {
            tvSisaBudget.setText("Sisa " + formatRupiah(sisaBudget));
            tvSisaBudget.setTextColor(Color.parseColor("#666666"));
            pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFA43C")));
        }
        pbBudget.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#D9D9D9")));
    }

    private String formatRupiah(double amount) {
        String formattedAmount = String.format(Locale.US, "%,.0f", Math.abs(amount)).replace(",", ".");
        String prefix = amount < 0 ? "- Rp " : "Rp ";
        return prefix + formattedAmount;
    }

    private String formatSignedRupiah(double amount) {
        String formattedAmount = String.format(Locale.US, "%,.0f", Math.abs(amount)).replace(",", ".");
        String prefix = amount < 0 ? "- Rp " : "+ Rp ";
        return prefix + formattedAmount;
    }

    private String formatIndonesianMonthYear(Calendar date) {
        String[] monthNames = {
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };

        return monthNames[date.get(Calendar.MONTH)] + " " + date.get(Calendar.YEAR);
    }

    private void bindMonthlyStatus(TextView statusView, double monthlySaldo) {
        if (monthlySaldo < 0) {
            statusView.setText("Negatif");
            statusView.setTextColor(Color.parseColor("#B3261E"));
            statusView.setBackgroundResource(R.drawable.home_status_negative_bg);
        } else {
            statusView.setText("Positif");
            statusView.setTextColor(Color.parseColor("#306D29"));
            statusView.setBackgroundResource(R.drawable.home_status_positive_bg);
        }
    }

    private double[] calculateCurrentMonthTotals(DatabaseHelper dbHelper, int currentMonth, int currentYear) {
        double[] totals = new double[2];
        Cursor cursor = dbHelper.getAllTransactions();

        try {
            int typeIndex = cursor.getColumnIndex("type");
            int amountIndex = cursor.getColumnIndex("amount");
            int dateIndex = cursor.getColumnIndex("date");

            if (typeIndex == -1 || amountIndex == -1 || dateIndex == -1) {
                return totals;
            }

            while (cursor.moveToNext()) {
                if (cursor.isNull(amountIndex)) {
                    continue;
                }

                String date = getCursorString(cursor, dateIndex);
                if (!isCurrentMonthDate(date, currentMonth, currentYear)) {
                    continue;
                }

                String type = getCursorString(cursor, typeIndex);
                double amount = cursor.getDouble(amountIndex);

                if ("Pemasukan".equalsIgnoreCase(type)) {
                    totals[0] += amount;
                } else if ("Pengeluaran".equalsIgnoreCase(type)) {
                    totals[1] += amount;
                }
            }
        } finally {
            cursor.close();
        }

        return totals;
    }

    private boolean isCurrentMonthDate(String date, int currentMonth, int currentYear) {
        if (date == null || date.isEmpty()) {
            return false;
        }

        String[] parts = date.split("/");
        if (parts.length != 3) {
            return false;
        }

        try {
            int month = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());
            return month == currentMonth && year == currentYear;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private void updateRecentTransactions(DatabaseHelper dbHelper) {
        View[] rows = {
                findViewById(R.id.rowTransaksi1),
                findViewById(R.id.rowTransaksi2),
                findViewById(R.id.rowTransaksi3)
        };

        ImageView[] icons = {
                findViewById(R.id.ivTransaksiIcon1),
                findViewById(R.id.ivTransaksiIcon2),
                findViewById(R.id.ivTransaksiIcon3)
        };

        TextView[] titles = {
                findViewById(R.id.tvTransaksi1),
                findViewById(R.id.tvTransaksi2),
                findViewById(R.id.tvTransaksi3)
        };

        TextView[] categories = {
                findViewById(R.id.tvTransaksiKategori1),
                findViewById(R.id.tvTransaksiKategori2),
                findViewById(R.id.tvTransaksiKategori3)
        };

        TextView[] amounts = {
                findViewById(R.id.tvTransaksiAmount1),
                findViewById(R.id.tvTransaksiAmount2),
                findViewById(R.id.tvTransaksiAmount3)
        };

        View[] dividers = {
                findViewById(R.id.dividerTransaksi1),
                findViewById(R.id.dividerTransaksi2)
        };

        TextView emptyText = findViewById(R.id.tvTransaksiEmpty);

        for (View row : rows) {
            row.setVisibility(View.GONE);
        }
        for (View divider : dividers) {
            divider.setVisibility(View.GONE);
        }
        emptyText.setVisibility(View.VISIBLE);

        List<RecentTransactionItem> recentItems = getSortedRecentTransactionItems(dbHelper);
        int index = 0;

        for (RecentTransactionItem item : recentItems) {
            if (index >= rows.length) {
                break;
            }

            rows[index].setVisibility(View.VISIBLE);
            icons[index].setImageResource(getTransactionIconRes(item.category, item.type));
            titles[index].setText(getTransactionTitle(item.note, item.category));
            categories[index].setText(getTransactionSubtitle(item.category, item.type));
            amounts[index].setText(formatTransactionAmount(item.type, item.amount));
            amounts[index].setTextColor(getTransactionAmountColor(item.type));

            index++;
        }

        emptyText.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        if (index > 1) {
            dividers[0].setVisibility(View.VISIBLE);
        }
        if (index > 2) {
            dividers[1].setVisibility(View.VISIBLE);
        }
    }

    private List<RecentTransactionItem> getSortedRecentTransactionItems(DatabaseHelper dbHelper) {
        List<RecentTransactionItem> items = new ArrayList<>();
        Cursor cursor = dbHelper.getAllTransactions();

        try {
            int idIndex = cursor.getColumnIndex("id");
            int typeIndex = cursor.getColumnIndex("type");
            int amountIndex = cursor.getColumnIndex("amount");
            int categoryIndex = cursor.getColumnIndex("category");
            int noteIndex = cursor.getColumnIndex("note");
            int dateIndex = cursor.getColumnIndex("date");

            if (idIndex == -1 || typeIndex == -1 || amountIndex == -1
                    || categoryIndex == -1 || noteIndex == -1 || dateIndex == -1) {
                return items;
            }

            while (cursor.moveToNext()) {
                RecentTransactionItem item = new RecentTransactionItem();
                item.id = cursor.getLong(idIndex);
                item.type = getCursorString(cursor, typeIndex);
                item.amount = cursor.isNull(amountIndex) ? 0 : cursor.getDouble(amountIndex);
                item.category = getCursorString(cursor, categoryIndex);
                item.note = getCursorString(cursor, noteIndex);
                item.date = getCursorString(cursor, dateIndex);
                parseRecentTransactionDate(item);
                items.add(item);
            }
        } finally {
            cursor.close();
        }

        sortRecentTransactionItems(items);
        return items;
    }

    private void parseRecentTransactionDate(RecentTransactionItem item) {
        item.validDate = false;

        if (item.date == null || item.date.trim().isEmpty()) {
            return;
        }

        String[] parts = item.date.trim().split("/");
        if (parts.length != 3) {
            return;
        }

        try {
            int day = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());

            if (day < 1 || month < 1 || month > 12 || year < 1) {
                return;
            }

            Calendar parsedDate = Calendar.getInstance();
            parsedDate.setLenient(false);
            parsedDate.clear();
            parsedDate.set(year, month - 1, day);
            parsedDate.getTime();

            item.day = day;
            item.month = month;
            item.year = year;
            item.validDate = true;
        } catch (IllegalArgumentException exception) {
            item.validDate = false;
        }
    }

    private void sortRecentTransactionItems(List<RecentTransactionItem> items) {
        Collections.sort(items, (first, second) -> {
            if (first.validDate != second.validDate) {
                return first.validDate ? -1 : 1;
            }

            if (first.validDate) {
                if (first.year != second.year) {
                    return second.year - first.year;
                }
                if (first.month != second.month) {
                    return second.month - first.month;
                }
                if (first.day != second.day) {
                    return second.day - first.day;
                }
            }

            if (first.id == second.id) {
                return 0;
            }
            return first.id < second.id ? 1 : -1;
        });
    }

    private static class RecentTransactionItem {
        long id;
        String type;
        double amount;
        String category;
        String note;
        String date;
        int year;
        int month;
        int day;
        boolean validDate;
    }

    private String getCursorString(Cursor cursor, int columnIndex) {
        if (cursor.isNull(columnIndex)) {
            return "";
        }
        return cursor.getString(columnIndex).trim();
    }

    private String getTransactionTitle(String note, String category) {
        if (!note.isEmpty()) {
            return note;
        }
        if (!category.isEmpty()) {
            return category;
        }
        return "Transaksi";
    }

    private String getTransactionSubtitle(String category, String type) {
        if (!category.isEmpty()) {
            return category;
        }
        if (!type.isEmpty()) {
            return type;
        }
        return "Transaksi";
    }

    private String formatTransactionAmount(String type, double amount) {
        String formattedAmount = String.format(Locale.US, "%,.0f", amount).replace(",", ".");

        if ("Pemasukan".equalsIgnoreCase(type)) {
            return "+ Rp " + formattedAmount;
        } else if ("Pengeluaran".equalsIgnoreCase(type)) {
            return "- Rp " + formattedAmount;
        }

        return "Rp " + formattedAmount;
    }

    private int getTransactionAmountColor(String type) {
        if ("Pemasukan".equalsIgnoreCase(type)) {
            return Color.parseColor("#306D29");
        } else if ("Pengeluaran".equalsIgnoreCase(type)) {
            return Color.parseColor("#B3261E");
        }

        return Color.parseColor("#262A24");
    }

    private int getTransactionIconRes(String category, String type) {
        if ("Pemasukan".equalsIgnoreCase(type)) {
            return R.drawable.ic_category_income_active;
        }

        String normalizedCategory = category.toLowerCase(Locale.ROOT);

        if (normalizedCategory.contains("makan") || normalizedCategory.contains("food")) {
            return R.drawable.ic_category_food_active;
        } else if (normalizedCategory.contains("edukasi") || normalizedCategory.contains("pendidikan")) {
            return R.drawable.ic_category_education_active;
        } else if (normalizedCategory.contains("tagihan") || normalizedCategory.contains("bill")) {
            return R.drawable.ic_category_bill_active;
        } else if (normalizedCategory.contains("hiburan")) {
            return R.drawable.ic_category_entertainment_active;
        } else if (normalizedCategory.contains("kesehatan")) {
            return R.drawable.ic_category_health_active;
        } else if (normalizedCategory.contains("rumah") || normalizedCategory.contains("sewa kos")) {
            return R.drawable.ic_category_house_active;
        } else if (normalizedCategory.contains("tabungan")) {
            return R.drawable.ic_category_savings_active;
        } else if (normalizedCategory.contains("belanja")) {
            return R.drawable.ic_category_shopping_active;
        } else if (normalizedCategory.contains("snack") || normalizedCategory.contains("camilan")) {
            return R.drawable.ic_category_snack_active;
        } else if (normalizedCategory.contains("lain")) {
            return R.drawable.ic_category_other_active;
        } else if ("Pengeluaran".equalsIgnoreCase(type)) {
            return R.drawable.ic_expenditure_active;
        }

        return R.drawable.ic_category_bill_active;
    }
}
