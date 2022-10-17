package com.example.blemultimeterapp;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentResultListener;

import com.example.blemultimeterapp.databinding.ActivityMainBinding;


// from this tutorial: https://punchthrough.com/android-ble-guide/
public class MainActivity extends FragmentActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        com.example.blemultimeterapp.databinding.ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.scanButton.setOnClickListener(view -> {
            ScanResultFragment scanResultFragment = new ScanResultFragment(result -> {
                Toast.makeText(this, result.getDevice().getName(), Toast.LENGTH_SHORT)
                        .show();
            });
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.root, scanResultFragment)
                    .commit();
        });
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
