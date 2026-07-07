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
    private List<Menu> stockItems = new ArrayList<>();
    private String URL_BASE = "http://192.168.1.5/pmob/api_uas/get_menus.php?id_tenant=";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tenant_stock, container, false);
        rvStock = view.findViewById(R.id.rvTenantStock);
        rvStock.setLayoutManager(new LinearLayoutManager(getContext()));
        loadStock();
        return view;
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
                
                JSONArray arr = new JSONArray(sb.toString());
                stockItems.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    stockItems.add(new Menu(
                            obj.getInt("id_menu"),
                            obj.getInt("id_tenant"),
                            obj.getString("nama_menu"),
                            obj.getInt("harga"),
                            obj.getString("status_stok")
                    ));
                }
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
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
        new Thread(() -> {
            try {
                // Assuming an update_stock.php exists
                URL url = new URL("http://192.168.1.5/pmob/api_uas/update_stock.php");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                
                String data = "id_menu=" + idMenu + "&status_stok=" + URLEncoder.encode(status, "UTF-8");
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

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(), "Stok diperbarui", Toast.LENGTH_SHORT).show();
                        loadStock();
                    });
                }
            } catch (Exception e) { e.printStackTrace(); }
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
            holder.switchStock.setChecked("tersedia".equals(item.getStatusStok()));
            
            holder.vOverlay.setVisibility("habis".equals(item.getStatusStok()) ? View.VISIBLE : View.GONE);
            holder.tvBadge.setVisibility("habis".equals(item.getStatusStok()) ? View.VISIBLE : View.GONE);

            holder.switchStock.setOnCheckedChangeListener((buttonView, isChecked) -> {
                listener.onChange(item.getId(), isChecked);
            });
        }

        @Override
        public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvMenuName, tvBadge;
            Switch switchStock;
            View vOverlay;
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvMenuName = itemView.findViewById(R.id.tvMenuName);
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
