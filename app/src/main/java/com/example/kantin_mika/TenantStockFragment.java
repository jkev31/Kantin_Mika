package com.example.kantin_mika;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.json.JSONArray;
import org.json.JSONObject;
import android.widget.EditText;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class TenantStockFragment extends Fragment {
    private RecyclerView rvStock;
    private TextView tvStockSummary;
    private View btnAddMenu;
    private List<Menu> stockItems = new ArrayList<>();
    private String URL_BASE = "http://192.168.1.5/pmob/api_uas/get_menus.php?id_tenant=";
    private String URL_ADD_MENU = "http://192.168.1.5/pmob/api_uas/add_menu.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tenant_stock, container, false);
        rvStock = view.findViewById(R.id.rvTenantStock);
        rvStock.setLayoutManager(new LinearLayoutManager(getContext()));
        tvStockSummary = view.findViewById(R.id.tvStockSummary);

        btnAddMenu = view.findViewById(R.id.btnAddMenu);
        btnAddMenu.setOnClickListener(v -> showAddMenuDialog());

        loadStock();
        return view;
    }

    private void showAddMenuDialog() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_menu, null);
        dialog.setContentView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etMenuName);
        EditText etPrice = dialogView.findViewById(R.id.etMenuPrice);
        View btnSave = dialogView.findViewById(R.id.btnSaveMenu);
        View btnClose = dialogView.findViewById(R.id.btnCloseDialog);

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String price = etPrice.getText().toString().trim();

            if (name.isEmpty() || price.isEmpty()) {
                Toast.makeText(getContext(), "Harap isi nama dan harga", Toast.LENGTH_SHORT).show();
                return;
            }

            saveNewMenu(name, price, dialog);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void saveNewMenu(String name, String price, BottomSheetDialog dialog) {
        int idTenant = getActivity().getSharedPreferences("TenantPref", android.content.Context.MODE_PRIVATE).getInt("id_tenant", 1);
        new Thread(() -> {
            try {
                URL urlAdd = new URL(URL_ADD_MENU);
                HttpURLConnection conn = (HttpURLConnection) urlAdd.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Menambahkan status_stok default 'Tersedia' karena ada di tabel DB
                String data = "id_tenant=" + idTenant +
                        "&nama_menu=" + URLEncoder.encode(name, "UTF-8") +
                        "&harga=" + price +
                        "&status_stok=" + URLEncoder.encode("Tersedia", "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8"));
                os.flush();
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();

                String resStr = response.toString();
                JSONObject jsonRes = new JSONObject(resStr);
                String status = jsonRes.optString("status");
                String message = jsonRes.optString("message");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if ("success".equalsIgnoreCase(status)) {
                            Toast.makeText(getContext(), "Menu berhasil ditambahkan", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            loadStock();
                        } else {
                            // Tampilkan pesan error dari server agar tahu apa yang salah (misal: kolom deskripsi tidak ada)
                            Toast.makeText(getContext(), "Gagal: " + message, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void loadStock() {
        int idTenant = getActivity().getSharedPreferences("TenantPref", android.content.Context.MODE_PRIVATE).getInt("id_tenant", 1);
        new Thread(() -> {
            try {
                URL url = new URL(URL_BASE + idTenant);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                
                String responseStr = sb.toString();
                JSONArray arr;
                if (responseStr.trim().startsWith("{")) {
                    JSONObject root = new JSONObject(responseStr);
                    arr = root.optJSONArray("data");
                } else {
                    arr = new JSONArray(responseStr);
                }

                if (arr == null) arr = new JSONArray();

                stockItems.clear();
                int tersedia = 0, habis = 0;
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    // Ambil status_stok, jika tidak ada cek 'status', default 'tersedia'
                    String status = obj.optString("status_stok", obj.optString("status", "tersedia")).trim();
                    
                    // Normalisasi status: jika database menyimpan 1/0, ubah ke tersedia/habis
                    if (status.equals("1")) status = "tersedia";
                    else if (status.equals("0")) status = "habis";
                    
                    if ("tersedia".equalsIgnoreCase(status)) tersedia++;
                    else habis++;

                    stockItems.add(new Menu(
                            obj.optInt("id_menu", 0),
                            obj.optInt("id_tenant", 0),
                            obj.optString("nama_menu", "Menu"),
                            obj.optInt("harga", 0),
                            status,
                            obj.optString("deskripsi", "")
                    ));
                }
                
                final int fTersedia = tersedia, fHabis = habis;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvStockSummary.setText(fTersedia + " tersedia · " + fHabis + " habis");
                        StockAdapter adapter = new StockAdapter(stockItems, (idMenu, isAvailable) -> {
                            updateStockStatus(idMenu, isAvailable ? "tersedia" : "habis");
                        });
                        rvStock.setAdapter(adapter);
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void updateStockStatus(int idMenu, String status) {
        // Kita kirim "Tersedia" atau "Habis" sesuai dengan format database
        final String normalizedStatus = status.equalsIgnoreCase("tersedia") ? "Tersedia" : "Habis";

        new Thread(() -> {
            String responseBody = "";
            try {
                URL url = new URL("http://192.168.1.5/pmob/api_uas/update_stock.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);

                // Mengirim data dalam format Form-Data (umum untuk PHP $_POST)
                String data = "id_menu=" + idMenu + "&status_stok=" + URLEncoder.encode(normalizedStatus, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8"));
                os.flush();
                os.close();

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                responseBody = sb.toString();

                JSONObject jsonRes = new JSONObject(responseBody);
                String apiStatus = jsonRes.optString("status");
                String message = jsonRes.optString("message");

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if ("success".equalsIgnoreCase(apiStatus)) {
                            Toast.makeText(getContext(), "Berhasil diubah ke: " + normalizedStatus, Toast.LENGTH_SHORT).show();
                        } else {
                            // Tampilkan pesan error dari PHP (misal: kolom tidak ditemukan)
                            Toast.makeText(getContext(), "Gagal: " + message, Toast.LENGTH_LONG).show();
                        }
                        loadStock(); // Sinkronisasi ulang UI dengan Database
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                final String errorMsg = responseBody.isEmpty() ? e.getMessage() : responseBody;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // Jika muncul error PHP (Fatal error), akan terlihat di Toast ini
                        Toast.makeText(getContext(), "Error Update: " + errorMsg, Toast.LENGTH_LONG).show();
                        loadStock(); // Kembalikan posisi switch ke status asli di database
                    });
                }
            }
        }).start();
    }

    private static class StockAdapter extends RecyclerView.Adapter<StockAdapter.ViewHolder> {
        private final List<Menu> items;
        private final OnStockChangeListener listener;

        public StockAdapter(List<Menu> items, OnStockChangeListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tenant_stock, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Menu item = items.get(position);
            holder.tvMenuName.setText(item.getNama());
            boolean isAvailable = "tersedia".equalsIgnoreCase(item.getStatusStok());
            
            holder.switchStock.setOnCheckedChangeListener(null);
            holder.switchStock.setChecked(isAvailable);
            updateSwitchUI(holder, isAvailable);

            holder.switchStock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateSwitchUI(holder, isChecked);
                listener.onChange(item.getId(), isChecked);
            });
        }

        private void updateSwitchUI(ViewHolder holder, boolean isChecked) {
            int colorActive = 0xFF10B981; // Hijau
            int colorInactive = 0xFF9CA3AF; // Abu-abu
            int colorTrackActive = 0x6610B981;
            int colorTrackInactive = 0x669CA3AF;

            if (isChecked) {
                holder.tvMenuStatus.setText("Tersedia");
                holder.tvMenuStatus.setTextColor(colorActive);
                holder.vOverlay.setVisibility(View.GONE);
                holder.tvBadge.setVisibility(View.GONE);
                holder.switchStock.setThumbTintList(android.content.res.ColorStateList.valueOf(colorActive));
                holder.switchStock.setTrackTintList(android.content.res.ColorStateList.valueOf(colorTrackActive));
            } else {
                holder.tvMenuStatus.setText("Habis");
                holder.tvMenuStatus.setTextColor(colorInactive);
                holder.vOverlay.setVisibility(View.VISIBLE);
                holder.tvBadge.setVisibility(View.VISIBLE);
                holder.switchStock.setThumbTintList(android.content.res.ColorStateList.valueOf(colorInactive));
                holder.switchStock.setTrackTintList(android.content.res.ColorStateList.valueOf(colorTrackInactive));
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMenuName, tvBadge, tvMenuStatus;
            Switch switchStock;
            View vOverlay;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMenuName = itemView.findViewById(R.id.tvMenuName);
                tvMenuStatus = itemView.findViewById(R.id.tvMenuStatus);
                switchStock = itemView.findViewById(R.id.switchStock);
                vOverlay = itemView.findViewById(R.id.vOutofStockOverlay);
                tvBadge = itemView.findViewById(R.id.tvOutofStockBadge);
            }
        }

        interface OnStockChangeListener {
            void onChange(int idMenu, boolean isAvailable);
        }
    }
}
