package com.studiox.taskit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private static final String PREF_USER_LOGGED_IN = "user_logged_in";
    private static final String PREF_USER_CHOSE_SKIP_LOGIN = "user_chose_skip_login";
    FirebaseAuth mAuth;
    LinearLayout siguppage, continuewithoutsigin;
    TextView loginpage;

    @Override
    public void onStart() {
        super.onStart();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isUserLoggedIn = preferences.getBoolean(PREF_USER_LOGGED_IN, false);
        boolean userChoseSkipLogin = preferences.getBoolean(PREF_USER_CHOSE_SKIP_LOGIN, false);

        if (isUserLoggedIn || userChoseSkipLogin) {
            navigateToHomepage();
        } else {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null && currentUser.isEmailVerified()) {
                navigateToHomepage();
            } else {
                // If not logged in, stay on the login screen
                // Display login and signup options (already handled in onCreate)
            }
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        siguppage = findViewById(R.id.signuppagebutton);
        loginpage = findViewById(R.id.loginpagebutton);
        continuewithoutsigin = findViewById(R.id.continuewithoutsigninbutton);


        siguppage.setOnClickListener(v -> {
            SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                performHapticFeedback();
            }

            Intent intent = new Intent(MainActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        loginpage.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        continuewithoutsigin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREF_USER_LOGGED_IN, false);
                editor.putBoolean(PREF_USER_CHOSE_SKIP_LOGIN, true);
                editor.apply();
                navigateToHomepage();
            }
        });
    }

    private void navigateToHomepage() {
        Intent intent = new Intent(MainActivity.this, TaskActivity.class);
        startActivity(intent);
        finish();
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


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }
}
