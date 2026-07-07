package com.example.kantin_mika;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class TenantHomeActivity extends AppCompatActivity {
    private TextView tabPesananMasuk, tabKelolaStok;
    private TextView tvStoreName, tvOwnerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_home);

        tabPesananMasuk = findViewById(R.id.tabPesananMasuk);
        tabKelolaStok = findViewById(R.id.tabKelolaStok);
        tvStoreName = findViewById(R.id.tvStoreName);
        tvOwnerName = findViewById(R.id.tvOwnerName);

        // Load logged in info
        SharedPreferences pref = getSharedPreferences("TenantPref", MODE_PRIVATE);
        tvStoreName.setText(pref.getString("nama_tenant", "Toko Saya"));
        tvOwnerName.setText("a.n. " + pref.getString("username", "Owner"));

        tabPesananMasuk.setOnClickListener(v -> switchTab(true));
        tabKelolaStok.setOnClickListener(v -> switchTab(false));

        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            pref.edit().clear().apply();
            finish();
        });

        // Default tab
        switchTab(true);
    }

    private void switchTab(boolean isOrders) {
        Fragment fragment;
        if (isOrders) {
            fragment = new TenantOrdersFragment();
            tabPesananMasuk.setBackgroundResource(R.drawable.bg_rounded_primary);
            tabPesananMasuk.setTextColor(getResources().getColor(android.R.color.white));
            tabKelolaStok.setBackgroundResource(R.drawable.bg_rounded_muted);
            tabKelolaStok.setTextColor(getResources().getColor(R.color.text_secondary));
        } else {
            fragment = new TenantStockFragment();
            tabKelolaStok.setBackgroundResource(R.drawable.bg_rounded_primary);
            tabKelolaStok.setTextColor(getResources().getColor(android.R.color.white));
            tabPesananMasuk.setBackgroundResource(R.drawable.bg_rounded_muted);
            tabPesananMasuk.setTextColor(getResources().getColor(R.color.text_secondary));
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.flTenantContainer, fragment)
                .commit();
    }
}
