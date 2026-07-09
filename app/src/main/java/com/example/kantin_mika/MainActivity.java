package com.example.kantin_mika;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnCustomer).getParent().getParent() instanceof View ? (View) findViewById(R.id.btnCustomer).getParent().getParent() : findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.btnCustomer).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ScanActivity.class));
        });

        findViewById(R.id.btnTenant).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, TenantLoginActivity.class));
        });

        findViewById(R.id.btnAbout).setOnClickListener(v -> {
            showAboutDialog();
        });
    }

    private void showAboutDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_about_us, null);
        bottomSheetDialog.setContentView(dialogView);

        dialogView.findViewById(R.id.btnCloseDialog).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }
}
