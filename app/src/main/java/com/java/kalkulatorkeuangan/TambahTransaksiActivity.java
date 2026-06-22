package com.java.kalkulatorkeuangan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class TambahTransaksiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_transaksi);

        View btnBatal = findViewById(R.id.btnBatal);
        EditText etNominal = findViewById(R.id.etNominal);
        EditText etCatatan = findViewById(R.id.etCatatan);
        TextView tvTanggal = findViewById(R.id.tvTanggal);
        RadioGroup radioGroup = findViewById(R.id.radioGroupTipe);
        Spinner spKategori = findViewById(R.id.spKategori);
        Button btnSimpan = findViewById(R.id.btnSimpan);

        DatabaseHelper dbHelper = new DatabaseHelper(this);

        // =========================
        // 1. SETUP SPINNER (FIX DI SINI)
        // =========================
        String[] kategoriList = {"Makanan", "Transport", "Belanja", "Tagihan", "Lainnya"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                kategoriList
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spKategori.setAdapter(adapter);

        // =========================
        // 2. DATE PICKER
        // =========================
        tvTanggal.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();

            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    TambahTransaksiActivity.this,
                    (view, selectedYear, selectedMonth, selectedDay) -> {

                        String tanggal =
                                selectedDay + "/" +
                                        (selectedMonth + 1) + "/" +
                                        selectedYear;

                        tvTanggal.setText(tanggal);
                    },
                    year,
                    month,
                    day
            );

            datePickerDialog.show();
        });

        // =========================
        // 3. SIMPAN TRANSAKSI
        // =========================
        btnSimpan.setOnClickListener(v -> {

            String nominalText = etNominal.getText().toString();
            String catatan = etCatatan.getText().toString();
            String tanggal = tvTanggal.getText().toString();

            if (nominalText.isEmpty()) {
                Toast.makeText(this, "Nominal wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Pilih Pemasukan / Pengeluaran", Toast.LENGTH_SHORT).show();
                return;
            }

            RadioButton selectedRadio = findViewById(selectedId);
            String tipe = selectedRadio.getText().toString().trim();

            String kategori = spKategori.getSelectedItem().toString();

            double nominal = Double.parseDouble(nominalText);

            boolean berhasil = dbHelper.insertTransaction(
                    tipe,
                    nominal,
                    kategori,
                    catatan,
                    tanggal
            );

            if (berhasil) {
                Toast.makeText(this, "Berhasil disimpan", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Gagal menyimpan", Toast.LENGTH_SHORT).show();
            }
        });

        // =========================
        // 4. BATAL
        // =========================
        btnBatal.setOnClickListener(v -> finish());
    }
}
