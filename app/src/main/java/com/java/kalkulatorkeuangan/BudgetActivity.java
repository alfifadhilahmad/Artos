package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.ProgressBar;

import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class BudgetActivity extends AppCompatActivity {

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

        TextView tvSisaBudget =
                findViewById(R.id.tvSisaBudget);

        TextView tvInfoBudget =
                findViewById(R.id.tvInfoBudget);

        android.widget.ProgressBar progressBudget =
                findViewById(R.id.progressBudget);

        DatabaseHelper dbHelper =
                new DatabaseHelper(this);

        double totalPengeluaran =
                dbHelper.getTotalPengeluaran();

        SharedPreferences prefs =
                getSharedPreferences(
                        "BudgetPrefs",
                        MODE_PRIVATE
                );

        double totalBudget =
                prefs.getFloat(
                        "budget",
                        5000000
                );

        double sisaBudget =
                totalBudget - totalPengeluaran;

        int persen =
                (int)((totalPengeluaran / totalBudget) * 100);

        if(persen > 100){
            persen = 100;
        }

        tvSisaBudget.setText(
                "Rp " + String.format("%.0f", sisaBudget)
        );

        tvInfoBudget.setText(
                persen + "% dari total Rp "
                        + String.format("%.0f", totalBudget)
                        + " sudah terpakai"
        );

        progressBudget.setProgress(persen);

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
                    prefs.edit();

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

}