package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.database.Cursor;
import android.graphics.Color;

import android.animation.ObjectAnimator;
import android.view.animation.DecelerateInterpolator;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        updateDashboardData();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
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

    @Override
    protected void onResume() {
        super.onResume();
        updateDashboardData();
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

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        double totalPemasukan = dbHelper.getTotalPemasukan();
        double totalPengeluaran = dbHelper.getTotalPengeluaran();
        double saldo = totalPemasukan - totalPengeluaran;

        tvSaldo.setText("Rp " + String.format("%,.0f", saldo));
        tvPemasukan.setText("Rp " + String.format("%,.0f", totalPemasukan));
        tvPengeluaran.setText("Rp " + String.format("%,.0f", totalPengeluaran));

        // --- BAGIAN 2: TRANSAKSI TERAKHIR ---
        TextView tvTransaksi1 = findViewById(R.id.tvTransaksi1);
        TextView tvTransaksi2 = findViewById(R.id.tvTransaksi2);
        TextView tvTransaksi3 = findViewById(R.id.tvTransaksi3);

        tvTransaksi1.setText("-");
        tvTransaksi2.setText("-");
        tvTransaksi3.setText("-");

        Cursor cursor = dbHelper.getLastTransactions();
        TextView[] listTv = {tvTransaksi1, tvTransaksi2, tvTransaksi3};
        int i = 0;

        while(cursor.moveToNext() && i < 3){
            String type = cursor.getString(1);
            double amount = cursor.getDouble(2);
            String note = cursor.getString(4);

            String simbol = type.equals("Pengeluaran") ? "- Rp " : "+ Rp ";
            listTv[i].setText(note + "   " + simbol + String.format("%,.0f", amount));
            i++;
        }
        cursor.close();

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

        tvBudgetBulanan.setText("Rp " + String.format("%,.0f", targetBudget));
        tvTerpakai.setText("Terpakai: Rp " + String.format("%,.0f", totalPengeluaran));
        tvPersenBudget.setText(persenTerpakai + "% digunakan");

        ObjectAnimator animation = ObjectAnimator.ofInt(pbBudget, "progress", 0, persenTerpakai);
        animation.setDuration(1200);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();

        if (sisaBudget < 0) {
            tvSisaBudget.setText("Overbudget: Rp " + String.format("%,.0f", Math.abs(sisaBudget)));
            tvSisaBudget.setTextColor(Color.RED);
            pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.RED));
        } else {
            tvSisaBudget.setText("Sisa: Rp " + String.format("%,.0f", sisaBudget));
            tvSisaBudget.setTextColor(Color.parseColor("#666666"));
            pbBudget.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
        }
    }
}