package com.java.kalkulatorkeuangan;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;
import java.util.Locale;

public class TambahTransaksiActivity extends AppCompatActivity {
    private String selectedDateForDb;
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private boolean isFormattingNominal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tambah_transaksi);

        View btnBatal = findViewById(R.id.btnBatal);
        EditText etNominal = findViewById(R.id.etNominal);
        EditText etCatatan = findViewById(R.id.etCatatan);
        TextView tvTanggal = findViewById(R.id.tvTanggal);
        RadioGroup radioGroup = findViewById(R.id.radioGroupTipe);
        RadioButton radioPengeluaran = findViewById(R.id.radioPengeluaran);
        RadioButton radioPemasukan = findViewById(R.id.radioPemasukan);
        Spinner spKategori = findViewById(R.id.spKategori);
        View btnSimpan = findViewById(R.id.btnSimpan);

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

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            radioPengeluaran.setTextColor(
                    checkedId == R.id.radioPengeluaran
                            ? android.graphics.Color.parseColor("#306D29")
                            : android.graphics.Color.parseColor("#262A24"));
            radioPemasukan.setTextColor(
                    checkedId == R.id.radioPemasukan
                            ? android.graphics.Color.parseColor("#306D29")
                            : android.graphics.Color.parseColor("#262A24"));
        });

        Calendar today = Calendar.getInstance();
        setSelectedDate(
                tvTanggal,
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH),
                today.get(Calendar.DAY_OF_MONTH)
        );

        etNominal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormattingNominal) {
                    return;
                }

                String cleanedValue = cleanNominalInput(s.toString());
                String formattedValue = formatNominalInput(cleanedValue);

                isFormattingNominal = true;
                etNominal.setText(formattedValue);
                etNominal.setSelection(formattedValue.length());
                isFormattingNominal = false;
            }
        });

        // =========================
        // 2. DATE PICKER
        // =========================
        View.OnClickListener dateClickListener = v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    TambahTransaksiActivity.this,
                    (view, year, month, day) -> setSelectedDate(tvTanggal, year, month, day),
                    selectedYear,
                    selectedMonth,
                    selectedDay
            );

            datePickerDialog.show();
        };
        tvTanggal.setOnClickListener(dateClickListener);
        ((View) tvTanggal.getParent()).setOnClickListener(dateClickListener);

        // =========================
        // 3. SIMPAN TRANSAKSI
        // =========================
        btnSimpan.setOnClickListener(v -> {

            String nominalText = cleanNominalInput(etNominal.getText().toString());
            String catatan = etCatatan.getText().toString();
            String tanggal = selectedDateForDb;

            if (nominalText.isEmpty()) {
                Toast.makeText(this, "Nominal wajib diisi", Toast.LENGTH_SHORT).show();
                return;
            }

            int selectedId = radioGroup.getCheckedRadioButtonId();

            if (selectedId == -1) {
                Toast.makeText(this, "Pilih Pemasukan / Pengeluaran", Toast.LENGTH_SHORT).show();
                return;
            }

            String tipe = selectedId == R.id.radioPemasukan ? "Pemasukan" : "Pengeluaran";

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

    private void setSelectedDate(TextView tvTanggal, int year, int month, int day) {
        selectedYear = year;
        selectedMonth = month;
        selectedDay = day;
        selectedDateForDb = day + "/" + (month + 1) + "/" + year;
        tvTanggal.setText(formatDisplayDate(year, month, day));
    }

    private String formatDisplayDate(int year, int month, int day) {
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.clear();
        selectedDate.set(year, month, day);

        String dateText = day + " " + getIndonesianMonthName(month) + " "
                + String.format(Locale.US, "%02d", year % 100);

        Calendar today = Calendar.getInstance();
        clearTime(today);

        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DAY_OF_MONTH, -1);

        if (isSameDate(selectedDate, today)) {
            return "Hari ini, " + dateText;
        }

        if (isSameDate(selectedDate, yesterday)) {
            return "Kemarin, " + dateText;
        }

        return getIndonesianDayName(selectedDate) + ", " + dateText;
    }

    private void clearTime(Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    private boolean isSameDate(Calendar firstDate, Calendar secondDate) {
        return firstDate.get(Calendar.YEAR) == secondDate.get(Calendar.YEAR)
                && firstDate.get(Calendar.MONTH) == secondDate.get(Calendar.MONTH)
                && firstDate.get(Calendar.DAY_OF_MONTH) == secondDate.get(Calendar.DAY_OF_MONTH);
    }

    private String getIndonesianDayName(Calendar date) {
        String[] dayNames = {
                "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
        };
        return dayNames[date.get(Calendar.DAY_OF_WEEK) - 1];
    }

    private String getIndonesianMonthName(int month) {
        String[] monthNames = {
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        };
        return monthNames[month];
    }

    private String cleanNominalInput(String value) {
        return value
                .replace("Rp", "")
                .replace(".", "")
                .replace(" ", "")
                .replaceAll("[^0-9]", "");
    }

    private String formatNominalInput(String value) {
        if (value.isEmpty()) {
            return "";
        }

        try {
            return String.format(Locale.US, "%,d", Long.parseLong(value)).replace(",", ".");
        } catch (NumberFormatException exception) {
            return value;
        }
    }
}
