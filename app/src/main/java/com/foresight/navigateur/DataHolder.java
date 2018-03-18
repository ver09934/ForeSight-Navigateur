package com.foresight.navigateur;

import android.bluetooth.BluetoothDevice;

public class DataHolder {

    private static BluetoothDevice data;

    // May need to be non-static?
    public static BluetoothDevice getData() {
        return data;
    }

    // May need to be non-static?
    public static void setData(BluetoothDevice data) {
        DataHolder.data = data;
    }

}