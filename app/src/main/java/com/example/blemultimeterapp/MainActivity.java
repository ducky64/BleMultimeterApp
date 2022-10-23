package com.example.blemultimeterapp;

import android.bluetooth.le.ScanResult;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;


// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends FragmentActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.scanButton.setOnClickListener(view -> {
            ScanResultFragment scanResultFragment = new ScanResultFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainerView, scanResultFragment)
                    .commit();

            getSupportFragmentManager().setFragmentResultListener(ScanResultFragment.kFragmentResultKey, this, (requestKey, result) -> {
                ScanResult scanResult = result.getParcelable(ScanResultFragment.kFragmentResultBundleScanResult);

                Bundle args = new Bundle();
                args.putParcelable(MultimeterFragment.kFragmentArgDevice, scanResult.getDevice());

                MultimeterFragment multimeterFragment = new MultimeterFragment();
                multimeterFragment.setArguments(args);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragmentContainerView, multimeterFragment)
                        .commit();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
