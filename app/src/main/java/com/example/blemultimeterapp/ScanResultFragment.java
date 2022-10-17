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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.wear.widget.WearableLinearLayoutManager;

import com.example.blemultimeterapp.databinding.FragmentScanResultBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;


public class ScanResultFragment extends Fragment {
    private FragmentScanResultBinding binding;

    private final static int kEnableBluetoothRequestCode = 1;
    private final static int kPermissionBluetoothRequestCode = 2;
    private final static int kPermissionLocationRequestCode = 3;

    interface ResultSelectAction {
        void action(ScanResult result);
    }

    public ScanResultFragment(ResultSelectAction action) {
        super();
        scanResultAdapter = new ScanResultAdapter(scanResults, result -> {
            getParentFragmentManager().beginTransaction()
                    .remove(this)
                    .commit();
            action.action(result);
        });
    }

    private Optional<BluetoothLeScanner> btScanner = Optional.empty();

    final private List<ScanResult> scanResults = new ArrayList<>();
    final private ScanResultAdapter scanResultAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentScanResultBinding.inflate(inflater, container, false);

        binding.scanResults.setEdgeItemsCenteringEnabled(true);
        binding.scanResults.setCircularScrollingGestureEnabled(true);
        binding.scanResults.setLayoutManager(new WearableLinearLayoutManager(this.getActivity()));
        binding.scanResults.setAdapter(scanResultAdapter);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        startBleScan();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopBleScan();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
        Activity context = this.getActivity();

        BluetoothAdapter btAdapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
        if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent((BluetoothAdapter.ACTION_REQUEST_ENABLE));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this.getContext(), "Bluetooth permissions required", Toast.LENGTH_SHORT)
                        .show();
                ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, kPermissionBluetoothRequestCode);
                return;
            }
            startActivityForResult(enableBtIntent, kEnableBluetoothRequestCode);
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.getContext(), "Location permission required for BLE scan", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, kPermissionLocationRequestCode);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this.getContext(), "BLE scan permission required", Toast.LENGTH_SHORT)
                    .show();
            ActivityCompat.requestPermissions(context, new String[]{Manifest.permission.BLUETOOTH_SCAN}, kPermissionLocationRequestCode);
            return;
        }

        if (btScanner.isPresent()) {
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

    protected void stopBleScan() {
        if (btScanner.isPresent()) {
            btScanner.get().stopScan(scanCallback);
            btScanner = Optional.empty();
            binding.scanResultCount.setText("Found: " + scanResults.size());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == kPermissionBluetoothRequestCode) {
            startBleScan();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == kEnableBluetoothRequestCode) {
            startBleScan();
        } else if (requestCode == kPermissionLocationRequestCode) {
            startBleScan();
        }
    }
}
