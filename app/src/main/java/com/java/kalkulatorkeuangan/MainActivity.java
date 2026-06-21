package com.java.kalkulatorkeuangan;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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

        btnLogin.setOnClickListener(v -> {

            // Ambil teks yang diketik dan hilangkan spasi berlebih
            String inputNama = etUsername.getText().toString().trim();

            // Cek biar user nggak bisa login kalau namanya kosong
            if (inputNama.isEmpty()) {
                Toast.makeText(MainActivity.this, "Isi username dulu bro!", Toast.LENGTH_SHORT).show();
                return;
            }

            // 1. Simpan nama ke memori HP (SharedPreferences)
            SharedPreferences prefs = getSharedPreferences("SesiLogin", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("USERNAME", inputNama);
            editor.apply();

            // 2. Pindah ke Home
            Intent intent = new Intent(MainActivity.this, HomeActivity.class);
            startActivity(intent);

            // Tutup halaman login ini biar user nggak bisa balik ke sini pakai tombol "Back" HP
            finish();
        });
    }
}