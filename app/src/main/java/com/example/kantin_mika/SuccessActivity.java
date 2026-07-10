package com.example.kantin_mika;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Locale;

public class SuccessActivity extends AppCompatActivity {
    private CartDBHelper dbHelper;
    private String URL_CREATE_ORDER = "http://192.168.101.7/pmob/api_uas/create_order.php";
    private TextView tvOrderId, tvTableNum, tvTotal;
    private LinearLayout llItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        dbHelper = new CartDBHelper(this);
        tvOrderId = findViewById(R.id.tvSuccessOrderId);
        tvTableNum = findViewById(R.id.tvSuccessTableNum);
        tvTotal = findViewById(R.id.tvSuccessTotal);
        llItems = findViewById(R.id.llSuccessItems);

        findViewById(R.id.btnOrderAgain).setOnClickListener(v -> {
            Intent intent = new Intent(SuccessActivity.this, HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        findViewById(R.id.btnOrderStatus).setOnClickListener(v -> {
            startActivity(new Intent(SuccessActivity.this, OrderStatusActivity.class));
        });

        loadDataAndInsertOrder();
    }

    private void loadDataAndInsertOrder() {
        Thread thread = new Thread(() -> {
            try {
                SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
                String rawTableNum = pref.getString("nomor_meja", "0");
                String normalizedTableNum;
                try {
                    normalizedTableNum = String.valueOf(Integer.parseInt(rawTableNum.replaceAll("[^0-9]", "")));
                } catch (Exception e) {
                    normalizedTableNum = rawTableNum;
                }
                final String tableNum = normalizedTableNum;
                String orderId = "ORD-" + System.currentTimeMillis();

                SQLiteDatabase db = dbHelper.getReadableDatabase();
                Cursor cursor = db.query("cart", null, null, null, null, null, null);

                JSONArray itemsArr = new JSONArray();
                int totalPay = 0;
                NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
                
                dbHelper.clearOrders();

                runOnUiThread(() -> {
                    tvOrderId.setText("#" + orderId);
                    tvTableNum.setText("Tunggu di Meja " + tableNum);
                    llItems.removeAllViews();
                });

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        int idMenu = cursor.getInt(0);
                        int idTenant = cursor.getInt(1);
                        String name = cursor.getString(2);
                        String tenantName = cursor.getString(3);
                        int harga = cursor.getInt(5);
                        int qty = cursor.getInt(6);
                        
                        dbHelper.saveOrder(orderId, idTenant, name, tenantName, harga, qty, "Menunggu");

                        JSONObject item = new JSONObject();
                        item.put("id_menu", idMenu);
                        item.put("id_tenant", idTenant);
                        item.put("qty", qty);
                        item.put("harga_satuan", harga);
                        itemsArr.put(item);
                        totalPay += (harga * qty);

                        final int subtotal = harga * qty;
                        runOnUiThread(() -> {
                            View itemView = LayoutInflater.from(this).inflate(R.layout.item_order_summary, llItems, false);
                            TextView t1 = itemView.findViewById(R.id.tvSummaryItemName);
                            TextView t2 = itemView.findViewById(R.id.tvSummaryItemPrice);
                            if (t1 != null) t1.setText(name + " ×" + qty);
                            if (t2 != null) t2.setText(formatter.format(subtotal));
                            llItems.addView(itemView);
                        });
                    }
                    cursor.close();
                }

                if (itemsArr.length() == 0) return;

                final int finalTotal = totalPay;
                runOnUiThread(() -> tvTotal.setText(formatter.format(finalTotal)));

                JSONObject payload = new JSONObject();
                payload.put("id_order", orderId);
                payload.put("nomor_meja", tableNum);
                payload.put("total_bayar", totalPay);
                payload.put("items", itemsArr);

                URL url = new URL(URL_CREATE_ORDER);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                OutputStream os = conn.getOutputStream();
                os.write(payload.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject resObj = new JSONObject(response.toString());
                String status = resObj.getString("status");
                
                runOnUiThread(() -> {
                    if ("success".equals(status)) {
                        dbHelper.clearCart();
                        
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putString("active_order_id", orderId);
                        editor.putString("active_order_table", tableNum);
                        editor.apply();

                        Toast.makeText(SuccessActivity.this, "Pesanan Berhasil!", Toast.LENGTH_SHORT).show();
                    } else {
                        try {
                            Toast.makeText(SuccessActivity.this, resObj.getString("message"), Toast.LENGTH_LONG).show();
                        } catch (Exception e) {}
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(SuccessActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        });
        thread.start();
    }
}
