package com.example.blemultimeterapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;


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

    public static List<UUID> allCharacteristicUuids = Arrays.asList(kUuidReading, kUuidMode, kUuidAdc, kUuidResistance);

    // Enums
    public enum Mode {
        kVoltage(0), kResistance(1), kDiode(2), kContinuity(3);

        static private HashMap<Integer, Mode> mapping;
        static {
            for (Mode mode : Mode.values()) {
                assert !mapping.containsKey(mode.value);
                mapping.put(mode.value, mode);
            }
        }

        final public int value;
        Mode(int value) {
            this.value = value;
        }

    }
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

        Optional<BluetoothGattService> multimeterService = Optional.empty();

        @NonNull
        @Override
        protected BleManagerGattCallback getGattCallback() {
            return new GattCallbackImpl();
        }

        private class GattCallbackImpl extends BleManagerGattCallback {
            @Override
            protected boolean isRequiredServiceSupported(@NonNull BluetoothGatt gatt) {
                multimeterService = Optional.ofNullable(gatt.getService(MultimeterService.kUuidService));
                if (!multimeterService.isPresent()) {
                    return false;
                }
                BluetoothGattService service = multimeterService.get();
                for (UUID characteristicUuid : MultimeterService.allCharacteristicUuids) {
                    if (service.getCharacteristic(characteristicUuid) == null) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            protected void initialize() {
                requestMtu(517)
                        .enqueue();

                if (!multimeterService.isPresent()) {
                    Log.wtf("MultimeterBleManager", "multimeterService missing in initialize()");
                    return;
                }
                BluetoothGattService service = multimeterService.get();
                setNotificationCallback(service.getCharacteristic(MultimeterService.kUuidReading))
                        .with((device, data) -> {
                            Integer value = data.getIntValue(Data.FORMAT_SINT32_LE, 0);
                            if (value == null) {
                                binding.deviceReading.setText("?");
                            } else {
                                binding.deviceReading.setText(((float)value / 1000) + " V");
                            }

                            Log.i("MultimeterBleManager", "Reading = " + value);
                        });
                setNotificationCallback(service.getCharacteristic(MultimeterService.kUuidAdc))
                        .with((device, data) -> {
                            Log.i("MultimeterBleManager", "ADC = " + data.getIntValue(Data.FORMAT_SINT32_LE, 0));
                        });
                setNotificationCallback(service.getCharacteristic(MultimeterService.kUuidMode))
                        .with((device, data) -> {
                            Log.i("MultimeterBleManager", "Mode = " + data.getIntValue(Data.FORMAT_UINT8, 0));
                        });
                for (UUID characteristicUuid : MultimeterService.allCharacteristicUuids) {
                    enableNotifications(service.getCharacteristic(characteristicUuid))
                            .enqueue();
                }

            }

            @Override
            protected void onServicesInvalidated() {
                multimeterService = Optional.empty();
            }
        }
    }

    @SuppressLint("MissingPermission")  // BLUETOOTH_CONNECT permission handled in ScanResultFragment
    protected void connectDevice(ScanResult result) {
        BluetoothDevice device = result.getDevice();
        binding.connectProgress.setVisibility(View.VISIBLE);
        binding.deviceName.setVisibility(View.VISIBLE);
        binding.deviceName.setText("Connecting " + device.getName());
        binding.deviceReading.setVisibility(View.INVISIBLE);

        new MultimeterBleManager(getApplicationContext()).connect(device)
                .retry(3)
                .done(cbDevice -> {
                    binding.connectProgress.setVisibility(View.INVISIBLE);
                    binding.deviceName.setText(device.getName());
                    binding.deviceReading.setText("-");
                    binding.deviceReading.setVisibility(View.VISIBLE);
                })
                .fail((cbDevice, status) -> {
                    binding.connectProgress.setVisibility(View.INVISIBLE);
                    binding.deviceName.setText("Failed " + device.getName());
                })
                .enqueue();

        Log.i("MainActivity", "Connecting GATT: " + device.getName() + " " + device.getAddress());
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
