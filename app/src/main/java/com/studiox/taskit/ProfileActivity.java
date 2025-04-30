package com.studiox.taskit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ShortcutManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseUserMetadata;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "TaskActivity";
    private static final String PREF_USER_CHOSE_SKIP_LOGIN = "user_chose_skip_login";
    ImageView profileImageView, settingButton;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    TextView nametextview, emailtextview, pendingTaskCount, completedTaskCount, currentTaskCount, allTaskCount, joinedDateTextView;
    LinearLayout profilebackbutton, signout, mainpage, historyPageButton;
    DatabaseHelper databaseHelper;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            signout.setVisibility(View.VISIBLE);
            mainpage.setVisibility(View.GONE);
        } else {
            signout.setVisibility(View.GONE);
            mainpage.setVisibility(View.VISIBLE);
        }
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    @SuppressLint({"SetTextI18n", "UseCompatLoadingForDrawables"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_profile);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        pendingTaskCount = findViewById(R.id.pendingTaskCount);
        currentTaskCount = findViewById(R.id.currentTaskCount);
        completedTaskCount = findViewById(R.id.completedTaskCount);
        allTaskCount = findViewById(R.id.allTaskCount);
        historyPageButton = findViewById(R.id.historyButton);
        settingButton = findViewById(R.id.settingsPageButton);
        profilebackbutton = findViewById(R.id.profilepagebackbutton);
        profileImageView = findViewById(R.id.profile_view);
        nametextview = findViewById(R.id.name_textview);
        emailtextview = findViewById(R.id.email_textview);
        mainpage = findViewById(R.id.gotomainpagebutton);
        signout = findViewById(R.id.signoutbutton);
        joinedDateTextView = findViewById(R.id.joined_date_textview);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        databaseHelper = new DatabaseHelper(this);

        fetchTaskCounts();


        historyPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileActivity.this, HistoryActivity.class);
                startActivity(intent);
                finish();
            }
        });

        settingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(ProfileActivity.this, SettingsActivity.class);
                startActivity(intent);
                finish();
            }
        });

        profilebackbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(ProfileActivity.this, TaskActivity.class);
                startActivity(intent);
                finish();
            }
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseUserMetadata metadata = user.getMetadata();
            if (metadata != null) {
                long creationTimestamp = metadata.getCreationTimestamp();
                Date joinDate = new Date(creationTimestamp);
                SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                String formattedDate = sdf.format(joinDate);
                joinedDateTextView.setText("Joined on : " + formattedDate);
            }
        }


        if (currentUser != null && !isNetworkAvailable()) {
            showThemedSnackbar(findViewById(android.R.id.content), "No internet connection!");
        }

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnCompleteListener(task -> {
                if (!isFinishing() && !isDestroyed()) { // Ensure Activity is still valid
                    if (task.isSuccessful() && task.getResult() != null) {
                        String profilePhotoUrl = task.getResult().getString("profileUrl");

                        if (profilePhotoUrl != null && !profilePhotoUrl.equals("N/A")) {
                            Glide.with(ProfileActivity.this)
                                    .load(profilePhotoUrl)
                                    .transform(new CircleCrop())
                                    .placeholder(R.drawable.profile_icon) // Shown while loading
                                    .error(R.drawable.profile_icon) // Shown if URL fails
                                    .skipMemoryCache(false) // Keep in memory cache
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // Use cache even when offline
                                    .into(profileImageView);
                        } else {
                            profileImageView.setImageDrawable(getDrawable(R.drawable.profile_icon));
                            Log.w(TAG, "Profile photo URL is missing or invalid");
                        }
                    } else {
                        profileImageView.setImageDrawable(getDrawable(R.drawable.profile_icon));
                        Log.e(TAG, "Failed to load profile photo", task.getException());
                    }
                }
            });
        } else {
            profileImageView.setImageDrawable(getDrawable(R.drawable.profile_icon));
            Log.w(TAG, "User is not logged in");
        }

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String username = task.getResult().getString("name");
                    String email = task.getResult().getString("email");

                    if (username != null && !username.isEmpty()) {
                        nametextview.setText(username);
                    } else {
                        nametextview.setText("Name");
                    }

                    if (email != null && !email.isEmpty()) {
                        emailtextview.setText(email);
                    } else {
                        emailtextview.setText("Email-ID");
                    }
                } else {
                    Log.e(TAG, "Failed to fetch username & email", task.getException());
                    nametextview.setText("Name");
                    emailtextview.setText("Email-ID");
                }
            });
        } else {
            Log.w(TAG, "User is not logged in");
            nametextview.setText("Name");
            emailtextview.setText("Email-ID");
        }

        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions);

        signout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                TaskActivity.isLoggingOutManually = true;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                    shortcutManager.removeDynamicShortcuts(Collections.singletonList("Add Task"));
                }

                FirebaseAuth.getInstance().signOut();
                mAuth.signOut();
                googleSignInClient.signOut();
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
                Toast.makeText(ProfileActivity.this, "Signout successfully!", Toast.LENGTH_SHORT).show();
            }
        });

        mainpage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                databaseHelper.clearDatabase();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                    shortcutManager.removeDynamicShortcuts(Collections.singletonList("Add Task"));
                }


                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(ProfileActivity.this);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(PREF_USER_CHOSE_SKIP_LOGIN, false);
                editor.apply();
                Intent intent = new Intent(ProfileActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
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

    private void fetchTaskCounts() {
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (currentUser == null) {
            fetchOfflineTaskCounts();
            return;
        }

        String userId = currentUser.getUid();
        db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int completedCount = 0, pendingCount = 0, allCount = 0, currentCount = 0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        boolean isCompleted = doc.getBoolean("completed") != null && doc.getBoolean("completed");
                        String taskDate = doc.getString("date");

                        allCount++;
                        if (isCompleted) {
                            completedCount++;
                        } else {
                            pendingCount++;

                            if (taskDate != null) {
                                try {
                                    SimpleDateFormat fullFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
                                    Date taskParsedDate = fullFormat.parse(taskDate.split(",")[0]);

                                    Calendar taskCal = Calendar.getInstance();
                                    taskCal.setTime(taskParsedDate);

                                    Calendar todayCal = Calendar.getInstance();

                                    if (taskCal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                                            taskCal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)) {
                                        currentCount++;
                                    }

                                } catch (Exception e) {
                                    Log.e(TAG, "Date parsing failed: " + e.getMessage());
                                }
                            }
                        }
                    }

                    updateTaskCountUI(pendingCount, completedCount, allCount, currentCount);
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Failed to load task statistics!", Toast.LENGTH_SHORT).show());
    }


    // Update UI with fetched task counts
    private void updateTaskCountUI(int pending, int completed, int all, int current) {
        pendingTaskCount.setText(String.valueOf(pending));
        completedTaskCount.setText(String.valueOf(completed));
        allTaskCount.setText(String.valueOf(all));
        currentTaskCount.setText(String.valueOf(current));
    }

    // Fallback for offline task counts from SQLite
    private void fetchOfflineTaskCounts() {
        int pendingCount = databaseHelper.getTaskCountByStatus(false);
        int completedCount = databaseHelper.getTaskCountByStatus(true);
        int allCount = databaseHelper.getTotalTaskCount();
        int currentCount = databaseHelper.getCurrentTaskCount();

        updateTaskCountUI(pendingCount, completedCount, allCount, currentCount);
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();

    }

    private void showThemedSnackbar(View view, String message) {
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);

        View snackbarView = snackbar.getView();
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) snackbarView.getLayoutParams();
        params.setMargins(70, 0, 70, 50);

        snackbarView.setLayoutParams(params);
        snackbar.show();
    }


    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(ProfileActivity.this, TaskActivity.class);
        startActivity(intent);
        finish();
    }

}