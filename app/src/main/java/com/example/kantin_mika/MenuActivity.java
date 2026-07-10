package com.example.kantin_mika;

import android.content.Intent;
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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import android.graphics.Paint;

public class MenuActivity extends AppCompatActivity {
    private RecyclerView rvMenuItems;
    private TextView tvTenantName, tvCartCount, tvCartTotal, tvCartBadgeTop;
    private View bottomCartContainer;
    private List<Menu> listMenus = new ArrayList<>();
    private CartDBHelper dbHelper;
    private int idTenant;
    private String namaTenant, usernameTenant;
    private String URL_GET_MENUS = "http://172.16.37.134/pmob/api_uas/get_menus.php?id_tenant=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        idTenant = getIntent().getIntExtra("id_tenant", 0);
        namaTenant = getIntent().getStringExtra("nama_tenant");
        usernameTenant = getIntent().getStringExtra("username_tenant");

        dbHelper = new CartDBHelper(this);
        rvMenuItems = findViewById(R.id.rvMenuItems);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        tvTenantName = findViewById(R.id.tvTenantName);
        tvTenantName.setText(namaTenant);

        tvCartCount = findViewById(R.id.tvCartCount);
        tvCartTotal = findViewById(R.id.tvCartTotal);
        tvCartBadgeTop = findViewById(R.id.tvCartBadgeTop);
        bottomCartContainer = findViewById(R.id.bottomCartContainer);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnCartBottom).setOnClickListener(v -> {
            startActivity(new Intent(MenuActivity.this, CartActivity.class));
        });
        findViewById(R.id.btnCartTop).setOnClickListener(v -> {
            startActivity(new Intent(MenuActivity.this, CartActivity.class));
        });

        loadMenus();
        updateCartUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateCartUI();
        if (rvMenuItems.getAdapter() != null) {
            rvMenuItems.getAdapter().notifyDataSetChanged();
        }
    }

    private void loadMenus() {
        Thread thread = new Thread(() -> {
            try {
                URL url = new URL(URL_GET_MENUS + idTenant);
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
            JSONArray arr;
            String trimmedData = data.trim();
            if (trimmedData.startsWith("{")) {
                JSONObject root = new JSONObject(trimmedData);
                arr = root.optJSONArray("data");
            } else {
                arr = new JSONArray(trimmedData);
            }

            if (arr == null) arr = new JSONArray();

            listMenus.clear();
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String status = obj.optString("status_stok", obj.optString("status", "tersedia")).trim();
                
                // Normalisasi status
                if (status.equals("1")) status = "tersedia";
                else if (status.equals("0")) status = "habis";

                listMenus.add(new Menu(
                        obj.getInt("id_menu"),
                        obj.getInt("id_tenant"),
                        obj.getString("nama_menu"),
                        obj.getInt("harga"),
                        status,
                        obj.optString("deskripsi", "")
                ));
            }
            runOnUiThread(() -> {
                MenuAdapter adapter = new MenuAdapter(listMenus, dbHelper, namaTenant, usernameTenant, this::updateCartUI);
                rvMenuItems.setAdapter(adapter);
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateCartUI() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(qty), SUM(qty * harga) FROM cart", null);
        if (cursor != null && cursor.moveToFirst()) {
            int count = cursor.getInt(0);
            int total = cursor.getInt(1);
            if (count > 0) {
                bottomCartContainer.setVisibility(View.VISIBLE);
                tvCartCount.setText(String.valueOf(count));
                tvCartBadgeTop.setVisibility(View.VISIBLE);
                tvCartBadgeTop.setText(String.valueOf(count));
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                tvCartTotal.setText(formatter.format(total));
            } else {
                bottomCartContainer.setVisibility(View.GONE);
                tvCartBadgeTop.setVisibility(View.GONE);
            }
            cursor.close();
        }
    }

    private static class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder> {
        private final List<Menu> menus;
        private final CartDBHelper dbHelper;
        private final String namaTenant, usernameTenant;
        private final Runnable onUpdate;

        public MenuAdapter(List<Menu> menus, CartDBHelper dbHelper, String namaTenant, String usernameTenant, Runnable onUpdate) {
            this.menus = menus;
            this.dbHelper = dbHelper;
            this.namaTenant = namaTenant;
            this.usernameTenant = usernameTenant;
            this.onUpdate = onUpdate;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_menu, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Menu menu = menus.get(position);
            holder.tvFoodName.setText(menu.getNama());
            holder.tvFoodDesc.setText(menu.getDeskripsi());
            NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            holder.tvFoodPrice.setText(formatter.format(menu.getHarga()));
            
            boolean isHabis = "habis".equalsIgnoreCase(menu.getStatusStok());
            
            if (isHabis) {
                holder.rlOutOfStockOverlay.setVisibility(View.VISIBLE);
                holder.containerQuantity.setVisibility(View.GONE);
                
                // Efek Habis: Abu-abu + Strikethrough
                int colorGray = 0xFF9CA3AF;
                holder.tvFoodName.setTextColor(colorGray);
                holder.tvFoodName.setPaintFlags(holder.tvFoodName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                
                holder.tvFoodPrice.setTextColor(colorGray);
                holder.tvFoodPrice.setPaintFlags(holder.tvFoodPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                
                holder.tvFoodDesc.setTextColor(colorGray);
                holder.tvFoodDesc.setPaintFlags(holder.tvFoodDesc.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                
                holder.itemView.setAlpha(0.6f);
                holder.itemView.setOnClickListener(null);
            } else {
                holder.rlOutOfStockOverlay.setVisibility(View.GONE);
                holder.containerQuantity.setVisibility(View.VISIBLE);
                
                // Efek Normal: Hitam/Oranye + Tanpa Strikethrough
                holder.tvFoodName.setTextColor(0xFF111827);
                holder.tvFoodName.setPaintFlags(holder.tvFoodName.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                
                holder.tvFoodPrice.setTextColor(0xFFF97316); // @color/primary
                holder.tvFoodPrice.setPaintFlags(holder.tvFoodPrice.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                
                holder.tvFoodDesc.setTextColor(0xFF9CA3AF); // default gray secondary
                holder.tvFoodDesc.setPaintFlags(holder.tvFoodDesc.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                
                holder.itemView.setAlpha(1.0f);
                updateQuantityUI(holder, menu);
            }
        }

        private void updateQuantityUI(ViewHolder holder, Menu menu) {
            int qty = getQuantityFromDB(menu.getId());
            if (qty > 0) {
                holder.btnAdd.setVisibility(View.GONE);
                holder.llQuantitySelector.setVisibility(View.VISIBLE);
                holder.tvQuantity.setText(String.valueOf(qty));
            } else {
                holder.btnAdd.setVisibility(View.VISIBLE);
                holder.llQuantitySelector.setVisibility(View.GONE);
            }

            holder.btnAdd.setOnClickListener(v -> {
                dbHelper.addToCart(menu.getId(), menu.getIdTenant(), menu.getNama(), namaTenant, usernameTenant, menu.getHarga(), 1);
                updateQuantityUI(holder, menu);
                onUpdate.run();
            });

            holder.btnPlus.setOnClickListener(v -> {
                dbHelper.addToCart(menu.getId(), menu.getIdTenant(), menu.getNama(), namaTenant, usernameTenant, menu.getHarga(), 1);
                updateQuantityUI(holder, menu);
                onUpdate.run();
            });

            holder.btnMinus.setOnClickListener(v -> {
                int currentQty = getQuantityFromDB(menu.getId());
                dbHelper.updateQty(menu.getId(), currentQty - 1);
                updateQuantityUI(holder, menu);
                onUpdate.run();
            });
        }

        private int getQuantityFromDB(int idMenu) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.query("cart", new String[]{"qty"}, "id_menu=?", new String[]{String.valueOf(idMenu)}, null, null, null);
            int qty = 0;
            if (cursor != null && cursor.moveToFirst()) {
                qty = cursor.getInt(0);
                cursor.close();
            }
            return qty;
        }

        @Override
        public int getItemCount() {
            return menus.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvFoodName, tvFoodPrice, tvFoodDesc, tvQuantity;
            View rlOutOfStockOverlay, btnAdd, llQuantitySelector, btnMinus, btnPlus, containerQuantity;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvFoodName = itemView.findViewById(R.id.tvFoodName);
                tvFoodPrice = itemView.findViewById(R.id.tvFoodPrice);
                tvFoodDesc = itemView.findViewById(R.id.tvFoodDesc);
                tvQuantity = itemView.findViewById(R.id.tvQuantity);
                rlOutOfStockOverlay = itemView.findViewById(R.id.rlOutOfStockOverlay);
                btnAdd = itemView.findViewById(R.id.btnAdd);
                llQuantitySelector = itemView.findViewById(R.id.llQuantitySelector);
                btnMinus = itemView.findViewById(R.id.btnMinus);
                btnPlus = itemView.findViewById(R.id.btnPlus);
                containerQuantity = itemView.findViewById(R.id.containerQuantity);
            }
        }
    }
}
