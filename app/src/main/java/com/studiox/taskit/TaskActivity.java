package com.studiox.taskit;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.TaskItAdapter.TaskItAdapter;
import com.studiox.taskit.TaskItModel.TaskItModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Locale;

public class TaskActivity extends AppCompatActivity {

    public static final android.os.Handler userStatusHandler = new android.os.Handler();
    private static final String TAG = "TaskActivity";
    private static final int USER_CHECK_INTERVAL_ONLINE = 30 * 1000; // 30 seconds
    private static final int USER_CHECK_INTERVAL_OFFLINE = 60 * 1000; // 1 minute
    public static boolean isLoggingOutManually = false;
    public static boolean hasLoggedOutAutomatically = false;
    private final Runnable checkUserRunnable = new Runnable() {
        @Override
        public void run() {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user != null) {
                if (isNetworkAvailable()) {
                    user.reload().addOnCompleteListener(task -> {
                        FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();

                        if (task.isSuccessful() && refreshedUser != null) {
                            // User still valid, check again later
                            userStatusHandler.postDelayed(this, USER_CHECK_INTERVAL_ONLINE);
                        } else {
                            // Only logout if reload fails AND user is null
                            if (refreshedUser == null &&
                                    !TaskActivity.isLoggingOutManually &&
                                    !TaskActivity.hasLoggedOutAutomatically) {
                                logoutUser();
                            } else {
                                // Temporary network issue, try again later
                                userStatusHandler.postDelayed(this, USER_CHECK_INTERVAL_ONLINE);
                            }
                        }
                    });
                } else {
                    // No internet — skip reload and check again after longer time
                    Log.w(TAG, "No internet — skipping reload");
                    userStatusHandler.postDelayed(this, USER_CHECK_INTERVAL_OFFLINE);
                }
            }
        }
    };
    NavigationView navigationView;
    DrawerLayout drawerLayout;
    ImageView profileImageView, noTaskFoundImageView, drawerImageView;
    FloatingActionButton addTaskPageButton;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    FirebaseUser currentUser;
    RecyclerView recyclerView;
    DatabaseHelper databaseHelper;
    TaskItAdapter adapter;
    EditText searchEditText;
    TextView noTaskFoundTextView, taskHeaderTextView;
    SwipeRefreshLayout swipeRefreshLayout;
    ChipGroup chipGroup;
    Chip allChip, pendingChip, currentChip, completedChip;
    Cursor cursor;
    ArrayList<TaskItModel> taskList, allTasks, pendingTasks, todayTasks, completedTasks;

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (isNetworkAvailable()) {
                user.reload().addOnCompleteListener(task -> {
                    FirebaseUser refreshedUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (!task.isSuccessful() || refreshedUser == null) {
                        FirebaseAuth.getInstance().signOut();
                        TaskActivity.hasLoggedOutAutomatically = true;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
                            shortcutManager.removeDynamicShortcuts(Collections.singletonList("Add Task"));
                        }

                        Toast.makeText(this, "Your account no longer exists. Please log in again.", Toast.LENGTH_LONG).show();

                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }
                });
            } else {
                Log.d(TAG, "No internet, skipping user.reload()");
            }
        }
    }

    @SuppressLint("WrongThread")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        recyclerView = findViewById(R.id.taskRecyclerView);
        noTaskFoundImageView = findViewById(R.id.noTaskFoundImageView);
        noTaskFoundTextView = findViewById(R.id.noTaskFoundTextView);
        taskHeaderTextView = findViewById(R.id.taskHeaderTextView);
        chipGroup = findViewById(R.id.chipGroup);
        allChip = findViewById(R.id.allChip);
        pendingChip = findViewById(R.id.pendingChip);
        completedChip = findViewById(R.id.completedChip);
        currentChip = findViewById(R.id.currentChip);
        addTaskPageButton = findViewById(R.id.addTaskButton);
        drawerLayout = findViewById(R.id.main);
        drawerImageView = findViewById(R.id.drawerImageView);
        navigationView = findViewById(R.id.nav_view);
        profileImageView = findViewById(R.id.profile_view);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        searchEditText = findViewById(R.id.searchEditText);
        databaseHelper = new DatabaseHelper(this);

        taskList = new ArrayList<>();

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();


        adapter = new TaskItAdapter(this, new ArrayList<>(), noTaskFoundTextView, noTaskFoundImageView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Intent intent = getIntent();
        String repeat = intent.getStringExtra("repeat");

        userStatusHandler.postDelayed(checkUserRunnable, USER_CHECK_INTERVAL_ONLINE);
        userStatusHandler.postDelayed(checkUserRunnable, USER_CHECK_INTERVAL_OFFLINE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            ShortcutInfo shortcut = new ShortcutInfo.Builder(this, "Add Task")
                    .setShortLabel("Add Task")
                    .setLongLabel("Add Task")
                    .setIcon(Icon.createWithResource(this, R.drawable.add_task_icon))
                    .setIntent(new Intent(this, AddTaskActivity.class)
                            .setAction(Intent.ACTION_VIEW))
                    .build();

            shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcut));
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent exactIntent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                startActivity(exactIntent);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1);
        }

        SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int theme = sharedPreferences.getInt("theme", 0);
        applyTheme(theme);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filterList(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        chipGroup.setSingleSelection(true); // ensures only one chip is selected
        chipGroup.setSelectionRequired(true); // prevents deselecting the last one

        int selectedColor = Color.parseColor("#4f378b");
        int unselectedColor = Color.TRANSPARENT;
        int strokeColor = ContextCompat.getColor(this, R.color.chipStrokeColor);
        int chipTextColor = ContextCompat.getColor(this, R.color.chipTextColor);
        float strokeWidth = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 0.2f, getResources().getDisplayMetrics()
        );

        // We’ll store the currently selected chip
        final Chip[] currentlySelectedChip = {null};

        // Attach listener AFTER setting default selected chip
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);

            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Update styling
                    chip.setChipBackgroundColor(ColorStateList.valueOf(selectedColor));
                    chip.setTextColor(Color.WHITE);
                    chip.setChipStrokeWidth(0);

                    // Unstyle the previous chip (if not the same one)
                    if (currentlySelectedChip[0] != null && currentlySelectedChip[0] != chip) {
                        Chip prevChip = currentlySelectedChip[0];
                        prevChip.setChipBackgroundColor(ColorStateList.valueOf(unselectedColor));
                        prevChip.setChipStrokeColor(ColorStateList.valueOf(strokeColor));
                        prevChip.setChipStrokeWidth(strokeWidth);
                        prevChip.setTextColor(chipTextColor);
                    }

                    currentlySelectedChip[0] = chip; // update current
                }
            });
        }

        // Set one chip as default selected
        allChip.setChecked(true);
        currentlySelectedChip[0] = allChip;
        allChip.setChipBackgroundColor(ColorStateList.valueOf(selectedColor));
        allChip.setChipStrokeWidth(0);
        allChip.setTextColor(Color.WHITE);


        allChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                taskHeaderTextView.setText("All Task's :");

                fetchFilteredTasks("all");

                cursor = databaseHelper.showData();
                allTasks = new ArrayList<>();

                while (cursor.moveToNext()) {
                    int completedIndex = cursor.getColumnIndex("completed");
                    boolean isCompleted = (completedIndex != -1) && (cursor.getInt(completedIndex) == 1);


                    allTasks.add(new TaskItModel(
                            cursor.getInt(0),
                            "",
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            isCompleted,
                            false,
                            repeat
                    ));
                }

                adapter.updateTaskList(allTasks);

                if (allTasks.isEmpty()) {
                    noTaskFoundImageView.setVisibility(View.VISIBLE);
                    noTaskFoundTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noTaskFoundImageView.setVisibility(View.GONE);
                    noTaskFoundTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        pendingChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                taskHeaderTextView.setText("Pending Task's :");

                fetchFilteredTasks("pending");

                cursor = databaseHelper.getPendingTasks();
                pendingTasks = new ArrayList<>();

                while (cursor.moveToNext()) {
                    int completedIndex = cursor.getColumnIndex("completed");
                    boolean isCompleted = (completedIndex != -1) && (cursor.getInt(completedIndex) == 1);


                    if (!isCompleted) {
                        pendingTasks.add(new TaskItModel(
                                cursor.getInt(0),
                                "",
                                cursor.getString(1),
                                cursor.getString(2),
                                cursor.getString(3),
                                cursor.getString(4),
                                false,
                                false,
                                repeat
                        ));
                    }
                }

                adapter.updateTaskList(pendingTasks);

                if (pendingTasks.isEmpty()) {
                    noTaskFoundImageView.setVisibility(View.VISIBLE);
                    noTaskFoundTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noTaskFoundImageView.setVisibility(View.GONE);
                    noTaskFoundTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });


        currentChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                taskHeaderTextView.setText("Todays Task's :");

                fetchFilteredTasks("today");

                cursor = databaseHelper.getTodaysTasks();
                todayTasks = new ArrayList<>();

                while (cursor.moveToNext()) {
                    todayTasks.add(new TaskItModel(
                            cursor.getInt(0),
                            "",
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            false,
                            false,
                            repeat
                    ));
                }

                adapter.updateTaskList(todayTasks);

                if (todayTasks.isEmpty()) {
                    noTaskFoundImageView.setVisibility(View.VISIBLE);
                    noTaskFoundTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noTaskFoundImageView.setVisibility(View.GONE);
                    noTaskFoundTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        completedChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                taskHeaderTextView.setText("Completed Task's :");

                fetchFilteredTasks("completed");

                cursor = databaseHelper.getCompletedTasks();
                completedTasks = new ArrayList<>();

                while (cursor.moveToNext()) {
                    completedTasks.add(new TaskItModel(
                            cursor.getInt(0),
                            "",
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            true,
                            false,
                            repeat
                    ));
                }

                adapter.updateTaskList(completedTasks);

                if (completedTasks.isEmpty()) {
                    noTaskFoundImageView.setVisibility(View.VISIBLE);
                    noTaskFoundTextView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    noTaskFoundImageView.setVisibility(View.GONE);
                    noTaskFoundTextView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            }
        });

        if (currentUser != null && !isNetworkAvailable()) {
            showThemedSnackbar(findViewById(android.R.id.content), "No internet connection!");
        }

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (allChip.isChecked()) {
                    fetchFilteredTasks("all");
                } else if (pendingChip.isChecked()) {
                    fetchFilteredTasks("pending");
                } else if (currentChip.isChecked()) {
                    fetchFilteredTasks("today");
                } else if (completedChip.isChecked()) {
                    fetchFilteredTasks("completed");
                }
                swipeRefreshLayout.setRefreshing(false);
            }
        });

        refreshTasks();

        recyclerView.setNestedScrollingEnabled(false);

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DocumentReference userRef = db.collection("users").document(userId);

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult() != null) {
                    String profilePhotoUrl = task.getResult().getString("profileUrl");

                    if (profilePhotoUrl != null && !profilePhotoUrl.equals("N/A")) {
                        if (!isFinishing() && !isDestroyed()) {  // Ensure Activity is still active
                            Glide.with(TaskActivity.this)
                                    .load(profilePhotoUrl)
                                    .transform(new CircleCrop())
                                    .placeholder(R.drawable.profile_icon) // Show while loading
                                    .error(R.drawable.profile_icon) // Show if failed
                                    .skipMemoryCache(false) // Keep in memory
                                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // Cache for offline use
                                    .into(profileImageView);
                        }
                    } else {
                        profileImageView.setImageDrawable(getDrawable(R.drawable.profile_icon));
                        Log.w(TAG, "Profile photo URL is missing or invalid");
                    }
                } else {
                    profileImageView.setImageDrawable(getDrawable(R.drawable.profile_icon));
                    Log.e(TAG, "Failed to load profile photo", task.getException());
                }
            });
        } else {
            Log.w(TAG, "User is not logged in");
            // User is not logged in, use SQLite database
            profileImageView.setImageDrawable(getDrawable(R.drawable.profile_icon));
        }

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_profile) {
                    Intent intent = new Intent(TaskActivity.this, ProfileActivity.class);
                    startActivity(intent);
                    finish();
                }

                if (itemId == R.id.nav_setting) {
                    Intent intent = new Intent(TaskActivity.this, SettingsActivity.class);
                    startActivity(intent);
                    finish();
                }

                if (itemId == R.id.nav_calendar) {
                    Intent intent = new Intent(TaskActivity.this, CalendarActivity.class);
                    startActivity(intent);
                    finish();
                }

                if (itemId == R.id.nav_history) {
                    Intent intent = new Intent(TaskActivity.this, HistoryActivity.class);
                    startActivity(intent);
                    finish();
                }

                if (itemId == R.id.nav_invite) {
                    shareApp();
                }

                return false;
            }
        });

        drawerImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.open();
            }
        });

        profileImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(TaskActivity.this, ProfileActivity.class);
                startActivity(intent);
                finish();
            }
        });

        addTaskPageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(TaskActivity.this, AddTaskActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private void logoutUser() {
        if (hasLoggedOutAutomatically || isLoggingOutManually) return;

        hasLoggedOutAutomatically = true;

        FirebaseAuth.getInstance().signOut();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
            shortcutManager.removeDynamicShortcuts(Collections.singletonList("Add Task"));
        }

        com.google.android.gms.auth.api.signin.GoogleSignInClient googleSignInClient =
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(
                        this,
                        new com.google.android.gms.auth.api.signin.GoogleSignInOptions
                                .Builder(com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .build()
                );

        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            Toast.makeText(this, "Account no longer exists. Logging out !", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(TaskActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void shareApp() {
        try {
            ApplicationInfo appInfo = getApplicationContext().getApplicationInfo();
            File srcFile = new File(appInfo.sourceDir);

            // Save as "TaskIt.apk" in the app's external storage directory
            File destFile = new File(getExternalFilesDir(null), "TaskIt.apk");

            // Copy the APK to the new location with the desired name
            copyFile(srcFile, destFile);

            // Get the URI using FileProvider
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".provider", destFile);

            // Create share intent
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/vnd.android.package-archive");
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Start sharing
            startActivity(Intent.createChooser(shareIntent, "Share TaskIt Via"));
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Unable to share TaskIt.", Toast.LENGTH_SHORT).show();
        }
    }

    // Helper method to copy the file
    private void copyFile(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) > 0) {
                out.write(buffer, 0, len);
            }
        }
    }

    // Method to update UI
    private void updateUI(ArrayList<TaskItModel> taskList) {
        adapter = new TaskItAdapter(this, taskList, noTaskFoundTextView, noTaskFoundImageView);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        if (taskList.isEmpty()) {
            noTaskFoundTextView.setVisibility(View.VISIBLE);
            noTaskFoundImageView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            noTaskFoundTextView.setVisibility(View.GONE);
            noTaskFoundImageView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
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

    private void fetchFilteredTasks(String filterType) {

        if (currentUser == null) return;

        String userId = currentUser.getUid();

        db.collection("users").document(userId).collection("tasks")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<TaskItModel> filteredTasks = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult()) {
                            boolean isCompleted = doc.getBoolean("completed") != null && doc.getBoolean("completed");
                            String taskDate = doc.getString("date");

                            // Filtering logic
                            if (filterType.equals("completed") && isCompleted) {
                                filteredTasks.add(mapToTask(doc, true));
                            } else if (filterType.equals("pending") && !isCompleted) {
                                filteredTasks.add(mapToTask(doc, false));
                            } else if (filterType.equals("today")) {
                                String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().getTime());
                                if (taskDate.equals(todayDate) && !isCompleted) {
                                    filteredTasks.add(mapToTask(doc, false));
                                }
                            } else if (filterType.equals("all")) {
                                filteredTasks.add(mapToTask(doc, isCompleted));
                            }
                        }
                        updateUI(filteredTasks);
                    }
                });
    }

    private TaskItModel mapToTask(DocumentSnapshot doc, boolean isCompleted) {
        boolean isReminder = Boolean.TRUE.equals(doc.getBoolean("reminder"));
        Intent intent = getIntent();
        String repeat = intent.getStringExtra("repeat");

        return new TaskItModel(
                0,  // SQLite auto-generated ID
                doc.getId(),  // Firestore ID
                doc.getString("task"),
                doc.getString("description"),
                doc.getString("date"),
                doc.getString("time"),
                isCompleted,
                isReminder,
                repeat
        );
    }

    // Function to reload tasks
    private void refreshTasks() {
        swipeRefreshLayout.setRefreshing(true);

        if (currentUser != null) {
            String userId = currentUser.getUid();

            db.collection("users").document(userId)
                    .collection("tasks")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            taskList.clear();
                            for (DocumentSnapshot doc : task.getResult()) {
                                taskList.add(new TaskItModel(
                                        0,  // SQLite auto-generated ID
                                        doc.getId(),  // Firestore ID
                                        doc.getString("task"),
                                        doc.getString("description"),
                                        doc.getString("date"),
                                        doc.getString("time"),
                                        doc.getBoolean("completed") != null && doc.getBoolean("completed"),
                                        doc.getBoolean("reminder") != null && doc.getBoolean("reminder"),
                                        doc.getString("repeat")
                                ));
                            }
                            updateUI(taskList);
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    })
                    .addOnFailureListener(e -> {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e("TaskActivity", "Error loading tasks", e);
                    });

        } else {
            // Load from SQLite if the user is not logged in
            cursor = databaseHelper.showData();
            taskList.clear();
            while (cursor.moveToNext()) {
                taskList.add(new TaskItModel(
                        cursor.getInt(0),    // Local SQLite ID (int)
                        "",                  // Empty Firestore ID (if not available in SQLite)
                        cursor.getString(1), // Task name
                        cursor.getString(2), // Description
                        cursor.getString(3), // Date
                        cursor.getString(4), // Time
                        cursor.getInt(5) == 1, // Boolean completed status
                        cursor.getInt(6) == 1,
                        cursor.getString(7)
                ));
            }
            updateUI(taskList);
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
        drawerLayout.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isLoggingOutManually && !hasLoggedOutAutomatically) {
            userStatusHandler.postDelayed(checkUserRunnable, USER_CHECK_INTERVAL_ONLINE);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        userStatusHandler.removeCallbacks(checkUserRunnable);
    }

}