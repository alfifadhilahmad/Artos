package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

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
        double totalPengeluaran = dbHelper.getTotalPengeluaran();

        // Set teks total pengeluaran header untuk bulan berjalan.
        tvTotalPengeluaran.setText(formatRupiah(totalPengeluaranBulanIni));
        tvPengeluaranMonth.setText(formatCurrentMonthLabel());

        Cursor cursor = dbHelper.getExpenseAnalysis();

        // Siapkan alat untuk "mencetak" XML item_analisis.xml
        LayoutInflater inflater = LayoutInflater.from(this);

        if (cursor.moveToFirst()) {
            do {
                String kategori = cursor.getString(0);
                double totalKategori = cursor.getDouble(1);

                // Cegah pembagian dengan nol biar aplikasi nggak crash
                double persen = 0;
                if (totalPengeluaran > 0) {
                    persen = (totalKategori / totalPengeluaran) * 100;
                }

                // 1. "Cetak" layout item_analisis.xml menjadi objek View
                View itemView = inflater.inflate(R.layout.item_analisis, containerKategori, false);

                // 2. Cari elemen-elemen di DALAM cetakan tersebut
                TextView tvNama = itemView.findViewById(R.id.tvNamaKategori);
                TextView tvPersen = itemView.findViewById(R.id.tvPersenKategori);
                ProgressBar pbKategori = itemView.findViewById(R.id.pbKategori);
                TextView tvTotal = itemView.findViewById(R.id.tvTotalKategori);

                // 3. Masukkan data dari database ke elemen-elemen tersebut
                tvNama.setText(kategori);
                tvPersen.setText(String.format("%.1f", persen) + "%");
                pbKategori.setProgress((int) persen);
                tvTotal.setText("Rp " + String.format("%.0f", totalKategori));

                // 4. Tempelkan cetakan yang sudah diisi data ke dalam container utama di layar
                containerKategori.addView(itemView);

            } while (cursor.moveToNext());
        }

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

    private String formatCurrentMonthLabel() {
        Calendar calendar = Calendar.getInstance();
        int monthIndex = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        return MONTH_NAMES[monthIndex] + " " + year;
    }
}
