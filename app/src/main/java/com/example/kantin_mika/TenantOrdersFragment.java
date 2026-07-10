package com.example.kantin_mika;

import android.content.Context;
import android.widget.Toast;
import java.io.OutputStream;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TenantOrdersFragment extends Fragment {
    private RecyclerView rvOrders;
    private TextView tvCountBaru, tvCountDiproses, tvCountDiantar, tvCountSelesai;
    private TextView chipSemua, chipBaru, chipDiproses, chipDiantar, chipSelesai;
    private OrderAdapter adapter;

    private String currentFilter = "semua";
    private List<Order> orderList = new ArrayList<>();
    // Sesuaikan URL ini dengan API kamu
    private String URL_GET_ORDERS = "http://192.168.101.7/pmob/api_uas/get_tenant_orders.php?id_tenant=";
    private String URL_UPDATE_STATUS = "http://192.168.101.7/pmob/api_uas/update_order_status.php";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tenant_orders, container, false);

        rvOrders = view.findViewById(R.id.rvTenantOrders);
        rvOrders.setLayoutManager(new LinearLayoutManager(getContext()));

        tvCountBaru = view.findViewById(R.id.tvCountBaru);
        tvCountDiproses = view.findViewById(R.id.tvCountDiproses);
        tvCountDiantar = view.findViewById(R.id.tvCountDiantar);
        tvCountSelesai = view.findViewById(R.id.tvCountSelesai);

        chipSemua = view.findViewById(R.id.chipSemua);
        chipBaru = view.findViewById(R.id.chipBaru);
        chipDiproses = view.findViewById(R.id.chipDiproses);
        chipDiantar = view.findViewById(R.id.chipDiantar);
        chipSelesai = view.findViewById(R.id.chipSelesai);

        chipSemua.setOnClickListener(v -> applyFilter("semua"));
        chipBaru.setOnClickListener(v -> applyFilter("baru"));
        chipDiproses.setOnClickListener(v -> applyFilter("diproses"));
        chipDiantar.setOnClickListener(v -> applyFilter("diantar"));
        chipSelesai.setOnClickListener(v -> applyFilter("selesai"));

        adapter = new OrderAdapter(new ArrayList<>(), this::updateOrderStatus);
        rvOrders.setAdapter(adapter);

        loadOrders();

        return view;
    }





    private void applyFilter(String filter) {
        currentFilter = filter;

        chipSemua.setBackgroundResource(R.drawable.bg_chip_muted);
        chipBaru.setBackgroundResource(R.drawable.bg_chip_muted);
        chipDiproses.setBackgroundResource(R.drawable.bg_chip_muted);
        chipDiantar.setBackgroundResource(R.drawable.bg_chip_muted);
        chipSelesai.setBackgroundResource(R.drawable.bg_chip_muted);
        chipSemua.setTextColor(0xFF6B7280);
        chipBaru.setTextColor(0xFF6B7280);
        chipDiproses.setTextColor(0xFF6B7280);
        chipDiantar.setTextColor(0xFF6B7280);
        chipSelesai.setTextColor(0xFF6B7280);

        TextView activeChip = chipSemua;
        if (filter.equals("baru")) activeChip = chipBaru;
        else if (filter.equals("diproses")) activeChip = chipDiproses;
        else if (filter.equals("diantar")) activeChip = chipDiantar;
        else if (filter.equals("selesai")) activeChip = chipSelesai;

        activeChip.setBackgroundResource(R.drawable.bg_chip_primary);
        activeChip.setTextColor(0xFFFFFFFF);

        renderFilteredList();
    }

    private void renderFilteredList() {
        List<Order> filtered = new ArrayList<>();
        for (Order o : orderList) {
            String status = o.status.toLowerCase();
            if (currentFilter.equals("semua")) {
                filtered.add(o);
            } else if (currentFilter.equals("baru") && (status.equals("baru") || status.equals("menunggu"))) {
                filtered.add(o);
            } else if (currentFilter.equals("diproses") && (status.equals("diproses") || status.equals("proses"))) {
                filtered.add(o);
            } else if (currentFilter.equals("diantar") && status.equals("diantar")) {
                filtered.add(o);
            } else if (currentFilter.equals("selesai") && status.equals("selesai")) {
                filtered.add(o);
            }
        }
        adapter.updateData(filtered);
    }
    private void updateOrderStatus(String idOrder, String newStatus) {
        if (getActivity() == null) return;
        int idTenant = getActivity().getSharedPreferences("TenantPref", Context.MODE_PRIVATE).getInt("id_tenant", 1);

        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id_order", idOrder);
                payload.put("status_pesanan", newStatus);
                payload.put("id_tenant", idTenant); // Tambahkan id_tenant ke payload

                URL url = new URL(URL_UPDATE_STATUS);
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
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject resObj = new JSONObject(sb.toString());
                boolean success = "success".equals(resObj.optString("status"));

                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (success) loadOrders();
                        else Toast.makeText(getContext(), "Gagal update status", Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
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
                android.util.Log.d("TENANT_ORDER_DEBUG", "Response: " + responseStr);

                JSONArray arr;
                if (responseStr.trim().startsWith("{")) {
                    JSONObject root = new JSONObject(responseStr);
                    arr = root.optJSONArray("data");
                } else {
                    arr = new JSONArray(responseStr);
                }

                if (arr == null) arr = new JSONArray();

                orderList.clear();
                int baru = 0, proses = 0, diantar = 0, selesai = 0;

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String status = obj.optString("status_pesanan", obj.optString("status", "Menunggu"));
                    
                    Order order = new Order(
                            obj.optString("id_order", "N/A"),
                            obj.optString("nomor_meja", "-"),
                            obj.optInt("total_bayar", 0),
                            status,
                            obj.optString("waktu_order", obj.optString("created_at", "12.00"))
                    );

                    if (obj.has("items")) {
                        JSONArray itemsArr = obj.optJSONArray("items");
                        if (itemsArr != null) {
                            for(int j=0; j<itemsArr.length(); j++) {
                                JSONObject itemObj = itemsArr.getJSONObject(j);
                                // Prefer subtotal from item, or calculate it
                                double itemSubtotal = itemObj.optDouble("subtotal", 0);
                                if (itemSubtotal == 0) {
                                    itemSubtotal = itemObj.optDouble("harga_satuan", 0) * itemObj.optInt("qty", 0);
                                }
                                
                                order.addItem(
                                        itemObj.optString("nama_menu", "Menu"),
                                        itemObj.optInt("qty", 0),
                                        itemSubtotal
                                );
                            }
                        }
                    }
                    
                    // If total_bayar is 0 from API, calculate from items
                    if (order.total == 0) {
                        double calculatedTotal = 0;
                        for (Double sub : order.itemSubtotals) calculatedTotal += sub;
                        order.total = (int) calculatedTotal;
                    }

                    orderList.add(order);

                    String sLow = status.toLowerCase();
                    if (sLow.equals("baru") || sLow.equals("menunggu")) baru++;
                    else if (sLow.equals("diproses") || sLow.equals("proses")) proses++;
                    else if (sLow.equals("diantar")) diantar++;
                    else if (sLow.equals("selesai")) selesai++;
                }

                final int fBaru = baru, fProses = proses, fDiantar = diantar, fSelesai = selesai;
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        tvCountBaru.setText(String.valueOf(fBaru));
                        tvCountDiproses.setText(String.valueOf(fProses));
                        tvCountDiantar.setText(String.valueOf(fDiantar));
                        tvCountSelesai.setText(String.valueOf(fSelesai));
                        renderFilteredList();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getActivity() != null) {
                    getActivity().runOnUiThread(() -> 
                        Toast.makeText(getContext(), "Gagal memuat pesanan: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private static class Order {
        String id, meja, status, waktu;
        int total;
        List<String> itemNames = new ArrayList<>();
        List<Integer> itemQtys = new ArrayList<>();
        List<Double> itemSubtotals = new ArrayList<>();

        Order(String id, String meja, int total, String status, String waktu) {
            this.id = id; this.meja = meja; this.total = total; this.status = status; this.waktu = waktu;
        }
        void addItem(String name, int qty, double subtotal) {
            itemNames.add(name);
            itemQtys.add(qty);
            itemSubtotals.add(subtotal);
        }
    }

    // Helper terpusat supaya format harga konsisten di seluruh fragment.
    private static String formatRupiah(double value) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("id", "ID"));
        nf.setMaximumFractionDigits(0);
        return "Rp " + nf.format(value);
    }

    // waktu_order dari database berupa datetime penuh (yyyy-MM-dd HH:mm:ss).
    // Figma cuma butuh jam:menit, jadi kita parse lalu format ulang.
    private static String formatJam(String rawWaktu) {
        if (rawWaktu == null || rawWaktu.isEmpty()) return "-";
        String[] patterns = {"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "HH:mm:ss", "HH:mm"};
        for (String pattern : patterns) {
            try {
                SimpleDateFormat inFmt = new SimpleDateFormat(pattern, new Locale("id", "ID"));
                Date parsed = inFmt.parse(rawWaktu);
                return new SimpleDateFormat("HH:mm", new Locale("id", "ID")).format(parsed);
            } catch (ParseException ignored) {
                // coba pattern berikutnya
            }
        }
        return rawWaktu;
    }

    private static class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {
        private final List<Order> items;
        private final OnStatusUpdateListener listener;

        OrderAdapter(List<Order> items, OnStatusUpdateListener listener) {
            this.items = items;
            this.listener = listener;
        }

        interface OnStatusUpdateListener {
            void onStatusUpdate(String idOrder, String newStatus);
        }

        void updateData(List<Order> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

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
            holder.tvWaktu.setText(formatJam(o.waktu));
            holder.tvTotal.setText(formatRupiah(o.total));
            holder.tvStatus.setText(o.status.toUpperCase());

            holder.llItems.removeAllViews();
            for (int i = 0; i < o.itemNames.size(); i++) {
                LinearLayout row = new LinearLayout(holder.itemView.getContext());
                row.setOrientation(LinearLayout.HORIZONTAL);
                if (i > 0) row.setPadding(0, 6, 0, 0);

                TextView tvName = new TextView(holder.itemView.getContext());
                tvName.setLayoutParams(new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                tvName.setText(o.itemNames.get(i) + " ×" + o.itemQtys.get(i));
                tvName.setTextColor(0xFF111827);
                tvName.setTextSize(14);

                TextView tvPrice = new TextView(holder.itemView.getContext());
                tvPrice.setText(formatRupiah(o.itemSubtotals.get(i)));
                tvPrice.setTextColor(0xFF111827);
                tvPrice.setTextSize(14);

                row.addView(tvName);
                row.addView(tvPrice);
                holder.llItems.addView(row);
            }
            String status = o.status.toLowerCase();

            if (status.equals("baru") || status.equals("menunggu")) {
                holder.tvStatus.setText("Pesanan Baru");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_outline_orange);
                holder.tvStatus.setTextColor(0xFFEA580C);

                holder.btnOrderAction.setVisibility(View.VISIBLE);
                holder.btnOrderAction.setText("Mulai Proses");
                holder.btnOrderAction.setBackgroundResource(R.drawable.bg_rounded_blue);
                holder.btnOrderAction.setOnClickListener(v -> listener.onStatusUpdate(o.id, "Diproses"));

            } else if (status.equals("diproses") || status.equals("proses")) {
                holder.tvStatus.setText("Diproses");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_blue);
                holder.tvStatus.setTextColor(0xFF2563EB);

                holder.btnOrderAction.setVisibility(View.VISIBLE);
                holder.btnOrderAction.setText("Tandai Diantar");
                holder.btnOrderAction.setBackgroundResource(R.drawable.bg_rounded_primary);
                holder.btnOrderAction.setOnClickListener(v -> listener.onStatusUpdate(o.id, "Diantar"));

            } else if (status.equals("diantar")) {
                holder.tvStatus.setText("Diantar");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_primary);
                holder.tvStatus.setTextColor(0xFFFFFFFF);
                
                holder.btnOrderAction.setVisibility(View.VISIBLE);
                holder.btnOrderAction.setText("Menunggu Pelanggan");
                holder.btnOrderAction.setBackgroundResource(R.drawable.bg_rounded_muted);
                holder.btnOrderAction.setOnClickListener(null);
                holder.btnOrderAction.setEnabled(false);

            } else if (status.equals("selesai")) {
                holder.tvStatus.setText("Selesai");
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_green);
                holder.tvStatus.setTextColor(0xFF059669);
                holder.btnOrderAction.setVisibility(View.GONE);
            } else {
                // Unknown status
                holder.tvStatus.setText(o.status.toUpperCase());
                holder.tvStatus.setBackgroundResource(R.drawable.bg_chip_muted);
                holder.tvStatus.setTextColor(0xFF6B7280);
                holder.btnOrderAction.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() { return items.size(); }



        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvId, tvMeja, tvWaktu, tvTotal, tvStatus, btnOrderAction;
            LinearLayout llItems;
            ViewHolder(View v) {
                super(v);
                tvId = v.findViewById(R.id.tvOrderId);
                tvMeja = v.findViewById(R.id.tvTableInfo);
                tvWaktu = v.findViewById(R.id.tvTimeInfo);
                tvTotal = v.findViewById(R.id.tvOrderTotal);
                tvStatus = v.findViewById(R.id.tvOrderStatusBadge);
                llItems = v.findViewById(R.id.llOrderItemsContainer);
                btnOrderAction = v.findViewById(R.id.btnOrderAction);
            }
        }
    }
}