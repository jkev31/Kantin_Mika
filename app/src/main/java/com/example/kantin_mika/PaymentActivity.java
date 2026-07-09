package com.example.kantin_mika;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentActivity extends AppCompatActivity {
    private CartDBHelper dbHelper;
    private TextView tvPaymentTotal, tvMerchantName, tvTenantNamePay, tvTenantOwnerPay, tvTenantSubtotalPay;
    private LinearLayout llOrderItems, llTenantTabs;
    private Map<Integer, List<CartItem>> tenantOrders = new HashMap<>();
    private List<Integer> tenantIds = new ArrayList<>();
    private int selectedTenantId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        dbHelper = new CartDBHelper(this);
        tvPaymentTotal = findViewById(R.id.tvPaymentTotal);
        tvMerchantName = findViewById(R.id.tvMerchantName);
        tvTenantNamePay = findViewById(R.id.tvTenantNamePay);
        tvTenantOwnerPay = findViewById(R.id.tvTenantOwnerPay);
        tvTenantSubtotalPay = findViewById(R.id.tvTenantSubtotalPay);
        llOrderItems = findViewById(R.id.llOrderItems);
        llTenantTabs = findViewById(R.id.llTenantTabs);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnConfirmPay).setOnClickListener(v -> {
            int currentIndex = tenantIds.indexOf(selectedTenantId);
            if (currentIndex < tenantIds.size() - 1) {
                // Move to next tenant
                selectedTenantId = tenantIds.get(currentIndex + 1);
                createTabs();
                updateContent();
            } else {
                // Last tenant confirmed
                startActivity(new Intent(PaymentActivity.this, SuccessActivity.class));
                finish();
            }
        });

        loadData();
    }

    private void loadData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("cart", null, null, null, null, null, null);
        
        tenantOrders.clear();
        tenantIds.clear();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                CartItem item = new CartItem(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getInt(6)
                );
                
                if (!tenantOrders.containsKey(item.getIdTenant())) {
                    tenantOrders.put(item.getIdTenant(), new ArrayList<>());
                    tenantIds.add(item.getIdTenant());
                }
                tenantOrders.get(item.getIdTenant()).add(item);
            }
            cursor.close();
        }

        if (!tenantIds.isEmpty()) {
            selectedTenantId = tenantIds.get(0);
            createTabs();
            updateContent();
        }
    }

    private void createTabs() {
        llTenantTabs.removeAllViews();
        for (int id : tenantIds) {
            View tabView = LayoutInflater.from(this).inflate(R.layout.item_tenant_tab, llTenantTabs, false);
            TextView tvTab = tabView.findViewById(R.id.tvTabName);
            String name = tenantOrders.get(id).get(0).getNamaTenant();
            tvTab.setText(name);

            if (id == selectedTenantId) {
                tvTab.setBackgroundResource(R.drawable.bg_rounded_primary);
                tvTab.setTextColor(Color.WHITE);
            } else {
                tvTab.setBackgroundResource(R.drawable.bg_rounded_muted);
                tvTab.setTextColor(Color.parseColor("#6B7280"));
            }

            tabView.setOnClickListener(v -> {
                selectedTenantId = id;
                createTabs();
                updateContent();
            });

            llTenantTabs.addView(tabView);
        }
    }

    private void updateContent() {
        List<CartItem> items = tenantOrders.get(selectedTenantId);
        if (items == null || items.isEmpty()) return;

        CartItem first = items.get(0);
        tvTenantNamePay.setText(first.getNamaTenant());
        tvTenantOwnerPay.setText("a.n. " + first.getNamaPemilik());
        tvMerchantName.setText(first.getNamaTenant());

        int subtotal = 0;
        llOrderItems.removeAllViews();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

        for (CartItem item : items) {
            int itemTotal = item.getHarga() * item.getQty();
            subtotal += itemTotal;

            View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_summary, llOrderItems, false);
            TextView t1 = itemView.findViewById(R.id.tvSummaryItemName);
            TextView t2 = itemView.findViewById(R.id.tvSummaryItemPrice);
            t1.setText(item.getNamaMenu() + " ×" + item.getQty());
            t2.setText(formatter.format(itemTotal));
            llOrderItems.addView(itemView);
        }

        tvTenantSubtotalPay.setText(formatter.format(subtotal));
        tvPaymentTotal.setText(formatter.format(subtotal));
    }
}
