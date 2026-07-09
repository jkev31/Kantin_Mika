package com.example.kantin_mika;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TenantOrdersFragment extends Fragment {
    private RecyclerView rvOrders;
    private TextView tvCountBaru, tvCountDiproses, tvCountSelesai;
    private TextView chipSemua, chipBaru, chipDiproses, chipSelesai;
    private OrderAdapter adapter;

    private String currentFilter = "semua";
    private List<Order> orderList = new ArrayList<>();
    // Sesuaikan URL ini dengan API kamu
    private String URL_GET_ORDERS = "http://192.168.101.4/pmob/api_uas/get_tenant_orders.php?id_tenant=";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tenant_orders, container, false);

        rvOrders = view.findViewById(R.id.rvTenantOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        tvCountBaru = view.findViewById(R.id.tvCountBaru);
        tvCountDiproses = view.findViewById(R.id.tvCountDiproses);
        tvCountSelesai = view.findViewById(R.id.tvCountSelesai);

        chipSemua = view.findViewById(R.id.chipSemua);
        chipBaru = view.findViewById(R.id.chipBaru);
        chipDiproses = view.findViewById(R.id.chipDiproses);
        chipSelesai = view.findViewById(R.id.chipSelesai);

        chipSemua.setOnClickListener(v -> applyFilter("semua"));
        chipBaru.setOnClickListener(v -> applyFilter("baru"));
        chipDiproses.setOnClickListener(v -> applyFilter("diproses"));
        chipSelesai.setOnClickListener(v -> applyFilter("selesai"));

        adapter = new OrderAdapter(new ArrayList<>());
        rvOrders.setAdapter(adapter);

        loadOrders();

        return view;
    }

    private void applyFilter(String filter) {
        currentFilter = filter;

        chipSemua.setBackgroundResource(R.drawable.bg_chip_muted);
        chipBaru.setBackgroundResource(R.drawable.bg_chip_muted);
        chipDiproses.setBackgroundResource(R.drawable.bg_chip_muted);
        chipSelesai.setBackgroundResource(R.drawable.bg_chip_muted);
        chipSemua.setTextColor(0xFF6B7280);
        chipBaru.setTextColor(0xFF6B7280);
        chipDiproses.setTextColor(0xFF6B7280);
        chipSelesai.setTextColor(0xFF6B7280);

        TextView activeChip = chipSemua;
        if (filter.equals("baru")) activeChip = chipBaru;
        else if (filter.equals("diproses")) activeChip = chipDiproses;
        else if (filter.equals("selesai")) activeChip = chipSelesai;

        activeChip.setBackgroundResource(R.drawable.bg_chip_primary);
        activeChip.setTextColor(0xFFFFFFFF);

        renderFilteredList();
    }

    private void renderFilteredList() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : orderList) {
            if (currentFilter.equals("semua")) {
                filtered.add(o);
            } else if (currentFilter.equals("diproses")
                    && ("diproses".equalsIgnoreCase(o.status) || "proses".equalsIgnoreCase(o.status))) {
                filtered.add(o);
            } else if (currentFilter.equals(o.status.toLowerCase())) {
                filtered.add(o);
            }
        }
        adapter.updateData(filtered);
    }

    private void loadOrders() {
        if (getActivity() == null) return;
        int idTenant = getActivity().getSharedPreferences("TenantPref", Context.MODE_PRIVATE).getInt("id_tenant", 1);

        new Thread(() -> {
            try {
                URL url = new URL(URL_GET_ORDERS + idTenant);
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

                orderList.clear();
                int baru = 0, proses = 0, selesai = 0;

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Order order = new Order(
                            obj.optString("id_order", "N/A"),
                            obj.optString("nomor_meja", "-"),
                            obj.optInt("total_bayar", 0),
                            obj.optString("status_pesanan", obj.optString("status", "baru")),
                            obj.optString("waktu_order", "12.00")
                    );

                    if (obj.has("items")) {
                        JSONArray itemsArr = obj.optJSONArray("items");
                        if (itemsArr != null) {
                            for(int j=0; j<itemsArr.length(); j++) {
                                JSONObject itemObj = itemsArr.getJSONObject(j);
                                order.addItem(itemObj.optString("nama_menu", "Menu"), itemObj.optInt("qty", 0));
                            }
                        }
                    }
                    orderList.add(order);

                    if ("baru".equalsIgnoreCase(order.status)) baru++;
                    else if ("diproses".equalsIgnoreCase(order.status) || "proses".equalsIgnoreCase(order.status)) proses++;
                    else selesai++;
                }

                final int fBaru = baru, fProses = proses, fSelesai = selesai;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvCountBaru.setText(String.valueOf(fBaru));
                        tvCountDiproses.setText(String.valueOf(fProses));
                        tvCountSelesai.setText(String.valueOf(fSelesai));
                        renderFilteredList();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class Order {
        String id, meja, status, waktu;
        int total;
        List<String> itemNames = new ArrayList<>();
        List<Integer> itemQtys = new ArrayList<>();

        Order(String id, String meja, int total, String status, String waktu) {
            this.id = id; this.meja = meja; this.total = total; this.status = status; this.waktu = waktu;
        }
        void addItem(String name, int qty) { itemNames.add(name); itemQtys.add(qty); }
    }

    private static class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
        private final List<Order> items;
        OrderAdapter(List<Order> items) { this.items = items; }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order_tenant, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Order o = items.get(position);
            holder.tvId.setText(o.id);
            holder.tvMeja.setText("Meja " + o.meja);
            holder.tvWaktu.setText(o.waktu);

            NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            holder.tvTotal.setText(nf.format(o.total));
            holder.tvStatus.setText(o.status.toUpperCase());

            holder.llItems.removeAllViews();
            for (int i=0; i<o.itemNames.size(); i++) {
                TextView tv = new TextView(holder.itemView.getContext());
                tv.setText("• " + o.itemNames.get(i) + " x" + o.itemQtys.get(i));
                tv.setTextColor(0xFF111827);
                tv.setTextSize(14);
                holder.llItems.addView(tv);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }

        public void updateData(List<Order> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvId, tvMeja, tvWaktu, tvTotal, tvStatus;
            LinearLayout llItems;
            ViewHolder(View v) {
                super(v);
                tvId = v.findViewById(R.id.tvOrderId);
                tvMeja = v.findViewById(R.id.tvTableInfo);
                tvWaktu = v.findViewById(R.id.tvTimeInfo);
                tvTotal = v.findViewById(R.id.tvOrderTotal);
                tvStatus = v.findViewById(R.id.tvOrderStatusBadge);
                llItems = v.findViewById(R.id.llOrderItemsContainer);
            }
        }
    }
}
