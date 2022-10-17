package com.example.blemultimeterapp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentResultListener;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;


// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends FragmentActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());

        com.example.blemultimeterapp.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.connectProgress.setVisibility(View.INVISIBLE);
//        binding.deviceName.setVisibility(View.INVISIBLE);
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

    protected void connectDevice(ScanResult result) {
        BluetoothDevice device = result.getDevice();
//        new Handler(Looper.getMainLooper()).post(() -> {
        runOnUiThread(() -> {
//            binding.connectProgress.setVisibility(View.VISIBLE);
//            binding.connectProgress.requestLayout();
//            binding.deviceName.setVisibility(View.VISIBLE);
            binding.deviceName.setText(device.getName());
//            binding.deviceName.requestLayout();
//            binding.deviceReading.setVisibility(View.INVISIBLE);

            Toast.makeText(this, device.getName(), Toast.LENGTH_SHORT)
                    .show();
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
