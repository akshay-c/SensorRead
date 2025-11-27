package com.example.datareader;

import android.graphics.Color;
import android.bluetooth.BluetoothDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceViewHolder> {

    private final List<BluetoothDevice> deviceList = new ArrayList<>();
    private int selectedPosition = RecyclerView.NO_POSITION;

    public interface OnItemClickListener {
        void onItemClick(BluetoothDevice device);
    }
    protected OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public DeviceViewHolder onCreateViewHolder( ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_2, parent, false);
        return new DeviceViewHolder(view);
    }

    @Override
    public void onBindViewHolder( DeviceViewHolder holder, int position) {
        BluetoothDevice device = deviceList.get(position);
        holder.deviceName.setText(device.getName());
        holder.deviceAddress.setText(device.getAddress());
        if (selectedPosition == position) {
            holder.itemView.setBackgroundColor(Color.GREEN);
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }

    public void addDevice(BluetoothDevice device) {
        if (!deviceList.contains(device)) {
            Log.i("DeviceList", "Adding Device: " + device.getName() + " | Address: " + device.getAddress() + ".");
            deviceList.add(device);
            notifyItemInserted(deviceList.size() - 1);
        }
    }

    public void clearDevices() {
        int size = deviceList.size();
        if (size > 0) {
            deviceList.clear();
            notifyItemRangeRemoved(0, size);
        }
    }
    public BluetoothDevice getSelectedDevice() {
        if (selectedPosition != RecyclerView.NO_POSITION && selectedPosition < deviceList.size()) {
            return deviceList.get(selectedPosition);
        }
        return null;
    }
    class DeviceViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceAddress;

        DeviceViewHolder(@NonNull View itemView) {
            super(itemView);
            deviceName = itemView.findViewById(android.R.id.text1);
            deviceAddress = itemView.findViewById(android.R.id.text2);
            itemView.setOnClickListener(v -> {
                Log.i("DeviceList", "ItemClicked ");
                if (listener != null) {
                    Log.i("DeviceList", "Listener Not Null ");
                    int position = getBindingAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemClick(deviceList.get(position));
                        int previousPosition = selectedPosition;

                        if (previousPosition != RecyclerView.NO_POSITION) {
                            notifyItemChanged(previousPosition);
                        }
                        selectedPosition = position;
                        notifyItemChanged(selectedPosition);
                    }
                }
            });
        }
    }
}
