package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;

import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class RiwayatActivity extends AppCompatActivity {
    private static final String FILTER_SEMUA = "Semua";
    private static final int CHIP_ACTIVE_TEXT_COLOR = 0xFFFFFFFF;
    private static final int CHIP_INACTIVE_TEXT_COLOR = 0xFF8A8F83;

    private final ArrayList<Transaction> allTransactions = new ArrayList<>();
    private RecyclerView rvRiwayat;
    private TextView tvRiwayatEmpty;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = FILTER_SEMUA;
    private ChipFilterOption[] chipFilterOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Target ke layout riwayat
        setContentView(R.layout.activity_riwayat);

        rvRiwayat =
                findViewById(R.id.rvRiwayat);
        tvRiwayatEmpty =
                findViewById(R.id.tvRiwayatEmpty);
        EditText etSearchRiwayat =
                findViewById(R.id.etSearchRiwayat);

        rvRiwayat.setLayoutManager(
                new LinearLayoutManager(this));

        DatabaseHelper dbHelper =
                new DatabaseHelper(this);

        Cursor cursor =
                dbHelper.getAllTransactions();
        Toast.makeText(
                this,
                "Jumlah data: " + cursor.getCount(),
                Toast.LENGTH_LONG
        ).show();


        while(cursor.moveToNext()){

            int id =
                    cursor.getInt(0);

            String type =
                    cursor.getString(1);

            double amount =
                    cursor.getDouble(2);

            String category =
                    cursor.getString(3);

            String note =
                    cursor.getString(4);

            String date =
                    cursor.getString(5);

            allTransactions.add(
                    new Transaction(
                            id,
                            type,
                            amount,
                            category,
                            note,
                            date
                    )
            );
        }

        cursor.close();

        applyFiltersAndRender();

        etSearchRiwayat.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s == null ? "" : s.toString();
                applyFiltersAndRender();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        etSearchRiwayat.setOnEditorActionListener((view, actionId, event) -> {
            boolean isSearchAction = actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE;
            boolean isEnterKey = event != null
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER
                    && event.getAction() == KeyEvent.ACTION_UP;

            if (isSearchAction || isEnterKey) {
                hideKeyboardAndClearFocus(etSearchRiwayat);
                return true;
            }

            return false;
        });
        setupFilterChips();

        Toast.makeText(
                this,
                "List size: " + allTransactions.size(),
                Toast.LENGTH_LONG
        ).show();

        setupCustomBottomNavigation();

        // 5. FUNGSI TOMBOL BACK DI HP BIAR SELALU BALIK KE HOME (Cara Baru Anti-Coret)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
            }
        });
    }

    private void setupCustomBottomNavigation() {
        findViewById(R.id.navHomeButton).setOnClickListener(v ->
                openBottomNavActivity(HomeActivity.class));

        findViewById(R.id.navPengeluaranButton).setOnClickListener(v ->
                openBottomNavActivity(PengeluaranActivity.class));

        findViewById(R.id.navBudgetButton).setOnClickListener(v ->
                openBottomNavActivity(BudgetActivity.class));

        findViewById(R.id.navRiwayatButton).setOnClickListener(v -> {
            // Already on Riwayat.
        });

        findViewById(R.id.fabAdd).setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), TambahTransaksiActivity.class)));
    }

    private void openBottomNavActivity(Class<?> destinationActivity) {
        startActivity(new Intent(getApplicationContext(), destinationActivity));
        overridePendingTransition(0, 0);
        finish();
    }

    private void applyFiltersAndRender() {
        ArrayList<Transaction> filteredTransactions =
                filterTransactions();

        sortTransactions(filteredTransactions);

        TransactionAdapter adapter =
                new TransactionAdapter(filteredTransactions);

        rvRiwayat.setAdapter(adapter);

        if (filteredTransactions.isEmpty()) {
            rvRiwayat.setVisibility(View.GONE);
            tvRiwayatEmpty.setVisibility(View.VISIBLE);
        } else {
            tvRiwayatEmpty.setVisibility(View.GONE);
            rvRiwayat.setVisibility(View.VISIBLE);
        }
    }

    private void setupFilterChips() {
        chipFilterOptions = new ChipFilterOption[] {
                new ChipFilterOption(FILTER_SEMUA, R.id.chipSemua),
                new ChipFilterOption("Pemasukan", R.id.chipPemasukan),
                new ChipFilterOption("Makanan", R.id.chipMakanan),
                new ChipFilterOption("Camilan", R.id.chipCamilan),
                new ChipFilterOption("Belanja", R.id.chipBelanja),
                new ChipFilterOption("Tagihan", R.id.chipTagihan),
                new ChipFilterOption("Kesehatan", R.id.chipKesehatan),
                new ChipFilterOption("Edukasi", R.id.chipEdukasi),
                new ChipFilterOption("Hiburan", R.id.chipHiburan),
                new ChipFilterOption("Tabungan", R.id.chipTabungan),
                new ChipFilterOption("Sewa Kos", R.id.chipSewaKos),
                new ChipFilterOption("Lainnya", R.id.chipLainnya)
        };

        for (ChipFilterOption option : chipFilterOptions) {
            TextView chip = findViewById(option.viewId);
            chip.setOnClickListener(v -> {
                currentCategoryFilter = option.filterValue;
                updateChipSelection();
                applyFiltersAndRender();
            });
        }

        updateChipSelection();
    }

    private void updateChipSelection() {
        for (ChipFilterOption option : chipFilterOptions) {
            TextView chip = findViewById(option.viewId);
            boolean isActive = option.filterValue.equals(currentCategoryFilter);
            chip.setBackgroundResource(
                    isActive
                            ? R.drawable.riwayat_chip_active_bg
                            : R.drawable.riwayat_chip_inactive_bg
            );
            chip.setTextColor(
                    isActive
                            ? CHIP_ACTIVE_TEXT_COLOR
                            : CHIP_INACTIVE_TEXT_COLOR
            );
        }
    }

    private void hideKeyboardAndClearFocus(View focusedView) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(focusedView.getWindowToken(), 0);
        }

        focusedView.clearFocus();
    }

    private ArrayList<Transaction> filterTransactions() {
        ArrayList<Transaction> filteredTransactions = new ArrayList<>();
        String normalizedQuery = currentSearchQuery == null
                ? ""
                : currentSearchQuery.trim().toLowerCase(Locale.ROOT);

        for (Transaction transaction : allTransactions) {
            if (matchesCategoryFilter(transaction)
                    && matchesSearchQuery(transaction, normalizedQuery)) {
                filteredTransactions.add(transaction);
            }
        }

        return filteredTransactions;
    }

    private boolean matchesCategoryFilter(Transaction transaction) {
        if (FILTER_SEMUA.equals(currentCategoryFilter)) {
            return true;
        }

        if ("Pemasukan".equals(currentCategoryFilter)) {
            return "Pemasukan".equalsIgnoreCase(transaction.getType())
                    || "Pemasukan".equalsIgnoreCase(transaction.getCategory());
        }

        return currentCategoryFilter.equalsIgnoreCase(transaction.getCategory());
    }

    private boolean matchesSearchQuery(Transaction transaction, String normalizedQuery) {
        if (normalizedQuery.isEmpty()) {
            return true;
        }

        return containsQuery(transaction.getNote(), normalizedQuery)
                || containsQuery(transaction.getCategory(), normalizedQuery)
                || containsQuery(transaction.getType(), normalizedQuery);
    }

    private boolean containsQuery(String value, String query) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void sortTransactions(ArrayList<Transaction> transactions) {
        Collections.sort(transactions, (firstTransaction, secondTransaction) -> {
            ParsedTransactionDate firstDate =
                    parseTransactionDate(firstTransaction.getDate());
            ParsedTransactionDate secondDate =
                    parseTransactionDate(secondTransaction.getDate());

            if (firstDate.validDate != secondDate.validDate) {
                return firstDate.validDate ? -1 : 1;
            }

            if (firstDate.validDate) {
                if (firstDate.year != secondDate.year) {
                    return Integer.compare(secondDate.year, firstDate.year);
                }

                if (firstDate.month != secondDate.month) {
                    return Integer.compare(secondDate.month, firstDate.month);
                }

                if (firstDate.day != secondDate.day) {
                    return Integer.compare(secondDate.day, firstDate.day);
                }
            }

            return Integer.compare(secondTransaction.getId(), firstTransaction.getId());
        });
    }

    private ParsedTransactionDate parseTransactionDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return ParsedTransactionDate.invalid();
        }

        String[] dateParts = dateText.trim().split("/");

        if (dateParts.length != 3) {
            return ParsedTransactionDate.invalid();
        }

        try {
            int day = Integer.parseInt(dateParts[0]);
            int month = Integer.parseInt(dateParts[1]);
            int year = Integer.parseInt(dateParts[2]);

            if (month < 1 || month > 12) {
                return ParsedTransactionDate.invalid();
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setLenient(false);
            calendar.clear();
            calendar.set(year, month - 1, day);
            calendar.getTime();

            return new ParsedTransactionDate(true, day, month, year);
        } catch (IllegalArgumentException exception) {
            return ParsedTransactionDate.invalid();
        }
    }

    private static class ParsedTransactionDate {
        boolean validDate;
        int day;
        int month;
        int year;

        ParsedTransactionDate(boolean validDate, int day, int month, int year) {
            this.validDate = validDate;
            this.day = day;
            this.month = month;
            this.year = year;
        }

        static ParsedTransactionDate invalid() {
            return new ParsedTransactionDate(false, 0, 0, 0);
        }
    }

    private static class ChipFilterOption {
        String filterValue;
        int viewId;

        ChipFilterOption(String filterValue, int viewId) {
            this.filterValue = filterValue;
            this.viewId = viewId;
        }
    }
}
