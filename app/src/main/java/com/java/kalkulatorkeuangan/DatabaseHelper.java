package com.java.kalkulatorkeuangan;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import android.content.ContentValues;
import android.database.Cursor;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "keuangan.db";

    // NAIK LEVEL: Ubah jadi 2 untuk nge-reset database dari sisa-sisa data lama
    private static final int DATABASE_VERSION = 2;

    public DatabaseHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_TABLE = "CREATE TABLE transactions (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "type TEXT," +
                "amount REAL," +
                "category TEXT," +
                "note TEXT," +
                "date TEXT)";

        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Ini yang bakal jalan ketika versi naik dari 1 ke 2
        db.execSQL("DROP TABLE IF EXISTS transactions");
        onCreate(db);
    }

    public boolean insertTransaction(
            String type,
            double amount,
            String category,
            String note,
            String date) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("type", type);
        values.put("amount", amount);
        values.put("category", category);
        values.put("note", note);
        values.put("date", date);

        long result = db.insert(
                "transactions",
                null,
                values);

        return result != -1;
    }

    public Cursor getAllTransactions() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM transactions ORDER BY id DESC",
                null
        );
    }

    public Cursor getLastTransactions() {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM transactions ORDER BY id DESC LIMIT 3",
                null
        );
    }

    public Cursor getTransactionById(int id) {

        SQLiteDatabase db = this.getReadableDatabase();

        return db.rawQuery(
                "SELECT * FROM transactions WHERE id = ?",
                new String[]{String.valueOf(id)}
        );
    }

    public boolean updateTransaction(
            int id,
            String type,
            double amount,
            String category,
            String note,
            String date) {

        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put("type", type);
        values.put("amount", amount);
        values.put("category", category);
        values.put("note", note);
        values.put("date", date);

        int rowsUpdated = db.update(
                "transactions",
                values,
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        return rowsUpdated > 0;
    }

    public boolean deleteTransaction(int id) {

        SQLiteDatabase db = this.getWritableDatabase();

        int rowsDeleted = db.delete(
                "transactions",
                "id = ?",
                new String[]{String.valueOf(id)}
        );

        return rowsDeleted > 0;
    }

    public double getTotalPemasukan() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount) FROM transactions WHERE type='Pemasukan'",
                null
        );

        double total = 0;

        if(cursor.moveToFirst()){
            total = cursor.getDouble(0);
        }

        cursor.close();

        return total;
    }

    public Cursor getExpenseAnalysis() {
        SQLiteDatabase db = this.getReadableDatabase();

        // Sesuaikan nama kolom menjadi category, amount, dan type
        return db.rawQuery(
                "SELECT category, SUM(amount) as total " +
                        "FROM transactions " +
                        "WHERE type = 'Pengeluaran' " +
                        "GROUP BY category",
                null
        );
    }

    public double getTotalPengeluaran() {

        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT SUM(amount) FROM transactions WHERE type='Pengeluaran'",
                null
        );

        double total = 0;

        if(cursor.moveToFirst()){
            total = cursor.getDouble(0);
        }

        cursor.close();

        return total;
    }
}
