package com.studiox.taskit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

public class SettingsActivity extends AppCompatActivity {

    LinearLayout backButton, appThemeButton, feebackbtn, aboutbtn, batterysaving;
    MaterialSwitch hapticFeedback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backButton = findViewById(R.id.backButton);
        appThemeButton = findViewById(R.id.appThemeLayout);
        hapticFeedback = findViewById(R.id.materialSwitchHapticFeedback);
        feebackbtn = findViewById(R.id.feedbackbutton);
        aboutbtn = findViewById(R.id.aboutbutton);
        batterysaving = findViewById(R.id.batterysaving);


        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(SettingsActivity.this, TaskActivity.class);
                startActivity(intent);
                finish();
            }
        });

        appThemeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRadioButtonDialog();
            }
        });

        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isHapticEnabled = sharedPreferences.getBoolean("haptic_feedback", true);

        hapticFeedback.setChecked(isHapticEnabled);
        hapticFeedback.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean("haptic_feedback", isChecked);
                editor.apply();

                if (isChecked) {
                    performHapticFeedback();
                    Toast.makeText(SettingsActivity.this, "Haptic Feedback Enabled!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SettingsActivity.this, "Haptic Feedback Disabled!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        feebackbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://forms.gle/cHNdp4vCCxFHgqZK6"));
                startActivity(intent);
            }
        });

        aboutbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
                finish();
            }
        });

        batterysaving.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                } else {
                    batterysaving.setVisibility(View.GONE);
                }
            }
        });

    }

    private void performHapticFeedback() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(50);
            }
        }
    }


    private void showRadioButtonDialog() {
        String[] options = {"System Default", "Light", "Dark"};

        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int selectedTheme = sharedPreferences.getInt("theme", 0); // Default to System Default (0)

        new MaterialAlertDialogBuilder(this)
                .setTitle("App Theme")
                .setSingleChoiceItems(options, selectedTheme, (dialog, which) -> {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putInt("theme", which);
                    editor.apply();
                    applyTheme(which);

                    dialog.dismiss();
                })
                .show();
    }

    private void applyTheme(int themeOption) {
        switch (themeOption) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }

        recreate();
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(SettingsActivity.this, TaskActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            batterysaving.setVisibility(View.GONE);
        }
    }

}