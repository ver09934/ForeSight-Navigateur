package com.foresight.navigateur;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

public class DataHolder {

    private static BluetoothDevice data = null;

    public static final String DEVICE_ADDRESS="00:14:03:05:FF:E6";
    public static final UUID PORT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Serial Port Service ID

    // May need to be non-static?
    public static BluetoothDevice getData() {
        return data;
    }

    // May need to be non-static?
    public static void setData(BluetoothDevice data) {
        DataHolder.data = data;
    }

    public static boolean containsData() {
        return (data != null);
    }

}