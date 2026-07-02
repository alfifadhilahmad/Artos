package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;

import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

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
    private View cardEditBudget;
    private EditText etTargetBudget;
    private boolean isFormattingBudgetInput = false;

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
        etTargetBudget = findViewById(R.id.etTargetBudget);
        Button btnUpdateBudget = findViewById(R.id.btnUpdateBudget);
        Button btnCancelEditBudget = findViewById(R.id.btnCancelEditBudget);

        tvSisaBudget = findViewById(R.id.tvSisaBudget);
        tvInfoBudget = findViewById(R.id.tvInfoBudget);
        TextView tvBudgetEdit = findViewById(R.id.tvBudgetEdit);
        tvBudgetUsed = findViewById(R.id.tvBudgetUsed);
        tvBudgetTotal = findViewById(R.id.tvBudgetTotal);
        tvBudgetPercentBadge = findViewById(R.id.tvBudgetPercentBadge);
        tvBudgetStatusTitle = findViewById(R.id.tvBudgetStatusTitle);
        tvBudgetStatusDescription = findViewById(R.id.tvBudgetStatusDescription);
        ivBudgetStatusIcon = findViewById(R.id.ivBudgetStatusIcon);
        budgetStatusIconContainer = findViewById(R.id.budgetStatusIconContainer);
        progressBudget = findViewById(R.id.progressBudget);
        cardEditBudget = findViewById(R.id.cardEditBudget);

        dbHelper = new DatabaseHelper(this);
        budgetPrefs = getSharedPreferences("BudgetPrefs", MODE_PRIVATE);

        setupEditBudgetCard(tvBudgetEdit, btnUpdateBudget, btnCancelEditBudget);
        updateBudgetData();

        setupCustomBottomNavigation();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (tvSisaBudget != null) {
            updateBudgetData();
        }
    }

    // FUNGSI TOMBOL BACK DI HP BIAR SELALU BALIK KE HOME

    private void setupEditBudgetCard(TextView tvBudgetEdit, Button btnUpdateBudget, Button btnCancelEditBudget) {
        cardEditBudget.setVisibility(View.GONE);
        setupBudgetInputFormatter();
        tvBudgetEdit.setPaintFlags(tvBudgetEdit.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        tvBudgetEdit.setOnClickListener(v -> showEditBudgetCard());
        btnCancelEditBudget.setOnClickListener(v -> hideEditBudgetCard(false));
        btnUpdateBudget.setOnClickListener(v -> saveBudgetFromInput());
    }

    private void setupCustomBottomNavigation() {
        findViewById(R.id.navHomeButton).setOnClickListener(v ->
                openBottomNavActivity(HomeActivity.class));

        findViewById(R.id.navPengeluaranButton).setOnClickListener(v ->
                openBottomNavActivity(PengeluaranActivity.class));

        findViewById(R.id.navBudgetButton).setOnClickListener(v -> {
            // Already on Budget.
        });

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

    private void showEditBudgetCard() {
        float currentBudget = budgetPrefs.getFloat("budget", 5000000);
        etTargetBudget.setText(formatPlainNumber(currentBudget));
        etTargetBudget.setSelection(etTargetBudget.getText().length());
        cardEditBudget.setVisibility(View.VISIBLE);
    }

    private void hideEditBudgetCard(boolean resetInput) {
        if (resetInput) {
            etTargetBudget.setText("");
        } else {
            float currentBudget = budgetPrefs.getFloat("budget", 5000000);
            etTargetBudget.setText(formatPlainNumber(currentBudget));
        }
        etTargetBudget.clearFocus();
        hideKeyboard();
        cardEditBudget.setVisibility(View.GONE);
    }

    private void saveBudgetFromInput() {
        Float parsedBudget = parseBudgetInput(etTargetBudget.getText().toString());

        if (parsedBudget == null || parsedBudget <= 0) {
            Toast.makeText(this, "Masukkan budget yang valid", Toast.LENGTH_SHORT).show();
            return;
        }

        budgetPrefs.edit()
                .putFloat("budget", parsedBudget)
                .apply();

        updateBudgetData();
        hideEditBudgetCard(true);
        Toast.makeText(this, "Budget berhasil diperbarui", Toast.LENGTH_SHORT).show();
    }

    private void setupBudgetInputFormatter() {
        etTargetBudget.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormattingBudgetInput) {
                    return;
                }

                String digitsOnly = editable.toString().replaceAll("[^0-9]", "");
                if (digitsOnly.isEmpty()) {
                    return;
                }

                try {
                    long amount = Long.parseLong(digitsOnly);
                    String formatted = formatPlainNumber(amount);
                    if (!formatted.equals(editable.toString())) {
                        isFormattingBudgetInput = true;
                        etTargetBudget.setText(formatted);
                        etTargetBudget.setSelection(formatted.length());
                        isFormattingBudgetInput = false;
                    }
                } catch (NumberFormatException e) {
                    isFormattingBudgetInput = true;
                    etTargetBudget.setText("");
                    isFormattingBudgetInput = false;
                }
            }
        });
    }

    private Float par/seBudgetInput(String input) {
        if (input == null) {
            return null;
        }

        String digitsOnly = input.replaceAll("[^0-9]", "");
        if (digitsOnly.isEmpty()) {
            return null;
        }

        try {
            return Float.parseFloat(digitsOnly);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatPlainNumber(double amount) {
        NumberFormat numberFormat = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        return numberFormat.format(Math.abs(amount));
    }

    private void hideKeyboard() {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(etTargetBudget.getWindowToken(), 0);
        }
    }

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
