package com.foresight.navigateur;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

public class BluetoothTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_test);

        enableBluetooth();

    }

    public static final int REQUEST_ENABLE_BT = 1;
    public BluetoothAdapter mBluetoothAdapter;

    public void enableBluetooth() {

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support bluetooth", Toast.LENGTH_LONG).show();
        }
        else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
            else {
                Toast.makeText(getApplicationContext(), "Bluetooth supported and already enabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    //public void discoverDevices() {}

}
