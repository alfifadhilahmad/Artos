package com.java.kalkulatorkeuangan;

import android.app.DatePickerDialog;
import android.app.AlertDialog;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class UpdateTransaksiActivity extends AppCompatActivity {
    public static final String EXTRA_TRANSACTION_ID = "transaction_id";

    private static final String TYPE_PENGELUARAN = "Pengeluaran";
    private static final String TYPE_PEMASUKAN = "Pemasukan";
    private static final String CATEGORY_PEMASUKAN = "Pemasukan";
    private static final String CATEGORY_MAKANAN = "Makanan";
    private static final String CATEGORY_LAINNYA = "Lainnya";
    private static final int CATEGORY_INACTIVE_COLOR = 0xFF555C50;

    private int transactionId = -1;
    private String originalType = "";
    private String selectedDbDate = "";
    private String selectedCategory = CATEGORY_MAKANAN;
    private int selectedYear;
    private int selectedMonth;
    private int selectedDay;
    private boolean isFormattingNominal;
    private CategoryOption[] categoryOptions;
    private EditText etNominal;
    private EditText etCatatan;
    private TextView tvTanggal;
    private RadioGroup radioGroup;
    private RadioButton radioPengeluaran;
    private RadioButton radioPemasukan;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_transaksi);

        transactionId = getIntent().getIntExtra(EXTRA_TRANSACTION_ID, -1);
        if (transactionId <= 0) {
            Toast.makeText(this, "Transaksi tidak valid", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etNominal = findViewById(R.id.etNominal);
        etCatatan = findViewById(R.id.etCatatan);
        tvTanggal = findViewById(R.id.tvTanggal);
        radioGroup = findViewById(R.id.radioGroupTipe);
        radioPengeluaran = findViewById(R.id.radioPengeluaran);
        radioPemasukan = findViewById(R.id.radioPemasukan);
        View btnHapus = findViewById(R.id.btnHapus);
        View btnSimpan = findViewById(R.id.btnSimpan);

        setupCategorySelector();

        etNominal.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (isFormattingNominal) {
                    return;
                }

                String cleanedValue = cleanNominalInput(editable.toString());
                String formattedValue = formatNominalInput(cleanedValue);

                isFormattingNominal = true;
                etNominal.setText(formattedValue);
                etNominal.setSelection(formattedValue.length());
                isFormattingNominal = false;
            }
        });

        loadTransactionData();

        View.OnClickListener dateClickListener = v -> {
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    UpdateTransaksiActivity.this,
                    (view, year, month, day) -> setSelectedDate(tvTanggal, year, month, day),
                    selectedYear,
                    selectedMonth,
                    selectedDay
            );

            datePickerDialog.show();
        };
        tvTanggal.setOnClickListener(dateClickListener);
        ((View) tvTanggal.getParent()).setOnClickListener(dateClickListener);

        btnHapus.setOnClickListener(v -> showDeleteConfirmationDialog());

        btnSimpan.setOnClickListener(v -> saveTransactionChanges());
    }

    private void setupCategorySelector() {
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
            View categoryView = findViewById(option.rootId);
            categoryView.setOnClickListener(v -> {
                if (canSelectCategory(option.name)) {
                    updateCategorySelection(option.name);
                }
            });
        }
    }

    private void updateCategorySelection(String category) {
        selectedCategory = category;

        for (CategoryOption option : categoryOptions) {
            boolean isSelected = option.name.equals(selectedCategory);
            ImageView icon = findViewById(option.iconId);
            TextView label = findViewById(option.labelId);

            icon.setImageResource(isSelected ? option.activeIconRes : option.inactiveIconRes);
            label.setTextColor(isSelected ? option.selectedColor : CATEGORY_INACTIVE_COLOR);
        }
    }

    private void loadTransactionData() {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Cursor cursor = dbHelper.getTransactionById(transactionId);

        try {
            if (cursor == null || !cursor.moveToFirst()) {
                Toast.makeText(this, "Transaksi tidak ditemukan", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            String type = cursor.getString(1);
            double amount = cursor.getDouble(2);
            String category = cursor.getString(3);
            String note = cursor.getString(4);
            String date = cursor.getString(5);

            originalType = safeText(type);
            selectedDbDate = safeText(date);

            applyOriginalType();
            configureCategoryAvailability();
            prefillAmount(amount);
            prefillDate(selectedDbDate);
            etCatatan.setText(safeText(note));
            updateCategorySelection(resolveCategory(category));
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private void applyOriginalType() {
        if (TYPE_PEMASUKAN.equals(originalType)) {
            radioGroup.check(R.id.radioPemasukan);
        } else {
            originalType = TYPE_PENGELUARAN;
            radioGroup.check(R.id.radioPengeluaran);
        }

        radioPengeluaran.setEnabled(false);
        radioPemasukan.setEnabled(false);
        radioPengeluaran.setAlpha(TYPE_PENGELUARAN.equals(originalType) ? 1f : 0.55f);
        radioPemasukan.setAlpha(TYPE_PEMASUKAN.equals(originalType) ? 1f : 0.55f);
        radioPengeluaran.setTextColor(
                TYPE_PENGELUARAN.equals(originalType) ? 0xFF306D29 : 0xFF262A24);
        radioPemasukan.setTextColor(
                TYPE_PEMASUKAN.equals(originalType) ? 0xFF306D29 : 0xFF262A24);
    }

    private void configureCategoryAvailability() {
        for (CategoryOption option : categoryOptions) {
            View categoryView = findViewById(option.rootId);
            boolean isAvailable = canSelectCategory(option.name)
                    || (TYPE_PEMASUKAN.equals(originalType) && CATEGORY_PEMASUKAN.equals(option.name));
            categoryView.setEnabled(isAvailable);
            categoryView.setAlpha(isAvailable ? 1f : 0.55f);
        }
    }

    private boolean canSelectCategory(String category) {
        if (TYPE_PEMASUKAN.equals(originalType)) {
            return CATEGORY_PEMASUKAN.equals(category);
        }

        return !CATEGORY_PEMASUKAN.equals(category);
    }

    private void prefillAmount(double amount) {
        long roundedAmount = Math.round(amount);
        etNominal.setText(formatNominalInput(String.valueOf(roundedAmount)));
    }

    private void prefillDate(String dbDate) {
        Calendar parsedDate = parseDbDate(dbDate);
        if (parsedDate == null) {
            Calendar today = Calendar.getInstance();
            selectedYear = today.get(Calendar.YEAR);
            selectedMonth = today.get(Calendar.MONTH);
            selectedDay = today.get(Calendar.DAY_OF_MONTH);
            tvTanggal.setText(dbDate == null || dbDate.trim().isEmpty()
                    ? formatDisplayDate(selectedYear, selectedMonth, selectedDay)
                    : dbDate);
            return;
        }

        setSelectedDate(
                tvTanggal,
                parsedDate.get(Calendar.YEAR),
                parsedDate.get(Calendar.MONTH),
                parsedDate.get(Calendar.DAY_OF_MONTH)
        );
    }

    private Calendar parseDbDate(String dbDate) {
        if (dbDate == null || dbDate.trim().isEmpty()) {
            return null;
        }

        SimpleDateFormat dbDateFormat =
                new SimpleDateFormat("d/M/yyyy", new Locale("id", "ID"));
        dbDateFormat.setLenient(false);

        try {
            Date parsedDate = dbDateFormat.parse(dbDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            return calendar;
        } catch (ParseException exception) {
            return null;
        }
    }

    private String resolveCategory(String category) {
        String safeCategory = safeText(category);

        if (TYPE_PEMASUKAN.equals(originalType)) {
            return CATEGORY_PEMASUKAN;
        }

        for (CategoryOption option : categoryOptions) {
            if (option.name.equals(safeCategory) && !CATEGORY_PEMASUKAN.equals(option.name)) {
                return safeCategory;
            }
        }

        return CATEGORY_LAINNYA;
    }

    private void saveTransactionChanges() {
        if (transactionId <= 0) {
            Toast.makeText(this, "Transaksi tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        String cleanedAmount = cleanNominalInput(etNominal.getText().toString());
        if (cleanedAmount.isEmpty()) {
            Toast.makeText(this, "Masukkan nominal yang valid", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(cleanedAmount);
        } catch (NumberFormatException exception) {
            Toast.makeText(this, "Masukkan nominal yang valid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (amount <= 0) {
            Toast.makeText(this, "Masukkan nominal yang valid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedDbDate == null || selectedDbDate.trim().isEmpty()
                || parseDbDate(selectedDbDate) == null) {
            Toast.makeText(this, "Tanggal tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isValidSelectedCategory()) {
            Toast.makeText(this, "Pilih kategori terlebih dahulu", Toast.LENGTH_SHORT).show();
            return;
        }

        String note = etCatatan.getText().toString().trim();
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean success = dbHelper.updateTransaction(
                transactionId,
                originalType,
                amount,
                selectedCategory,
                note,
                selectedDbDate
        );

        if (success) {
            Toast.makeText(this, "Transaksi berhasil diperbarui", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Gagal memperbarui transaksi", Toast.LENGTH_SHORT).show();
        }
    }

    private void showDeleteConfirmationDialog() {
        if (transactionId <= 0) {
            Toast.makeText(this, "Transaksi tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Hapus Transaksi?")
                .setMessage("Transaksi yang dihapus tidak dapat dikembalikan.")
                .setNegativeButton("Batal", null)
                .setPositiveButton("Hapus", (dialogInterface, which) -> deleteCurrentTransaction())
                .create();

        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                    .setTextColor(Color.parseColor("#306D29"));
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    .setTextColor(Color.parseColor("#B3261E"));
        });

        dialog.show();
    }

    private void deleteCurrentTransaction() {
        if (transactionId <= 0) {
            Toast.makeText(this, "Transaksi tidak valid", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseHelper dbHelper = new DatabaseHelper(this);
        boolean success = dbHelper.deleteTransaction(transactionId);

        if (success) {
            Toast.makeText(this, "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Gagal menghapus transaksi", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidSelectedCategory() {
        if (selectedCategory == null || selectedCategory.trim().isEmpty()) {
            return false;
        }

        if (TYPE_PEMASUKAN.equals(originalType)) {
            return CATEGORY_PEMASUKAN.equals(selectedCategory);
        }

        if (CATEGORY_PEMASUKAN.equals(selectedCategory)) {
            return false;
        }

        for (CategoryOption option : categoryOptions) {
            if (option.name.equals(selectedCategory)) {
                return true;
            }
        }

        return false;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private void setSelectedDate(TextView tvTanggal, int year, int month, int day) {
        selectedYear = year;
        selectedMonth = month;
        selectedDay = day;
        selectedDbDate = day + "/" + (month + 1) + "/" + year;
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
