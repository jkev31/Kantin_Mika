package com.example.kantin_mika;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ScanManualActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_manual);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnManualBack).setOnClickListener(v -> finish());

        RecyclerView rvTableNumbers = findViewById(R.id.rvTableNumbers);
        rvTableNumbers.setLayoutManager(new GridLayoutManager(this, 3));

        List<String> tables = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            tables.add(String.valueOf(i));
        }

        TableAdapter adapter = new TableAdapter(tables, tableNumber -> {
            SharedPreferences pref = getSharedPreferences("KantinPref", MODE_PRIVATE);
            pref.edit().putString("nomor_meja", tableNumber).apply();
            startActivity(new Intent(ScanManualActivity.this, HomeActivity.class));
        });
        rvTableNumbers.setAdapter(adapter);
    }

    private static class TableAdapter extends RecyclerView.Adapter<TableAdapter.ViewHolder> {
        private final List<String> tables;
        private final OnTableClickListener listener;

        public TableAdapter(List<String> tables, OnTableClickListener listener) {
            this.tables = tables;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_table, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String table = tables.get(position);
            holder.tvTableNumber.setText(table);
            holder.itemView.setOnClickListener(v -> listener.onTableClick(table));
        }

        @Override
        public int getItemCount() {
            return tables.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvTableNumber;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvTableNumber = itemView.findViewById(R.id.tvTableNumber);
            }
        }

        interface OnTableClickListener {
            void onTableClick(String tableNumber);
        }
    }
}
