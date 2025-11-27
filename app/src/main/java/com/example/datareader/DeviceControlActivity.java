package com.example.datareader;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.TextView;
import android.util.Log;

public class DeviceControlActivity extends AppCompatActivity {

    public static final String EXTRA_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRA_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView textView = new TextView(this);
        setContentView(textView);

        String deviceName = getIntent().getStringExtra(EXTRA_DEVICE_NAME);
        String deviceAddress = getIntent().getStringExtra(EXTRA_DEVICE_ADDRESS);

        if (getSupportActionBar() != null) {
            if (deviceName != null) {
                getSupportActionBar().setTitle(deviceName);
            } else {
                getSupportActionBar().setTitle("Device Control");
            }
        }

        String nameForDisplay = (deviceName != null) ? deviceName : "Unknown Device";
        String addressForDisplay = (deviceAddress != null) ? deviceAddress : "No Address";

        String displayText = "Device Name: " + nameForDisplay + "\n" + "Device Address: " + addressForDisplay;
        textView.setText(displayText);
        textView.setTextSize(18);


        Log.d("DeviceControlActivity", "Activity created for " + deviceName);
    }
}
