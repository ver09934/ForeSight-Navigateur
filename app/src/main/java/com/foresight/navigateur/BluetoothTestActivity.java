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
To check if bluetooth is on and we have previously paired with the correct MAC address
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

    //------------------------Enabling Bluetooth-----------------------

    public void setupBluetooth() {

        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Your device does not support bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                Toast.makeText(getApplicationContext(), "Bluetooth supported and already enabled", Toast.LENGTH_SHORT).show();
            }
        }


    }

    //--------------------Finding and Storing Bluetooth Device--------------------------

    public void findPairedDevices() {

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                String displayText = "Paired Devices: \n";

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {

                        if (device.getAddress().equals(DataHolder.DEVICE_ADDRESS)) {

                            displayText += device.getName() + ", " + device.getAddress() + "(Selected Device)\n";

                        }
                        else {
                            displayText += device.getName() + ", " + device.getAddress() + "\n"; // MAC address
                        }

                    }
                }

                mBluetoothTextView.setText(displayText);

                /*
                if (found)
                    Toast.makeText(getApplicationContext(), "HC-06 found", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(getApplicationContext(), "Please pair with HC-06 " + DataHolder.DEVICE_ADDRESS, Toast.LENGTH_SHORT).show();
                    */

            }
        }
    }

    //-------------------Button Methods-------------------------------

    public void bluetoothFunctionOne(View view) {
        setupBluetooth();
    }

    public void bluetoothFunctionTwo(View view) {
        findPairedDevices();
    }

}
