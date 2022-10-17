package com.example.blemultimeterapp;

import android.bluetooth.le.ScanResult;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.blemultimeterapp.databinding.RowScanResultBinding;

import java.util.List;


public class ScanResultAdapter extends RecyclerView.Adapter<ScanResultAdapter.ViewHolder> {
    final private List<ScanResult> items;
    final private ClickAction onClick;

    interface ClickAction {
        void action(ScanResult result);
    }

    public ScanResultAdapter(List<ScanResult> items, ClickAction onClick) {
        this.items = items;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RowScanResultBinding view = RowScanResultBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        final private RowScanResultBinding itemView;
        public ViewHolder(@NonNull RowScanResultBinding itemView) {
            super(itemView.getRoot());
            this.itemView = itemView;
        }

        void bind(ScanResult result) {
            itemView.deviceName.setText(result.getDevice().getName());
            itemView.macAddress.setText(result.getDevice().getAddress());
            itemView.signalStrength.setText(result.getRssi() + " dBm");
            itemView.getRoot().setOnClickListener(view -> {
               ScanResultAdapter.this.onClick.action(result);
            });
        }
    }
}
