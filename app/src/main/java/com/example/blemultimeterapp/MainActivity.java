package com.example.blemultimeterapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;


// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends Activity {

    private ActivityMainBinding binding;

    private final static int kEnableBluetoothRequestCode = 1;
    private final static int kPermissionBluetoothRequestCode = 2;
    private final static int kPermissionLocationRequestCode = 3;
    private BluetoothAdapter btAdapter = null;
    private Optional<BluetoothLeScanner> btScanner = Optional.empty();

    private List<ScanResult> scanResults = new ArrayList<>();
    private ScanResultAdapter scanResultAdapter = new ScanResultAdapter(scanResults, result -> {
        Toast.makeText(MainActivity.this.getApplicationContext(), result.getDevice().getName(), Toast.LENGTH_SHORT)
                .show();
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.scanResults.setEdgeItemsCenteringEnabled(true);
        binding.scanResults.setCircularScrollingGestureEnabled(true);
        binding.scanResults.setLayoutManager(new WearableLinearLayoutManager(this));
        binding.scanResults.setAdapter(scanResultAdapter);

        binding.scanButton.setOnClickListener(view -> startBleScan());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (btAdapter == null) {
            btAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        }
        startup();
    }

    protected void startup() {
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent((BluetoothAdapter.ACTION_REQUEST_ENABLE));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this.getApplicationContext(), "Bluetooth permissions required", Toast.LENGTH_SHORT)
                        .show();
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, kPermissionBluetoothRequestCode);
                return;
            }
            startActivityForResult(enableBtIntent, kEnableBluetoothRequestCode);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == kPermissionBluetoothRequestCode) {
            startup();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == kEnableBluetoothRequestCode) {
            startup();
        } else if (requestCode == kPermissionLocationRequestCode) {
            startBleScan();
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            OptionalInt index = IntStream.range(0, scanResults.size())
                    .filter(i -> scanResults.get(i).getDevice().getAddress().equals(result.getDevice().getAddress()))
                    .findFirst();
            if (index.isPresent()) {  // update existing device
                scanResults.set(index.getAsInt(), result);
                scanResultAdapter.notifyItemChanged(index.getAsInt());
            } else {  // add new device
                scanResults.add(result);
                scanResultAdapter.notifyItemInserted(scanResults.size() - 1);
                binding.scanResultCount.setText("Scanning: " + scanResults.size());
            }
        }
    };

    protected void startBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.getApplicationContext(), "Location permission required for BLE scan", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, kPermissionLocationRequestCode);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.getApplicationContext(), "BLE scan permission required", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, kPermissionLocationRequestCode);
            return;
        }

        if (btScanner.isPresent()) {
            btScanner.get().stopScan(scanCallback);
            btScanner = Optional.empty();
            binding.scanResultCount.setText("Found: " + scanResults.size());
            return;
        }

        binding.scanResultCount.setText("Scanning");
        BluetoothLeScanner scanner = btAdapter.getBluetoothLeScanner();
        btScanner = Optional.of(scanner);
        ScanFilter filter = new ScanFilter.Builder()
//                .setServiceUuid(ParcelUuid.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E"))
                .setDeviceName("DuckyMultimeter")
                .build();
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        scanResults.clear();
        scanResultAdapter.notifyDataSetChanged();
        scanner.startScan(Arrays.asList(filter), settings, scanCallback);

    }
}
