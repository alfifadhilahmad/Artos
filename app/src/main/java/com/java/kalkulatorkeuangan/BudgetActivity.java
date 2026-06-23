package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BudgetActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private SharedPreferences budgetPrefs;
    private TextView tvSisaBudget;
    private TextView tvInfoBudget;
    private TextView tvBudgetUsed;
    private TextView tvBudgetTotal;
    private TextView tvBudgetPercentBadge;
    private TextView tvBudgetStatusTitle;
    private TextView tvBudgetStatusDescription;
    private ImageView ivBudgetStatusIcon;
    private FrameLayout budgetStatusIconContainer;
    private ProgressBar progressBudget;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Taruh baris ini di dalam fungsi onCreate() kamu
        getOnBackPressedDispatcher().addCallback(this, new androidx.activity.OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });

        // 1. PASTIKAN NAMA LAYOUT SESUAI
        setContentView(R.layout.activity_budget);
        EditText etTargetBudget =
                findViewById(R.id.etTargetBudget);

        Button btnUpdateBudget =
                findViewById(R.id.btnUpdateBudget);

        tvSisaBudget = findViewById(R.id.tvSisaBudget);
        tvInfoBudget = findViewById(R.id.tvInfoBudget);
        tvBudgetUsed = findViewById(R.id.tvBudgetUsed);
        tvBudgetTotal = findViewById(R.id.tvBudgetTotal);
        tvBudgetPercentBadge = findViewById(R.id.tvBudgetPercentBadge);
        tvBudgetStatusTitle = findViewById(R.id.tvBudgetStatusTitle);
        tvBudgetStatusDescription = findViewById(R.id.tvBudgetStatusDescription);
        ivBudgetStatusIcon = findViewById(R.id.ivBudgetStatusIcon);
        budgetStatusIconContainer = findViewById(R.id.budgetStatusIconContainer);
        progressBudget = findViewById(R.id.progressBudget);

        dbHelper = new DatabaseHelper(this);
        budgetPrefs = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);

        updateBudgetData();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 2. NYALAKAN IKON SESUAI HALAMAN SAAT INI
        bottomNavigationView.setSelectedItemId(R.id.nav_budget);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_pengeluaran) {
                startActivity(new Intent(getApplicationContext(), PengeluaranActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_budget) {
                // 3. KALAU KLIK BUDGET PADAHAL LAGI DI BUDGET, DIAM SAJA
                return true;
            } else if (itemId == R.id.nav_riwayat) {
                startActivity(new Intent(getApplicationContext(), RiwayatActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });

        // Logika untuk tombol "+"
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TambahTransaksiActivity.class));
        });

        btnUpdateBudget.setOnClickListener(v -> {

            String input =
                    etTargetBudget.getText().toString();

            if(input.isEmpty()){

                Toast.makeText(
                        this,
                        "Masukkan budget dulu",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            float budgetBaru =
                    Float.parseFloat(input);

            SharedPreferences.Editor editor =
                    budgetPrefs.edit();

            editor.putFloat(
                    "budget",
                    budgetBaru
            );

            editor.apply();

            Toast.makeText(
                    this,
                    "Budget berhasil disimpan",
                    Toast.LENGTH_SHORT
            ).show();

            recreate();
        });
    }

    // FUNGSI TOMBOL BACK DI HP BIAR SELALU BALIK KE HOME

    private void updateBudgetData() {
        double totalBudget = budgetPrefs.getFloat("budget", 5000000);
        double usedThisMonth = calculateCurrentMonthExpense();
        double remainingBudget = totalBudget - usedThisMonth;
        double usedPercentage = 0;

        if (totalBudget > 0) {
            usedPercentage = (usedThisMonth / totalBudget) * 100;
        } else {
            remainingBudget = 0;
        }

        int progress = (int) Math.round(usedPercentage);
        if (progress < 0) {
            progress = 0;
        } else if (progress > 100) {
            progress = 100;
        }

        tvSisaBudget.setText(formatSignedRupiah(remainingBudget));
        tvBudgetUsed.setText("Terpakai " + formatRupiah(usedThisMonth));
        tvBudgetTotal.setText("dari " + formatRupiah(totalBudget));
        tvBudgetPercentBadge.setText(formatPercentage(usedPercentage) + " terpakai");
        tvInfoBudget.setText(formatPercentage(usedPercentage) + " dari total " + formatRupiah(totalBudget) + " sudah terpakai");
        progressBudget.setProgress(progress);
        updateBudgetStatus(usedPercentage);
    }

    private double calculateCurrentMonthExpense() {
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

                if ("Pengeluaran".equals(type) && isSameMonthYear(dateText, currentCalendar)) {
                    total += cursor.getDouble(amountIndex);
                }
            }
        } finally {
            cursor.close();
        }

        return total;
    }

    private boolean isSameMonthYear(String dateText, Calendar targetCalendar) {
        Calendar transactionCalendar = parseTransactionDate(dateText);
        if (transactionCalendar == null) {
            return false;
        }

        return transactionCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR)
                && transactionCalendar.get(Calendar.MONTH) == targetCalendar.get(Calendar.MONTH);
    }

    private Calendar parseTransactionDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

        SimpleDateFormat dbDateFormat = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        dbDateFormat.setLenient(false);

        try {
            Date parsedDate = dbDateFormat.parse(dateText.trim());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            return calendar;
        } catch (ParseException e) {
            return null;
        }
    }

    private String formatRupiah(double amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return "Rp " + numberFormat.format(Math.abs(amount));
    }

    private String formatSignedRupiah(double amount) {
        if (amount < 0) {
            return "- " + formatRupiah(amount);
        }
        return formatRupiah(amount);
    }

    private String formatPercentage(double percentage) {
        if (Math.abs(percentage - Math.round(percentage)) < 0.05) {
            return String.format(Locale.getDefault(), "%.0f%%", percentage);
        }
        return String.format(new Locale("id", "ID"), "%.1f%%", percentage);
    }

    private void updateBudgetStatus(double usedPercentage) {
        if (usedPercentage < 80) {
            ivBudgetStatusIcon.setImageResource(R.drawable.ic_check_budget);
            budgetStatusIconContainer.setBackground(createRoundedBackground("#D8EFD4", 14));
            tvBudgetStatusTitle.setText("Pengeluaran masih terkendali");
            tvBudgetStatusDescription.setText("Kamu masih dalam batas budget bulan ini. Terus pertahankan pola baik ini sampai akhir bulan nanti.");
        } else if (usedPercentage <= 100) {
            ivBudgetStatusIcon.setImageResource(R.drawable.ic_warning_budget);
            budgetStatusIconContainer.setBackground(createRoundedBackground("#FFF0B8", 14));
            tvBudgetStatusTitle.setText("Budget hampir mencapai batas");
            tvBudgetStatusDescription.setText("Sisa budget kamu tinggal sedikit. Pertimbangkan untuk mengurangi pengeluaran tidak mendesak.");
        } else {
            ivBudgetStatusIcon.setImageResource(R.drawable.ic_cancel_budget);
            budgetStatusIconContainer.setBackground(createRoundedBackground("#FFDAD6", 14));
            tvBudgetStatusTitle.setText("Pengeluaran melebihi budget");
            tvBudgetStatusDescription.setText("Kamu sudah melewati batas budget bulan ini. Tinjau pengeluaranmu.");
        }
    }

    private GradientDrawable createRoundedBackground(String colorHex, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.parseColor(colorHex));
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

}
