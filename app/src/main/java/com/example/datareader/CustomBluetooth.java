package com.example.datareader;

import android.Manifest;
import android.bluetooth.*;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.UUID;

public class CustomBluetooth {
    private static CustomBluetooth instance;
    private static final long SCAN_PERIOD = 10000; // Using a more reasonable 10 seconds
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bluetoothLeScanner;
    private boolean scanning = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Context context;

    public interface BluetoothListener {
        void onDeviceFound(BluetoothDevice device);
        void onScanStarted();
        void onScanStopped();
        void onDeviceConnected(BluetoothDevice device);
        void onDeviceDisconnected();
    }

    private BluetoothListener listener;
    private BluetoothGatt bluetoothGatt;

    private final MutableLiveData<Boolean> _isConnected = new MutableLiveData<>();
    public final LiveData<Boolean> isConnected = _isConnected;

    private final MutableLiveData<byte[]> _receivedData = new MutableLiveData<>();
    public final LiveData<byte[]> receivedData = _receivedData;

    private static final UUID TARGET_SERVICE_UUID = UUID.fromString("ea07beb5-483e-36e1-4688-b7f5ea61914b");
    private static final UUID TARGET_CHARACTERISTIC_UUID = UUID.fromString("4f4bc5c9-c331-8fcc-459e-1fb54ffac201");
    private static final UUID CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    private CustomBluetooth(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized CustomBluetooth getInstance(Context context) {
        if (instance == null) {
            instance = new CustomBluetooth(context);
        }
        return instance;
    }


    public void setBluetoothListener(BluetoothListener listener) {
        this.listener = listener;
    }

    public void setBluetoothAdapter(BluetoothAdapter adapter) {
        if (adapter != null) {
            this.bluetoothAdapter = adapter;
            this.bluetoothLeScanner = this.bluetoothAdapter.getBluetoothLeScanner();
        } else {
            Log.e("Bluetooth", "Provided BluetoothAdapter is null.");
        }
    }

    public void connectToDevice(BluetoothDevice device) {
        if (device == null) {
            Log.e("Bluetooth", "Cannot connect to a null device.");
            return;
        }
        stopScan();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Bluetooth", "BLUETOOTH_CONNECT permission not granted.");
                return;
            }
        }
        Log.d("Bluetooth", "Attempting to connect to GATT server on: " + device.getName());
        bluetoothGatt = device.connectGatt(context, false, gattCallback);
    }
    public void startScan() {
        if (scanning) {
            Log.d("Bluetooth", "Scan already in progress.");
            return;
        }
        scanLeDevice(true);
    }

    public void stopScan() {
        if (scanning) {
            scanLeDevice(false);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (bluetoothLeScanner == null) {
            Log.e("Bluetooth", "BluetoothLeScanner not initialized. Cannot scan.");
            return;
        }

        String requiredPermission = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                ? Manifest.permission.BLUETOOTH_SCAN
                : Manifest.permission.ACCESS_FINE_LOCATION;

        if (ContextCompat.checkSelfPermission(context, requiredPermission) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Bluetooth", "Scan permission not granted. The Activity should have requested it.");
            Toast.makeText(context, "Scan Permission Required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (enable) {
            handler.postDelayed(() -> {
                if (scanning) {
                    Log.d("Bluetooth", "Stopping scan due to timer.");
                    stopScan();
                }
            }, SCAN_PERIOD);

            scanning = true;
            if (listener != null) {
                listener.onScanStarted();
            }
            bluetoothLeScanner.startScan(scanCallback);
            Log.d("Bluetooth", "Scan started...");
            Toast.makeText(context, "Scan started...", Toast.LENGTH_SHORT).show();
        } else {
            scanning = false;
            if (listener != null) {
                listener.onScanStopped();
            }
            bluetoothLeScanner.stopScan(scanCallback);
            Log.d("Bluetooth", "Scan stopped.");
        }
    }

    public void disconnect(){
        if (bluetoothGatt == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Bluetooth", "BLUETOOTH_CONNECT permission not granted, cannot disconnect.");
                bluetoothGatt.close();
                bluetoothGatt = null;
                return;
            }
        }

        Log.d("Bluetooth", "Disconnecting from GATT server.");
        bluetoothGatt.disconnect();
        bluetoothGatt.close();
        bluetoothGatt = null;
    }


    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device != null && device.getName() != null) {
                if (listener != null) {
                    listener.onDeviceFound(device);
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.e("Bluetooth", "Scan failed with error code: " + errorCode);
        }
    };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i("BluetoothGatt", "Gatt Listener Triggered");
            BluetoothDevice device = gatt.getDevice();

            if (listener != null) {
                listener.onDeviceConnected(device);
            }

            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i("BluetoothGatt", "Successfully connected to " + device.getName());
                    _isConnected.postValue(true);
                    handler.postDelayed(() -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                            Log.e("BluetoothGatt", "Permission denied to discover services.");
                            return;
                        }
                        Log.i("BluetoothGatt", "Starting service discovery...");
                        gatt.discoverServices();
                    }, 500);
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i("BluetoothGatt", "Successfully disconnected from " + device.getName());
                    _isConnected.postValue(false);
                    stopPolling();
                    gatt.close();
                    bluetoothGatt = null;
                }
            } else {
                Log.w("BluetoothGatt", "GATT Error on connection state change. Status: " + status);
                _isConnected.postValue(false);
                gatt.close();
                stopPolling();
                bluetoothGatt = null;
            }
            }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                for(BluetoothGattService service : gatt.getServices())
                {
                    Log.i("BluetoothGatt", "Services discovered." + service.getUuid() + " : " + TARGET_SERVICE_UUID);
                }
                BluetoothGattService service = gatt.getService(TARGET_SERVICE_UUID);
                if (service != null) {
                    for(BluetoothGattCharacteristic charec : service.getCharacteristics())
                    {
                        Log.i("BluetoothGatt", "Characteristics discovered." + charec.getUuid() + " : " + TARGET_CHARACTERISTIC_UUID);
                    }
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(TARGET_CHARACTERISTIC_UUID);
                    if (characteristic == null) {
                        return;
                    }
                    int properties = characteristic.getProperties();
                    if ((properties & BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0 &&
                            (properties & BluetoothGattCharacteristic.PROPERTY_INDICATE) == 0) {
                        Log.e("BluetoothGatt", "Characteristic does not support notifications or indications!");
                        return;
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    boolean success = gatt.setCharacteristicNotification(characteristic, true);
                    if (success) {
                        Log.i("BluetoothGatt", "Successfully enabled local notifications for characteristic.");
                    } else {
                        Log.e("BluetoothGatt", "Failed to enable local notifications for characteristic.");
                    }
//                    properties = characteristic.getProperties();
//                    if ((properties & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 &&
//                            (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) {
//                        Log.w("BluetoothGatt", "Characteristic is not writable. Assuming no start command is needed.");
//                        return;
//                    }
//
//                    byte[] startCommand = new byte[]{(byte) 0x01};
//
//                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                        int result = gatt.writeCharacteristic(characteristic, startCommand, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
//                        Log.i("BluetoothGatt", "Initiating characteristic write with result code: " + result);
//                    } else {
//                        characteristic.setValue(startCommand);
//                        boolean writeSuccess = gatt.writeCharacteristic(characteristic);
//                        if (writeSuccess) {
//                            Log.i("BluetoothGatt", "Successfully initiated characteristic write.");
//                        } else {
//                            Log.e("BluetoothGatt", "Failed to initiate characteristic write.");
//                        }
//                    }
                    BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CCCD_UUID);
                    if (descriptor == null) {
                        Log.w("BluetoothGatt", "CCCD descriptor not found. Peripheral might start notifying anyway.");
                        return;
                    }

                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.e("BluetoothGatt", "Permission denied to write descriptor.");
                        return;
                    }
                    boolean writeSuccess = gatt.writeDescriptor(descriptor);
                    if (writeSuccess) {
                        Log.i("BluetoothGatt", "Writing to CCCD descriptor to enable notifications...");
                    } else {
                        Log.e("BluetoothGatt", "Failed to initiate descriptor write.");
                    }
                    startPolling(gatt,characteristic);
                } else {
                    Log.i("BluetoothGatt", "Service not found.");
                }
            }
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,
                                            byte[] value) {
            Log.i("BluetoothGatt", "Characteristic changed." + characteristic.getUuid() +" : " +
            value);
            if (TARGET_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                _receivedData.postValue(value);
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (CCCD_UUID.equals(descriptor.getUuid())) {
                    Log.i("BluetoothGatt", "SUCCESS: Device is now subscribed to notifications.");
                }
            } else {
                Log.e("BluetoothGatt", "FAILURE: Failed to write descriptor, status: " + status);
            }
        }

        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (TARGET_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                    byte[] data = characteristic.getValue();
                    Log.i("BluetoothGatt", "Manual read successful. Data: " + data);

                    _receivedData.postValue(data);
                }
            } else {
                Log.e("BluetoothGatt", "onCharacteristicRead failed with status: " + status);
            }
        }
    };
    private final Handler pollingHandler = new Handler(Looper.getMainLooper());
    private BluetoothGattCharacteristic pollingCharacteristic;
    private boolean isPolling = false;
    private static final int POLLING_INTERVAL_MS = 3000;

    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (bluetoothGatt != null && pollingCharacteristic != null && isPolling) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Log.e("BluetoothGatt", "Polling stopped: BLUETOOTH_CONNECT permission not granted.");
                    stopPolling();
                    return;
                }

                if (bluetoothGatt.readCharacteristic(pollingCharacteristic)) {
                    Log.d("BluetoothGatt", "Polling: Initiating characteristic read.");
                } else {
                    Log.e("BluetoothGatt", "Polling: Failed to initiate characteristic read.");
                }

                pollingHandler.postDelayed(this, POLLING_INTERVAL_MS);
            }
        }
    };

    private void startPolling(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        if (!isPolling) {
            Log.i("BluetoothGatt", "Starting polling every " + POLLING_INTERVAL_MS + " ms.");
            this.pollingCharacteristic = characteristic;
            this.isPolling = true;
            pollingHandler.post(pollingRunnable);
        }
    }
    private void stopPolling() {
        if (isPolling) {
            Log.i("BluetoothGatt", "Stopping polling.");
            isPolling = false;
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingCharacteristic = null;
        }
    }
}
