package com.example.blemultimeterapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;


class MultimeterService {
    public static UUID applyUuidOffset(UUID uuid, int offset) {
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        return new UUID(msb + ((long)offset << 32) , lsb);
    }

    public static UUID kUuidService = UUID.fromString("0bde0a01-cdb0-46bd-a3a2-85b7e16329d1");  // custom protocol

    // Characteristics
    public static UUID kUuidReading = applyUuidOffset(kUuidService, 1);
    public static UUID kUuidMode = applyUuidOffset(kUuidService, 2);
    public static UUID kUuidAdc = applyUuidOffset(kUuidService, 3);
    public static UUID kUuidResistance = applyUuidOffset(kUuidService, 4);
}


// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends FragmentActivity {
    private ActivityMainBinding binding;

    private Optional<BluetoothGatt> connectedGatt = Optional.empty();

    public static int kGattMaxMtuSize = 517;  // defined in Android source, https://punchthrough.com/android-ble-guide/

    public static UUID kUuidCccDescriptor = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static UUID kUuidNus = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
    public static UUID kUuidDeviceInfo = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb");

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

    // use Nordic's BLE library since Android's library is full of wtf
    class MultimeterBleManager extends BleManager {
        public MultimeterBleManager(@NonNull Context context) {
            super(context);
        }

        @NonNull
        @Override
        protected BleManagerGattCallback getGattCallback() {
            return new GattCallbackImpl();
        }

        private class GattCallbackImpl extends BleManagerGattCallback {
            @Override
            protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
                return false;
            }

            @Override
            protected void initialize() {

            }

            @Override
            protected void onServicesInvalidated() {

            }
        }
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
                    binding.deviceName.setText(gatt.getDevice().getName());
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
        @SuppressLint("MissingPermission")  // BLUETOOTH_CONNECT permission handled in ScanResultFragment
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            for (BluetoothGattService service : gatt.getServices()) {
                Log.i("BluetoothGattCallback", "Discovered service " + service.getUuid());
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    Log.i("BluetoothGattCallback", "  with characteristic " + characteristic.getUuid());
                }
            }
            BluetoothGattService multimeterService = gatt.getService(MultimeterService.kUuidService);
            if (multimeterService == null) {
                Log.e("BluetoothGattCallback", "Missing GATT service");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this.getApplicationContext(),
                                    "Missing GATT service", Toast.LENGTH_SHORT)
                            .show();
                });
                return;
            }
            BluetoothGattCharacteristic readingChar = multimeterService.getCharacteristic(MultimeterService.kUuidReading);
            BluetoothGattCharacteristic modeChar = multimeterService.getCharacteristic(MultimeterService.kUuidMode);
            BluetoothGattCharacteristic adcChar = multimeterService.getCharacteristic(MultimeterService.kUuidAdc);
            BluetoothGattCharacteristic resistanceChar = multimeterService.getCharacteristic(MultimeterService.kUuidResistance);
            if (readingChar == null || modeChar == null || adcChar == null || resistanceChar == null) {
                Log.e("BluetoothGattCallback", "Missing GATT characteristics");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this.getApplicationContext(),
                                    "Missing GATT characteristics", Toast.LENGTH_SHORT)
                            .show();
                });
                return;
            }

            if (!gatt.setCharacteristicNotification(readingChar, true)
                    || !gatt.setCharacteristicNotification(adcChar, true)
                    || !gatt.setCharacteristicNotification(modeChar, true)
                    || !gatt.setCharacteristicNotification(resistanceChar, true)) {
                Log.e("BluetoothGattCallback", "Can't set notifications");
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this.getApplicationContext(),
                                    "Can't set notifications", Toast.LENGTH_SHORT)
                            .show();
                });
                return;
            }
            BluetoothGattDescriptor descriptor;
            descriptor = readingChar.getDescriptor(kUuidCccDescriptor);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            descriptor = adcChar.getDescriptor(kUuidCccDescriptor);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            descriptor = modeChar.getDescriptor(kUuidCccDescriptor);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            gatt.writeDescriptor(descriptor);

            runOnUiThread(() -> {
                binding.connectProgress.setVisibility(View.INVISIBLE);
                binding.deviceReading.setText("-");  // initialize blank reading
                binding.deviceReading.setVisibility(View.VISIBLE);
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("BluetoothGattCallback", "Characteristic changed " + characteristic.getUuid() + " -> " + Arrays.toString(characteristic.getValue()));
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
