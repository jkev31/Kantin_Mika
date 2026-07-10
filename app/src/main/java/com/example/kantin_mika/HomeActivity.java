package com.example.kantin_mika;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {
    private RecyclerView rvTenants;
    private TextView tvTableInfo, tvCartBadgeTop, tvCartCount, tvCartTotal;
    private View bottomCartContainer;
    private CartDBHelper dbHelper;
    private List<Tenant> listTenants = new ArrayList<>();
    private String URL_GET_TENANTS = "http://172.16.37.134/pmob/api_uas/get_tenants.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        rvTenants = findViewById(R.id.rvTenants);
        rvTenants.setLayoutManager(new LinearLayoutManager(this));
        tvTableInfo = findViewById(R.id.tvTableInfo);
        tvCartBadgeTop = findViewById(R.id.tvCartBadgeTop);
        tvCartCount = findViewById(R.id.tvCartCount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        bottomCartContainer = findViewById(R.id.bottomCartContainer);
        dbHelper = new CartDBHelper(this);

        SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
        String tableNum = pref.getString("nomor_meja", "-");
        tvTableInfo.setText("Meja " + tableNum);

        findViewById(R.id.btnChangeTable).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnCart).setOnClickListener(v -> {
             startActivity(new Intent(HomeActivity.this, CartActivity.class));
        });
        findViewById(R.id.btnCartTop).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CartActivity.class));
        });

        loadTenants();
        checkActiveOrder();
        updateCartUI();
    }

    private void updateCartUI() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(qty), SUM(qty * harga) FROM cart", null);
        
        int totalItems = 0;
        int totalPrice = 0;
        
        if (cursor != null && cursor.moveToFirst()) {
            totalItems = cursor.getInt(0);
            totalPrice = cursor.getInt(1);
            cursor.close();
        }

        if (totalItems > 0) {
            tvCartBadgeTop.setVisibility(View.VISIBLE);
            tvCartBadgeTop.setText(String.valueOf(totalItems));
            
            bottomCartContainer.setVisibility(View.VISIBLE);
            tvCartCount.setText(totalItems + " item");
            
            java.text.NumberFormat formatter = java.text.NumberFormat.getCurrencyInstance(new java.util.Locale("id", "ID"));
            tvCartTotal.setText(formatter.format(totalPrice));
        } else {
            tvCartBadgeTop.setVisibility(View.GONE);
            bottomCartContainer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartUI();
    }

    private void checkActiveOrder() {
        SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
        String activeOrderId = pref.getString("active_order_id", null);
        String activeOrderTable = pref.getString("active_order_table", null);
        String currentTable = pref.getString("nomor_meja", "");

        View btnActiveOrder = findViewById(R.id.btnActiveOrder);
        if (activeOrderId != null && activeOrderTable != null && activeOrderTable.equals(currentTable)) {
            // Check real status from server
            new Thread(() -> {
                try {
                    URL url = new URL("http://172.16.37.134/pmob/api_uas/get_order_status.php?id_order=" + activeOrderId);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    InputStream is = conn.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    
                    JSONObject res = new JSONObject(sb.toString());
                    String status = res.optString("status_pesanan", "");
                    
                    runOnUiThread(() -> {
                        if ("Selesai".equalsIgnoreCase(status)) {
                            // Order is done, hide and clear
                            pref.edit().remove("active_order_id").remove("active_order_table").apply();
                            btnActiveOrder.setVisibility(View.GONE);
                        } else {
                            btnActiveOrder.setVisibility(View.VISIBLE);
                            TextView tvActiveOrderInfo = findViewById(R.id.tvActiveOrderInfo);
                            tvActiveOrderInfo.setText(activeOrderId + " · Meja " + activeOrderTable);
                            btnActiveOrder.setOnClickListener(v -> {
                                startActivity(new Intent(HomeActivity.this, OrderStatusActivity.class));
                            });
                        }
                    });
                } catch (Exception e) {
                    // Fallback to showing it if network fails, or just keep current state
                    runOnUiThread(() -> btnActiveOrder.setVisibility(View.VISIBLE));
                }
            }).start();
        } else {
            btnActiveOrder.setVisibility(View.GONE);
        }
    }

    private void loadTenants() {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(URL_GET_TENANTS);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                parseJSON(response.toString());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    private void parseJSON(String data) {
        try {
            JSONArray jsonArray = new JSONArray(data);
            listTenants.clear();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                listTenants.add(new Tenant(
                        obj.getInt("id_tenant"),
                        obj.getString("nama_tenant"),
                        obj.getString("username"),
                        obj.getString("password")
                ));
            }
            runOnUiThread(() -> {
                TenantAdapter adapter = new TenantAdapter(listTenants, tenant -> {
                    Intent intent = new Intent(HomeActivity.this, MenuActivity.class);
                    intent.putExtra("id_tenant", tenant.getId());
                    intent.putExtra("nama_tenant", tenant.getNama());
                    intent.putExtra("username_tenant", tenant.getUsername());
                    startActivity(intent);
                });
                rvTenants.setAdapter(adapter);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class TenantAdapter extends RecyclerView.Adapter<TenantAdapter.ViewHolder> {
        private final List<Tenant> tenants;
        private final OnTenantClickListener listener;

        public TenantAdapter(List<Tenant> tenants, OnTenantClickListener listener) {
            this.tenants = tenants;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tenant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Tenant tenant = tenants.get(position);
            holder.tvTenantName.setText(tenant.getNama());
            holder.itemView.setOnClickListener(v -> listener.onTenantClick(tenant));
        }

        @Override
        public int getItemCount() {
            return tenants.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTenantName;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTenantName = itemView.findViewById(R.id.tvTenantName);
            }
        }

        interface OnTenantClickListener {
            void onTenantClick(Tenant tenant);
        }
    }
}
