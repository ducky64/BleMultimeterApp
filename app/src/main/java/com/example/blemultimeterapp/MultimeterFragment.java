package com.example.blemultimeterapp;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.blemultimeterapp.databinding.FragmentMultimeterBinding;

import java.util.Optional;
import java.util.UUID;

import no.nordicsemi.android.ble.BleManager;
import no.nordicsemi.android.ble.data.Data;


public class MultimeterFragment extends Fragment {
    private FragmentMultimeterBinding binding;

    public final static String kFragmentArgDevice = MultimeterFragment.class.getName() + ".Device";

    private Optional<MultimeterBleManager> bleManager = Optional.empty();

    private Optional<MultimeterService.Mode> mode = Optional.empty();

    public MultimeterFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMultimeterBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        BluetoothDevice device = requireArguments().getParcelable(kFragmentArgDevice);
        connectDevice(device);

    }

    @SuppressLint("MissingPermission")  // BLUETOOTH_CONNECT permission handled prior
    protected void connectDevice(BluetoothDevice device) {
        binding.connectProgress.setVisibility(View.VISIBLE);
        binding.deviceName.setText(device.getName());
        binding.mode.setText("Connecting");
        binding.deviceReading.setVisibility(View.INVISIBLE);

        new MultimeterBleManager(getContext()).connect(device)
                .retry(3)
                .then(cbDevice -> {
                    binding.connectProgress.setVisibility(View.INVISIBLE);
                })
                .done(cbDevice -> {
                    binding.deviceReading.setText("-");
                    binding.deviceReading.setVisibility(View.VISIBLE);
                })
                .fail((cbDevice, status) -> {
                    binding.mode.setText("Failed");
                })
                .enqueue();

        Log.i("MultimeterFragment", "Connecting GATT: " + device.getName() + " " + device.getAddress());
    }

    @Override
    public void onPause() {
        super.onPause();
        bleManager.ifPresent(multimeterBleManager ->
                multimeterBleManager.disconnect().enqueue()
        );
    }

    public void onReadingUpdate(BluetoothDevice device, Data data) {
        Integer value = data.getIntValue(Data.FORMAT_SINT32_LE, 0);
        Log.i("MultimeterBleManager", "Reading = " + value);

        if (!mode.isPresent() || (
                mode.get() != MultimeterService.Mode.kVoltage
                && mode.get() != MultimeterService.Mode.kDiode)) {
            return;
        }

        if (value == null) {
            binding.deviceReading.setText("?");
        } else {
            binding.deviceReading.setText(((float)value / 1000) + " V");
        }
    }

    public void onResistanceUpdate(BluetoothDevice device, Data data) {
        Integer value = data.getIntValue(Data.FORMAT_UINT32_LE, 0);
        Log.i("MultimeterBleManager", "Resistance = " + value);

        if (!mode.isPresent() || (
                mode.get() != MultimeterService.Mode.kResistance
                        && mode.get() != MultimeterService.Mode.kContinuity)) {
            return;
        }

        if (value == null) {
            binding.deviceReading.setText("?");
        } else {
            binding.deviceReading.setText(((float)value / 1000) + " Ω");
        }
    }

    public void onModeUpdate(BluetoothDevice device, Data data) {
        Integer value = data.getIntValue(Data.FORMAT_UINT8, 0);
        Log.i("MultimeterBleManager", "Mode = " + value);

        if (value == null) {
            mode = Optional.empty();
            binding.mode.setText("?");
        } else {
            Optional<MultimeterService.Mode> mode = MultimeterService.Mode.fromValue(value);
            this.mode = mode;
            binding.mode.setText(mode.toString());
        }
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
                requestMtu(BleConstants.kGattMaxMtuSize)
                        .enqueue();

                if (!multimeterService.isPresent()) {
                    Log.wtf("MultimeterBleManager", "multimeterService missing in initialize()");
                    return;
                }
                BluetoothGattService service = multimeterService.get();
                setNotificationCallback(service.getCharacteristic(MultimeterService.kUuidReading))
                        .with(MultimeterFragment.this::onReadingUpdate);
                setNotificationCallback(service.getCharacteristic(MultimeterService.kUuidResistance))
                        .with(MultimeterFragment.this::onResistanceUpdate);

                // mode isn't constantly updated, so kick off the first one
                readCharacteristic(service.getCharacteristic(MultimeterService.kUuidMode))
                        .with(MultimeterFragment.this::onModeUpdate)
                        .enqueue();
                setNotificationCallback(service.getCharacteristic(MultimeterService.kUuidMode))
                        .with(MultimeterFragment.this::onModeUpdate);

                for (UUID characteristicUuid : MultimeterService.allCharacteristicUuids) {
                    enableNotifications(service.getCharacteristic(characteristicUuid))
                            .enqueue();
                }
            }

            @Override
            protected void onServicesInvalidated() {
                Log.i("MultimeterBleManager", "Services invalidated");
                multimeterService = Optional.empty();
                binding.mode.setText("Disconnected");
            }
        }
    }

}
