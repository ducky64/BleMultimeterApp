package com.example.blemultimeterapp;

import android.app.Activity;
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
                    .replace(R.id.frameLayout, scanResultFragment)
                    .commit();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
