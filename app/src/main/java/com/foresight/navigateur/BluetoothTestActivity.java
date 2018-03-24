package com.foresight.navigateur;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Set;

/*
To check if bluetooth is on and we have previously paired with the correct MAC address
 */

public class BluetoothTestActivity extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private TextView mBluetoothTextView;
    private BluetoothSocket socket;

    private OutputStream outputStream;
    private InputStream inputStream;

    boolean connected = false;

    Thread thread;
    byte buffer[];
    int bufferPosition;
    boolean stopThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_test);

        mBluetoothTextView = (TextView) findViewById(R.id.bluetooth_text_view);
        mBluetoothTextView.setText("");

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

        String spacing = "   ";

        if (mBluetoothAdapter != null) {
            if (mBluetoothAdapter.isEnabled()) {
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                String displayText = "Paired Devices: \n";

                if (pairedDevices.size() > 0) {
                    // There are paired devices. Get the name and address of each paired device.
                    for (BluetoothDevice device : pairedDevices) {

                        if (device.getAddress().equals(MapsActivity.DEVICE_ADDRESS)) {

                            displayText += device.getName() + spacing + device.getAddress() + spacing + "(Selected Device)\n";
                            mBluetoothDevice = device;
                            break;

                        }
                        else {
                            displayText += device.getName() + spacing + device.getAddress() + "\n"; // MAC address
                        }

                    }
                }

                mBluetoothTextView.setText(displayText);

            }
        }
    }

    //-----------Creatiing Connection------------------

    public void BTconnect() {

        connected = true;

        mBluetoothTextView.setText("");

        try {
            socket = mBluetoothDevice.createRfcommSocketToServiceRecord(MapsActivity.PORT_UUID);
            socket.connect();
            mBluetoothTextView.append(getString(R.string.bluetooth_socket_connected));
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
            mBluetoothTextView.append(getString(R.string.bluetooth_socket_failed));
        }

        if (connected) {
            try {
                outputStream=socket.getOutputStream();
                mBluetoothTextView.append(getString(R.string.bluetooth_output_created));
            } catch (IOException e) {
                e.printStackTrace();
                mBluetoothTextView.append(getString(R.string.bluetooth_output_failed));
            }

            try {
                inputStream=socket.getInputStream();
                mBluetoothTextView.append(getString(R.string.bluetooth_input_created));
            } catch (IOException e) {
                e.printStackTrace();
                mBluetoothTextView.append(getString(R.string.bluetooth_input_failed));
            }

        }

        //return connected;
    }

    void beginListenForData() {
        final Handler handler = new Handler();
        stopThread = false;
        buffer = new byte[1024];
        Thread thread  = new Thread(new Runnable() {
            public void run() {
                while(!Thread.currentThread().isInterrupted() && !stopThread) {
                    try {
                        int byteCount = inputStream.available();
                        if (byteCount > 1)
                        // TODO: Greater than 1? Get Rohan to pad output with leading zeroes
                        {
                            byte[] rawBytes = new byte[byteCount];
                            inputStream.read(rawBytes);
                            final String string = new String(rawBytes,"UTF-8");
                            handler.post(new Runnable() {
                                public void run()
                                {
                                    mBluetoothTextView.append("\nReceived String: " + string);
                                }
                            });

                        }
                    }
                    catch (IOException ex) {
                        stopThread = true;
                    }
                }
            }
        });
        thread.start();
    }

    public void sendData(String inputString) {
        if (connected) {
            inputString.concat("\n");
            try {
                outputStream.write(inputString.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBluetoothTextView.append(getString(R.string.bluetooth_sent_text, inputString));
        }
    }

    //-------------------Button Methods-------------------------------

    public void bluetoothFunctionOne(View view) {
        setupBluetooth();
    }

    public void bluetoothFunctionTwo(View view) {
        findPairedDevices();
    }

    public void bluetoothFunctionThree(View view) {
        findPairedDevices();
        BTconnect();
        beginListenForData();
    }

    public void testFunctionA(View view) {
        sendData("a");
    }

    public void testFunctionB(View view) {
        sendData("b");
    }

    public void testFunctionC(View view) {
        sendData("c");
    }

    public void testFunctionD(View view) {
        sendData("d");
    }

    public void testFunctionE(View view) {
        sendData("e");
    }

    public void testFunctionF(View view) {
        sendData("f");
    }

    public void testFunctionG(View view) {
        sendData("g");
    }

    public void testFunctionH(View view) {
        sendData("h");
    }
}
