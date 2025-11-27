package com.example.datareader;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;

import com.example.datareader.databinding.ActivitySensorDataBinding;

public class SensorData extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private CustomBluetooth myBluetooth;

    private ActivitySensorDataBinding binding;
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i("SensorData", "SensorData created.");

        binding = ActivitySensorDataBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        myBluetooth = CustomBluetooth.getInstance(this);

        myBluetooth.isConnected.observe(this, new Observer<Boolean>() {
            @Override
            public void onChanged(Boolean connected) {
                if (connected != null && !connected) {
                    Toast.makeText(SensorData.this, "Device Disconnected", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        });

        myBluetooth.receivedData.observe(this, new Observer<byte[]>() {
            @Override
            public void onChanged(byte[] data) {
                if (data != null) {
                    String hexData = bytesToHex(data);
                    binding.lblDataValue.setText(hexData);
                    Log.d("SensorData", "UI updated with data: " + hexData);
                }
            }
        });

        String deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);

        if (getSupportActionBar() != null && deviceName != null) {
            getSupportActionBar().setTitle(deviceName);
        }

        String info = "-O-: " + (deviceName != null ? deviceName : "Unknown Device");
        binding.connectedDeviceInfo.setText(info);
        binding.lblDataText.setText("Sensor Value:");
        binding.btnDisconnect.setText("Disconnect");
        Log.d("SensorData", "Button set.");

        binding.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("SensorData", "Disconnect button clicked.");

                if (myBluetooth != null) {
                    myBluetooth.disconnect();
                }

                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("SensorData", "onDestroy called, ensuring disconnection.");
        if (myBluetooth != null) {
            myBluetooth.disconnect();
        }
    }
}
