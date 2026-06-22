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
    private static final String CATEGORY_PEMASUKAN = "Pemasukan";
    private static final String CATEGORY_MAKANAN = "Makanan";
    private static final int CATEGORY_INACTIVE_COLOR = 0xFF555C50;

    private String selectedDateForDb;
    private String selectedCategory = CATEGORY_MAKANAN;
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private boolean isFormattingNominal;
    private boolean isSyncingTypeAndCategory;
    private CategoryOption[] categoryOptions;

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

        setupCategorySelector(radioGroup);

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            updateTypeSegmentColors(checkedId, radioPengeluaran, radioPemasukan);

            if (isSyncingTypeAndCategory) {
                return;
            }

            if (checkedId == R.id.radioPemasukan) {
                updateCategorySelection(CATEGORY_PEMASUKAN, radioGroup);
            } else if (CATEGORY_PEMASUKAN.equals(selectedCategory)) {
                updateCategorySelection(CATEGORY_MAKANAN, radioGroup);
            }
        });
        updateCategorySelection(CATEGORY_MAKANAN, radioGroup);

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

            String kategori = selectedCategory;

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

    private void setupCategorySelector(RadioGroup radioGroup) {
        categoryOptions = new CategoryOption[] {
                new CategoryOption(CATEGORY_PEMASUKAN, R.id.catPemasukan, R.id.ivCatPemasukan,
                        R.id.tvCatPemasukan, R.drawable.ic_category_income_active,
                        R.drawable.ic_category_income_inactive, 0xFF306D29),
                new CategoryOption(CATEGORY_MAKANAN, R.id.catMakanan, R.id.ivCatMakanan,
                        R.id.tvCatMakanan, R.drawable.ic_category_food_active,
                        R.drawable.ic_category_food_inactive, 0xFF8A4600),
                new CategoryOption("Camilan", R.id.catCamilan, R.id.ivCatCamilan,
                        R.id.tvCatCamilan, R.drawable.ic_category_snack_active,
                        R.drawable.ic_category_snack_inactive, 0xFF0053A4),
                new CategoryOption("Belanja", R.id.catBelanja, R.id.ivCatBelanja,
                        R.id.tvCatBelanja, R.drawable.ic_category_shopping_active,
                        R.drawable.ic_category_shopping_inactive, 0xFF6E28A8),
                new CategoryOption("Tagihan", R.id.catTagihan, R.id.ivCatTagihan,
                        R.id.tvCatTagihan, R.drawable.ic_category_bill_active,
                        R.drawable.ic_category_bill_inactive, 0xFFB3261E),
                new CategoryOption("Kesehatan", R.id.catKesehatan, R.id.ivCatKesehatan,
                        R.id.tvCatKesehatan, R.drawable.ic_category_health_active,
                        R.drawable.ic_category_health_inactive, 0xFF00796B),
                new CategoryOption("Edukasi", R.id.catEdukasi, R.id.ivCatEdukasi,
                        R.id.tvCatEdukasi, R.drawable.ic_category_education_active,
                        R.drawable.ic_category_education_inactive, 0xFF0076A8),
                new CategoryOption("Hiburan", R.id.catHiburan, R.id.ivCatHiburan,
                        R.id.tvCatHiburan, R.drawable.ic_category_entertainment_active,
                        R.drawable.ic_category_entertainment_inactive, 0xFFC2185B),
                new CategoryOption("Tabungan", R.id.catTabungan, R.id.ivCatTabungan,
                        R.id.tvCatTabungan, R.drawable.ic_category_savings_active,
                        R.drawable.ic_category_savings_inactive, 0xFF8A6D00),
                new CategoryOption("Sewa Kos", R.id.catSewaKos, R.id.ivCatSewaKos,
                        R.id.tvCatSewaKos, R.drawable.ic_category_house_active,
                        R.drawable.ic_category_house_inactive, 0xFF5145B5),
                new CategoryOption("Lainnya", R.id.catLainnya, R.id.ivCatLainnya,
                        R.id.tvCatLainnya, R.drawable.ic_category_other_active,
                        R.drawable.ic_category_other_inactive, 0xFF555C50)
        };

        for (CategoryOption option : categoryOptions) {
            findViewById(option.rootId).setOnClickListener(v ->
                    updateCategorySelection(option.name, radioGroup));
        }
    }

    private void updateCategorySelection(String category, RadioGroup radioGroup) {
        selectedCategory = category;

        for (CategoryOption option : categoryOptions) {
            boolean isSelected = option.name.equals(category);
            ImageView icon = findViewById(option.iconId);
            TextView label = findViewById(option.labelId);

            icon.setImageResource(isSelected ? option.activeIconRes : option.inactiveIconRes);
            label.setTextColor(isSelected ? option.selectedColor : CATEGORY_INACTIVE_COLOR);
        }

        isSyncingTypeAndCategory = true;
        if (CATEGORY_PEMASUKAN.equals(category)) {
            radioGroup.check(R.id.radioPemasukan);
        } else {
            radioGroup.check(R.id.radioPengeluaran);
        }
        isSyncingTypeAndCategory = false;
    }

    private void updateTypeSegmentColors(
            int checkedId,
            RadioButton radioPengeluaran,
            RadioButton radioPemasukan) {
        radioPengeluaran.setTextColor(
                checkedId == R.id.radioPengeluaran
                        ? android.graphics.Color.parseColor("#306D29")
                        : android.graphics.Color.parseColor("#262A24"));
        radioPemasukan.setTextColor(
                checkedId == R.id.radioPemasukan
                        ? android.graphics.Color.parseColor("#306D29")
                        : android.graphics.Color.parseColor("#262A24"));
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

    private static class CategoryOption {
        String name;
        int rootId;
        int iconId;
        int labelId;
        int activeIconRes;
        int inactiveIconRes;
        int selectedColor;

        CategoryOption(
                String name,
                int rootId,
                int iconId,
                int labelId,
                int activeIconRes,
                int inactiveIconRes,
                int selectedColor) {
            this.name = name;
            this.rootId = rootId;
            this.iconId = iconId;
            this.labelId = labelId;
            this.activeIconRes = activeIconRes;
            this.inactiveIconRes = inactiveIconRes;
            this.selectedColor = selectedColor;
        }
    }
}
