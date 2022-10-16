package com.example.blemultimeterapp;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;
import com.google.android.material.snackbar.Snackbar;

// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends Activity {

    private TextView mTextView;
    private Button mScanButton;
    private ActivityMainBinding binding;

    private final static int kEnableBluetoothRequestCode = 1;
    private final static int kPermissionBluetoothRequestCode = 2;
    private final static int kPermissionLocationRequestCode = 3;
    private BluetoothAdapter bluetoothAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mTextView = binding.text;
        mScanButton = binding.scanButton;
        mScanButton.setOnClickListener(view -> startBleScan());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bluetoothAdapter == null) {
            bluetoothAdapter = ((BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        }
        startup();
    }

    protected void startup() {
        if (!bluetoothAdapter.isEnabled()) {
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

    protected void startBleScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.getApplicationContext(), "Location permission required for BLE scan", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, kPermissionLocationRequestCode);
            return;
        }
        Toast.makeText(this.getApplicationContext(), "Starting BLE Scan", Toast.LENGTH_SHORT)
                .show();
    }
}
