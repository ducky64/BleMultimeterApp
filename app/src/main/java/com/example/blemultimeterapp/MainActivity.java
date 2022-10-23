package com.example.blemultimeterapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;

import java.util.Optional;
import java.util.UUID;


class MultimeterService {
    public static ParcelUuid applyUuidOffset(ParcelUuid uuid, int offset) {
        long msb = uuid.getUuid().getMostSignificantBits();
        long lsb = uuid.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb + ((long)offset << 32) , lsb));
    }

    public static ParcelUuid kUuidService = ParcelUuid.fromString("0bde0a01-cdb0-46bd-a3a2-85b7e16329d1");  // custom protocol

    // Characteristics
    public static ParcelUuid kUuidReading = applyUuidOffset(kUuidService, 1);
    public static ParcelUuid kUuidMode = applyUuidOffset(kUuidService, 2);
    public static ParcelUuid kUuidAdc = applyUuidOffset(kUuidService, 3);
    public static ParcelUuid kUuidResistance = applyUuidOffset(kUuidService, 4);
}


// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends FragmentActivity {
    private ActivityMainBinding binding;

    private Optional<BluetoothGatt> connectedGatt = Optional.empty();

    public static int kGattMaxMtuSize = 517;  // defined in Android source, https://punchthrough.com/android-ble-guide/

    public static ParcelUuid kUuidNus = ParcelUuid.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static ParcelUuid kUuidDeviceInfo = ParcelUuid.fromString("0000180a-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.connectProgress.setVisibility(View.INVISIBLE);
        binding.deviceName.setVisibility(View.INVISIBLE);
        binding.deviceReading.setVisibility(View.INVISIBLE);

        binding.scanButton.setOnClickListener(view -> {
            ScanResultFragment scanResultFragment = new ScanResultFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainerView, scanResultFragment)
                    .commit();
            getSupportFragmentManager().setFragmentResultListener(ScanResultFragment.kFragmentResultKey, this, (requestKey, result) -> {
                connectDevice(result.getParcelable(ScanResultFragment.kFragmentResultBundleScanResult));
            });
        });
    }

    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        @SuppressLint("MissingPermission")  // BLUETOOTH_CONNECT permission handled in ScanResultFragment
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.i("BluetoothGattCallback", "State change -> " + newState + " status=" + status);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedGatt = Optional.of(gatt);

                runOnUiThread(() -> {
                    binding.connectProgress.setVisibility(View.INVISIBLE);
                    binding.deviceName.setText(gatt.getDevice().getName());
                    binding.deviceReading.setText("-");  // initialize blank reading
                    binding.deviceReading.setVisibility(View.VISIBLE);
                });

                // running on UI thread recommended to avoid potential deadlock: https://punchthrough.com/android-ble-guide/
                runOnUiThread(() -> {
                    gatt.discoverServices();
                });

            } else {
                gatt.close();
                connectedGatt = Optional.empty();

                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this.getApplicationContext(),
                                    "Unexpected connection state change -> " + newState + ", status=" + status,
                                    Toast.LENGTH_SHORT)
                            .show();
                    binding.connectProgress.setVisibility(View.INVISIBLE);
                    binding.deviceName.setVisibility(View.INVISIBLE);
                    binding.deviceReading.setVisibility(View.INVISIBLE);
                });
            }
        }

        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            super.onMtuChanged(gatt, mtu, status);
            Log.i("BluetoothGattCallback", "MTU changed -> " + mtu);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            Log.i("BluetoothGattCallback", "Expected VRead " + MultimeterService.kUuidReading);

            for (BluetoothGattService service : gatt.getServices()) {
                Log.i("BluetoothGattCallback", "Discovered service " + service.getUuid());
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    Log.i("BluetoothGattCallback", "  with characteristic " + characteristic.getUuid());
                }

            }
        }
    };

    @SuppressLint("MissingPermission")  // BLUETOOTH_CONNECT permission handled in ScanResultFragment
    protected void connectDevice(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        binding.connectProgress.setVisibility(View.VISIBLE);
        binding.deviceName.setVisibility(View.VISIBLE);
        binding.deviceName.setText("Connecting " + device.getName());
        binding.deviceReading.setVisibility(View.INVISIBLE);

        device.connectGatt(getApplicationContext(), false, gattCallback);
        Log.i("MainActivity", "Connecting GATT: " + device.getName() + " " + device.getAddress());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
