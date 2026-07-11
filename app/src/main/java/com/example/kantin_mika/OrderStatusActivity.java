package com.example.kantin_mika;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;
import android.widget.LinearLayout;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderStatusActivity extends AppCompatActivity {
    private TextView tvOrderSubtitle, tvOrderSubtitleInfo;
    private RecyclerView rvOrderCards;
    private CartDBHelper dbHelper;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable refreshRunnable;
    private String URL_GET_STATUS = "http://192.168.1.5/pmob/api_uas/get_order_status.php?id_order=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_status);

        dbHelper = new CartDBHelper(this);
        tvOrderSubtitle = findViewById(R.id.tvOrderSubtitle);
        tvOrderSubtitleInfo = findViewById(R.id.tvOrderSubtitleInfo);
        rvOrderCards = findViewById(R.id.rvOrderCards);
        rvOrderCards.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnOrderAgain).setOnClickListener(v -> {
            Intent intent = new Intent(OrderStatusActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        loadOrderInfo();
        
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchStatusFromServer();
                handler.postDelayed(this, 5000); // Refresh every 5 seconds
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(refreshRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refreshRunnable);
    }

    private void fetchStatusFromServer() {
        SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
        String activeOrderId = pref.getString("active_order_id", null);
        if (activeOrderId == null) {
            loadOrderItems(); // Fallback to local
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(URL_GET_STATUS + activeOrderId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject res = new JSONObject(sb.toString());
                if ("success".equals(res.optString("status"))) {
                    JSONArray statuses = res.optJSONArray("statuses");
                    if (statuses != null) {
                        SQLiteDatabase db = dbHelper.getWritableDatabase();
                        boolean allFinished = true;
                        
                        for (int i = 0; i < statuses.length(); i++) {
                            JSONObject item = statuses.getJSONObject(i);
                            int tenantId = item.getInt("id_tenant");
                            String statusTenant = item.getString("status");

                            // Update status per tenant di SQLite
                            android.content.ContentValues values = new android.content.ContentValues();
                            values.put("status", statusTenant);
                            db.update("orders", values, "order_id=? AND id_tenant=?", 
                                    new String[]{activeOrderId, String.valueOf(tenantId)});
                                    
                            if (!"Selesai".equalsIgnoreCase(statusTenant)) {
                                allFinished = false;
                            }
                        }

                        // Jika SEMUA tenant sudah selesai, baru hapus session order aktif
                        if (allFinished && statuses.length() > 0) {
                            pref.edit().remove("active_order_id").remove("active_order_table").apply();
                        }
                    }
                    
                    runOnUiThread(this::loadOrderItems);
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::loadOrderItems);
            }
        }).start();
    }

    public void updateStatusToServer(String orderId, int idTenant, String newStatus) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id_order", orderId);
                payload.put("status_pesanan", newStatus);
                payload.put("id_tenant", idTenant);

                // Reusing URL_GET_STATUS but changing file name or using a separate URL
                // Let's check what URL_UPDATE_STATUS is in TenantOrdersFragment
                URL url = new URL("http://192.168.1.5/pmob/api_uas/update_order_status.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                java.io.OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject resObj = new JSONObject(sb.toString());
                if ("success".equals(resObj.optString("status"))) {
                    fetchStatusFromServer();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadOrderInfo() {
        SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
        String activeOrderId = pref.getString("active_order_id", "KNT-000000");
        String activeOrderTable = pref.getString("active_order_table", "-");

        tvOrderSubtitle.setText(activeOrderId + " · Meja " + activeOrderTable);
        tvOrderSubtitleInfo.setText("Status diperbarui secara otomatis. Silakan tunggu di Meja " + activeOrderTable + ".");
    }

    private void loadOrderItems() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("orders", null, null, null, null, null, null);
        
        Map<Integer, List<OrderItem>> grouped = new HashMap<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                OrderItem item = new OrderItem(
                        cursor.getInt(2), // idTenant
                        cursor.getString(3), // namaMenu
                        cursor.getString(4), // namaTenant
                        cursor.getInt(5), // harga
                        cursor.getInt(6), // qty
                        cursor.getString(7)  // status
                );
                if (!grouped.containsKey(item.idTenant)) {
                    grouped.put(item.idTenant, new ArrayList<>());
                }
                grouped.get(item.idTenant).add(item);
            }
            cursor.close();
        }

        List<Integer> tenantIds = new ArrayList<>(grouped.keySet());
        OrderGroupAdapter adapter = new OrderGroupAdapter(tenantIds, grouped);
        rvOrderCards.setAdapter(adapter);
    }

    private static class OrderItem {
        int idTenant;
        String namaMenu, namaTenant, status;
        int harga, qty;
        OrderItem(int idTenant, String namaMenu, String namaTenant, int harga, int qty, String status) {
            this.idTenant = idTenant;
            this.namaMenu = namaMenu;
            this.namaTenant = namaTenant;
            this.harga = harga;
            this.qty = qty;
            this.status = status;
        }
    }

    private static class OrderGroupAdapter extends RecyclerView.Adapter<OrderGroupAdapter.ViewHolder> {
        private final List<Integer> tenantIds;
        private final Map<Integer, List<OrderItem>> groupedItems;

        public OrderGroupAdapter(List<Integer> tenantIds, Map<Integer, List<OrderItem>> groupedItems) {
            this.tenantIds = tenantIds;
            this.groupedItems = groupedItems;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_tenant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int tenantId = tenantIds.get(position);
            List<OrderItem> items = groupedItems.get(tenantId);
            
            SharedPreferences pref = holder.itemView.getContext().getSharedPreferences("KantinPref", MODE_PRIVATE);
            String activeOrderId = pref.getString("active_order_id", "KNT-000000");
            String activeOrderTable = pref.getString("active_order_table", "-");
            
            holder.tvOrderId.setText(activeOrderId);
            holder.tvTableInfo.setText("Meja " + activeOrderTable);
            holder.btnAction.setVisibility(View.GONE); // User shouldn't see "Mulai Proses"
            
            if (!items.isEmpty()) {
                String status = items.get(0).status;
                holder.tvStatusBadge.setText(status.toUpperCase());
                
                // Normalisasi status sesuai database (Menunggu, Diproses, Selesai)
                if ("menunggu".equalsIgnoreCase(status) || "baru".equalsIgnoreCase(status)) {
                    holder.tvStatusBadge.setText("MENUNGGU");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_outline_orange);
                    holder.tvStatusBadge.setTextColor(0xFFEA580C);
                } else if ("diproses".equalsIgnoreCase(status) || "proses".equalsIgnoreCase(status)) {
                    holder.tvStatusBadge.setText("DIPROSES");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_blue);
                    holder.tvStatusBadge.setTextColor(0xFF2563EB);
                } else if ("diantar".equalsIgnoreCase(status)) {
                    holder.tvStatusBadge.setText("DIANTAR");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_primary);
                    holder.tvStatusBadge.setTextColor(0xFFFFFFFF);

                    holder.btnAction.setVisibility(View.VISIBLE);
                    holder.btnAction.setText("Tandai Selesai");
                    holder.btnAction.setBackgroundResource(R.drawable.bg_rounded_green);
                    holder.btnAction.setOnClickListener(v -> {
                        if (holder.itemView.getContext() instanceof OrderStatusActivity) {
                            ((OrderStatusActivity) holder.itemView.getContext()).updateStatusToServer(activeOrderId, tenantId, "Selesai");
                        }
                    });
                } else if ("selesai".equalsIgnoreCase(status)) {
                    holder.tvStatusBadge.setText("SELESAI");
                    holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_green);
                    holder.tvStatusBadge.setTextColor(0xFF059669);
                    holder.btnAction.setVisibility(View.GONE);
                }
            }

            holder.llItemsContainer.removeAllViews();
            int total = 0;
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            for (OrderItem item : items) {
                total += (item.harga * item.qty);
                View itemView = LayoutInflater.from(holder.itemView.getContext()).inflate(R.layout.item_order_summary, holder.llItemsContainer, false);
                TextView t1 = itemView.findViewById(R.id.tvSummaryItemName);
                TextView t2 = itemView.findViewById(R.id.tvSummaryItemPrice);
                t1.setText(item.namaMenu + " ×" + item.qty);
                t2.setText(formatter.format(item.harga * item.qty));
                holder.llItemsContainer.addView(itemView);
            }
            holder.tvTotal.setText(formatter.format(total));
        }

        @Override
        public int getItemCount() { return tenantIds.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            LinearLayout llItemsContainer;
            TextView tvTotal, tvOrderId, tvTableInfo, tvStatusBadge, btnAction;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                llItemsContainer = itemView.findViewById(R.id.llOrderItemsContainer);
                tvTotal = itemView.findViewById(R.id.tvOrderTotal);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvTableInfo = itemView.findViewById(R.id.tvTableInfo);
                tvStatusBadge = itemView.findViewById(R.id.tvOrderStatusBadge);
                btnAction = itemView.findViewById(R.id.btnOrderAction);
            }
        }
    }
}
