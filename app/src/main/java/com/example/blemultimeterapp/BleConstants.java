package com.example.blemultimeterapp;

import java.util.UUID;

public class BleConstants {
    public static int kGattMaxMtuSize = 517;  // defined in Android source, https://punchthrough.com/android-ble-guide/

    public static UUID kUuidDeviceInfo = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    public static UUID kUuidNus = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");  // Nordic UART
}
