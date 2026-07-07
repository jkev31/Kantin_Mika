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

public class CartActivity extends AppCompatActivity {
    private RecyclerView rvCartTenants;
    private TextView tvTotalItems, tvSummaryTotal, tvCheckoutTotal, tvDeliveryInfo;
    private LinearLayout llSummaryItems;
    private View llEmptyState, llCartItemsContainer, bottomCheckoutContainer;
    private CartDBHelper dbHelper;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        dbHelper = new CartDBHelper(this);
        rvCartTenants = findViewById(R.id.rvCartTenants);
        rvCartTenants.setLayoutManager(new LinearLayoutManager(this));

        tvTotalItems = findViewById(R.id.tvTotalItems);
        tvSummaryTotal = findViewById(R.id.tvSummaryTotal);
        tvCheckoutTotal = findViewById(R.id.tvCheckoutTotal);
        tvDeliveryInfo = findViewById(R.id.tvDeliveryInfo);
        llSummaryItems = findViewById(R.id.llSummaryItems);
        llEmptyState = findViewById(R.id.llEmptyState);
        llCartItemsContainer = findViewById(R.id.llCartItemsContainer);
        bottomCheckoutContainer = findViewById(R.id.bottomCheckoutContainer);

        SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
        tvDeliveryInfo.setText("Diantar ke Meja " + pref.getString("nomor_meja", "-"));

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddMore).setOnClickListener(v -> finish());
        findViewById(R.id.btnEmptyStateMenu).setOnClickListener(v -> {
            startActivity(new Intent(CartActivity.this, HomeActivity.class));
            finish();
        });

        findViewById(R.id.btnCheckout).setOnClickListener(v -> {
            startActivity(new Intent(CartActivity.this, PaymentActivity.class));
        });

        loadCart();
    }

    private void loadCart() {
        cartItems.clear();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("cart", null, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                cartItems.add(new CartItem(
                        cursor.getInt(0),
                        cursor.getInt(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getInt(5),
                        cursor.getInt(6)
                ));
            }
            cursor.close();
        }
        updateUI();
    }

    private void updateUI() {
        if (cartItems.isEmpty()) {
            llEmptyState.setVisibility(View.VISIBLE);
            llCartItemsContainer.setVisibility(View.GONE);
            bottomCheckoutContainer.setVisibility(View.GONE);
            tvTotalItems.setText("0 item");
        } else {
            llEmptyState.setVisibility(View.GONE);
            llCartItemsContainer.setVisibility(View.VISIBLE);
            bottomCheckoutContainer.setVisibility(View.VISIBLE);

            int totalQty = 0;
            int totalPrice = 0;
            Map<Integer, List<CartItem>> grouped = new HashMap<>();
            llSummaryItems.removeAllViews();
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

            for (CartItem item : cartItems) {
                totalQty += item.getQty();
                totalPrice += (item.getHarga() * item.getQty());
                if (!grouped.containsKey(item.getIdTenant())) {
                    grouped.put(item.getIdTenant(), new ArrayList<>());
                }
                grouped.get(item.getIdTenant()).add(item);

                // Add to Summary
                View summaryView = LayoutInflater.from(this).inflate(R.layout.item_order_summary, llSummaryItems, false);
                TextView tvName = summaryView.findViewById(R.id.tvSummaryItemName);
                TextView tvPrice = summaryView.findViewById(R.id.tvSummaryItemPrice);
                tvName.setText(item.getNamaMenu() + " ×" + item.getQty());
                tvPrice.setText(formatter.format(item.getHarga() * item.getQty()));
                llSummaryItems.addView(summaryView);
            }

            tvTotalItems.setText(totalQty + " item");
            tvSummaryTotal.setText(formatter.format(totalPrice));
            tvCheckoutTotal.setText(formatter.format(totalPrice));

            List<Integer> tenantIds = new ArrayList<>(grouped.keySet());
            TenantGroupAdapter adapter = new TenantGroupAdapter(tenantIds, grouped, new TenantGroupAdapter.OnCartUpdateListener() {
                @Override
                public void onUpdate(int idMenu, int newQty) {
                    dbHelper.updateQty(idMenu, newQty);
                    loadCart();
                }
            });
            rvCartTenants.setAdapter(adapter);
        }
    }

    private static class TenantGroupAdapter extends RecyclerView.Adapter<TenantGroupAdapter.ViewHolder> {
        private final List<Integer> tenantIds;
        private final Map<Integer, List<CartItem>> groupedItems;
        private final OnCartUpdateListener listener;

        public TenantGroupAdapter(List<Integer> tenantIds, Map<Integer, List<CartItem>> groupedItems, OnCartUpdateListener listener) {
            this.tenantIds = tenantIds;
            this.groupedItems = groupedItems;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart_tenant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int tenantId = tenantIds.get(position);
            List<CartItem> items = groupedItems.get(tenantId);
            
            if (!items.isEmpty()) {
                holder.tvTenantName.setText(items.get(0).getNamaTenant());
            } else {
                holder.tvTenantName.setText("Tenant #" + tenantId);
            }

            int subtotal = 0;
            for (CartItem item : items) subtotal += (item.getHarga() * item.getQty());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            holder.tvTenantSubtotal.setText(formatter.format(subtotal));

            holder.rvTenantItems.setLayoutManager(new LinearLayoutManager(holder.itemView.getContext()));
            CartItemAdapter itemAdapter = new CartItemAdapter(items, listener);
            holder.rvTenantItems.setAdapter(itemAdapter);
        }

        @Override
        public int getItemCount() {
            return tenantIds.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTenantName, tvTenantSubtotal;
            RecyclerView rvTenantItems;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTenantName = itemView.findViewById(R.id.tvTenantName);
                tvTenantSubtotal = itemView.findViewById(R.id.tvTenantSubtotal);
                rvTenantItems = itemView.findViewById(R.id.rvTenantItems);
            }
        }

        interface OnCartUpdateListener {
            void onUpdate(int idMenu, int newQty);
        }
    }

    private static class CartItemAdapter extends RecyclerView.Adapter<CartItemAdapter.ViewHolder> {
        private final List<CartItem> items;
        private final TenantGroupAdapter.OnCartUpdateListener listener;

        public CartItemAdapter(List<CartItem> items, TenantGroupAdapter.OnCartUpdateListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cart, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CartItem item = items.get(position);
            holder.tvItemName.setText(item.getNamaMenu());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            holder.tvUnitPrice.setText(formatter.format(item.getHarga()));
            holder.tvQuantity.setText(String.valueOf(item.getQty()));
            holder.tvItemSubtotal.setText(formatter.format(item.getHarga() * item.getQty()));

            holder.btnPlus.setOnClickListener(v -> listener.onUpdate(item.getIdMenu(), item.getQty() + 1));
            holder.btnMinus.setOnClickListener(v -> listener.onUpdate(item.getIdMenu(), item.getQty() - 1));
            holder.btnDelete.setOnClickListener(v -> listener.onUpdate(item.getIdMenu(), 0));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvItemName, tvUnitPrice, tvQuantity, tvItemSubtotal;
            View btnPlus, btnMinus, btnDelete;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvItemName = itemView.findViewById(R.id.tvItemName);
                tvUnitPrice = itemView.findViewById(R.id.tvUnitPrice);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                tvItemSubtotal = itemView.findViewById(R.id.tvItemSubtotal);
                btnPlus = itemView.findViewById(R.id.btnPlus);
                btnMinus = itemView.findViewById(R.id.btnMinus);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }
        }
    }
}
