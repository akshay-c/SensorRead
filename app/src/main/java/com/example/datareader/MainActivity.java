package com.example.datareader;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.example.datareader.SensorData;

import com.example.datareader.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity implements CustomBluetooth.BluetoothListener {

    private ActivityMainBinding binding;
    private CustomBluetooth myBluetooth;
    private BluetoothAdapter bluetoothAdapter;
    private DeviceListAdapter deviceListAdapter;
    private final ActivityResultLauncher<Intent> enableBluetoothLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Log.d("Bluetooth", "User enabled Bluetooth. Now checking permissions.");
                    requestBlePermissions();
                } else {
                    Toast.makeText(this, "Bluetooth is required to scan for devices", Toast.LENGTH_LONG).show();
                }
            });

    private final ActivityResultLauncher<String[]> requestPermissionsLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = permissions.values().stream().allMatch(granted -> granted);

                if (allGranted) {
                    Log.d("Bluetooth", "All permissions granted. Starting scan.");
                    myBluetooth.startScan();
                } else {
                    Toast.makeText(this, "Scan and Connect permissions are required", Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        deviceListAdapter = new DeviceListAdapter();
        deviceListAdapter.setOnItemClickListener(device -> {
            Log.d("MainActivity", "Item clicked: " + device.getName());
            Toast.makeText(this, "Selected: " + device.getName(), Toast.LENGTH_SHORT).show();
        });
        binding.deviceList.setLayoutManager(new LinearLayoutManager(this));
        binding.deviceList.setAdapter(deviceListAdapter);

        myBluetooth = CustomBluetooth.getInstance(this);
        myBluetooth.setBluetoothListener(this);

        BluetoothManager bluetoothManager = getSystemService(BluetoothManager.class);
        if (bluetoothManager != null) {
            bluetoothAdapter = bluetoothManager.getAdapter();
            myBluetooth.setBluetoothAdapter(bluetoothAdapter);
        }

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "This device does not support Bluetooth", Toast.LENGTH_LONG).show();
            binding.btnScan.setEnabled(false);
            return;
        }

        binding.btnScan.setOnClickListener(v -> {
            Log.d("Bluetooth", "Scan button clicked!");
            startScanProcess();
        });

        binding.btnConnect.setOnClickListener(v -> {
            BluetoothDevice selectedDevice = deviceListAdapter.getSelectedDevice();

            if (selectedDevice != null) {
                Log.d("MainActivity", "Connect button clicked. Attempting to connect to: " + selectedDevice.getName());
                Toast.makeText(this, "Connecting to " + selectedDevice.getName(), Toast.LENGTH_SHORT).show();

                myBluetooth.stopScan();

                myBluetooth.connectToDevice(selectedDevice);
            } else {
                Toast.makeText(this, "Please select a device from the list first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startScanProcess() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBluetoothLauncher.launch(enableBtIntent);
        } else {
            requestBlePermissions();
        }
    }

    private void requestBlePermissions() {
        String[] requiredPermissions;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requiredPermissions = new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT};
        } else {
            requiredPermissions = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        }

        boolean allPermissionsGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allPermissionsGranted = false;
                break;
            }
        }

        if (allPermissionsGranted) {
            myBluetooth.startScan();
        } else {
            requestPermissionsLauncher.launch(requiredPermissions);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (myBluetooth != null) {
            myBluetooth.stopScan();
        }
    }
    @Override
    public void onDeviceFound(BluetoothDevice device) {
        runOnUiThread(() -> {
            deviceListAdapter.addDevice(device);
        });
    }

    @Override
    public void onScanStarted() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Scan Started...", Toast.LENGTH_SHORT).show();
        });
    }
    @Override
    public void onScanStopped() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Scan Stopped", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDeviceConnected(BluetoothDevice device) {
        runOnUiThread(() -> {
            Log.i("MainActivity", "Device connected. Opening control activity.");
            Toast.makeText(this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(MainActivity.this, SensorData.class);

            intent.putExtra("DEVICE_NAME", device.getName());
            intent.putExtra("DEVICE_ADDRESS", device.getAddress());

            startActivity(intent);
        });
    }
    @Override
    public void onDeviceDisconnected() {
        runOnUiThread(() -> {
            Toast.makeText(this, "Device disconnected", Toast.LENGTH_SHORT).show();
        });
    }

}