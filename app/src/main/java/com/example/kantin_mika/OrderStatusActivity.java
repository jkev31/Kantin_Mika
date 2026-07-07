package com.example.kantin_mika;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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
        loadOrderItems();
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
                        cursor.getInt(6)  // qty
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
        String namaMenu, namaTenant;
        int harga, qty;
        OrderItem(int idTenant, String namaMenu, String namaTenant, int harga, int qty) {
            this.idTenant = idTenant;
            this.namaMenu = namaMenu;
            this.namaTenant = namaTenant;
            this.harga = harga;
            this.qty = qty;
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
            TextView tvTotal, tvOrderId, tvTableInfo;
            View btnAction;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                llItemsContainer = itemView.findViewById(R.id.llOrderItemsContainer);
                tvTotal = itemView.findViewById(R.id.tvOrderTotal);
                tvOrderId = itemView.findViewById(R.id.tvOrderId);
                tvTableInfo = itemView.findViewById(R.id.tvTableInfo);
                btnAction = itemView.findViewById(R.id.btnOrderAction);
            }
        }
    }
}
