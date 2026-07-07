package com.example.kantin_mika;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ScanActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnScanQR).setOnClickListener(v -> {
            startActivity(new Intent(ScanActivity.this, ScanCameraActivity.class));
        });

        findViewById(R.id.btnManual).setOnClickListener(v -> {
            startActivity(new Intent(ScanActivity.this, ScanManualActivity.class));
        });
    }
}
