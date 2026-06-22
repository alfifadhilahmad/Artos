package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.database.Cursor;

import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;

public class RiwayatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. Target ke layout riwayat
        setContentView(R.layout.activity_riwayat);

        RecyclerView rvRiwayat =
                findViewById(R.id.rvRiwayat);

        rvRiwayat.setLayoutManager(
                new LinearLayoutManager(this));

        ArrayList<Transaction> transactionList =
                new ArrayList<>();

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

            transactionList.add(
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

        Collections.sort(transactionList, (firstTransaction, secondTransaction) -> {
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

        TransactionAdapter adapter =
                new TransactionAdapter(transactionList);

        rvRiwayat.setAdapter(adapter);

        Toast.makeText(
                this,
                "List size: " + transactionList.size(),
                Toast.LENGTH_LONG
        ).show();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        // 2. Ikon riwayat menyala
        bottomNavigationView.setSelectedItemId(R.id.nav_riwayat);

        // 3. Logika Navigasi Bawah
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
                startActivity(new Intent(getApplicationContext(), BudgetActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_riwayat) {
                // Kalau klik Riwayat padahal lagi di Riwayat, diam saja
                return true;
            }
            return false;
        });

        // 4. Logika untuk tombol "+"
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(getApplicationContext(), TambahTransaksiActivity.class));
        });

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
}
