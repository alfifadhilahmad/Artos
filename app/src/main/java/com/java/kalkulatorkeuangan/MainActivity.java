package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnLogin = findViewById(R.id.btnLogin);
        // Hubungkan dengan EditText Username di XML lu
        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);

        btnLogin.setOnClickListener(v -> {

            // Ambil teks yang diketik dan hilangkan spasi berlebih
            String inputNama = etUsername.getText().toString().trim();

            // Cek biar user nggak bisa login kalau namanya kosong
            if (inputNama.isEmpty()) {
                Toast.makeText(MainActivity.this, "Masukan Username dan Password", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Simpan nama ke memori HP (SharedPreferences)
            SharedPreferences prefs = getSharedPreferences("SesiLogin", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("USERNAME", inputNama);
            editor.apply();

            etUsername.clearFocus();
            etPassword.clearFocus();
            hideKeyboard();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // 2. Pindah ke Home
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                startActivity(intent);

                // Tutup halaman login ini biar user nggak bisa balik ke sini pakai tombol "Back" HP
                finish();
            }, 120);
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = getWindow().getDecorView();
        }

        InputMethodManager inputMethodManager =
                (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (inputMethodManager != null) {
            inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
