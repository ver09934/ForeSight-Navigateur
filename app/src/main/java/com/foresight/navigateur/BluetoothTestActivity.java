package com.foresight.navigateur;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

/*
Bluetooth setup and testing class.
Contains a lot of code borrowed from the android developer guides,
with a link available here:
https://developer.android.com/guide/topics/connectivity/bluetooth.html
*/

public class BluetoothTestActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private TextView mBluetoothTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_test);

        mBluetoothTextView = (TextView) findViewById(R.id.bluetooth_text_view);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    }

    public void enableBluetooth() {
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

    public void listDevicesToTextView() {
        if (mBluetoothAdapter != null) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            String displayText = "Paired Devices: \n";

            if (pairedDevices.size() > 0) {
                // There are paired devices. Get the name and address of each paired device.
                for (BluetoothDevice device : pairedDevices) {
                    displayText += "Name: " + device.getName() + " ";
                    displayText += "Mac address: " + device.getAddress() + "\n"; // MAC address
                }
            }

            mBluetoothTextView.setText(displayText);
        }
    }

    public void testMethod() {

    }

    public void bluetoothFunctionOne(View view) {
        enableBluetooth();
    }

    public void bluetoothFunctionTwo(View view) {
        listDevicesToTextView();
    }

    public void bluetoothFunctionThree(View view) {
        testMethod();
    }

}
