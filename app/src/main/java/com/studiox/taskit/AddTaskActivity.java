package com.studiox.taskit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.DateValidatorPointForward;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.datepicker.MaterialPickerOnPositiveButtonClickListener;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.Notification.NotificationReceiver;
import com.studiox.taskit.TaskItModel.TaskItModel;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class AddTaskActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    LinearLayout backButton, saveTaskButton, datePickerButton, repeatDialogLayout, timePickerButton;
    MaterialSwitch reminderSwitch;
    TextView reminderTextView, dateTextView, timeTextView, repeatTextView;
    EditText taskName, taskDescription;
    DatabaseHelper databaseHelper;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;
    FirebaseFirestore db;
    private long selectedDateMillis = -1;
    private String repeatOption = "No";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_task);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        taskName = findViewById(R.id.taskNameEditText);
        taskDescription = findViewById(R.id.taskDescriptionEditText);
        saveTaskButton = findViewById(R.id.saveTaskButton);
        repeatDialogLayout = findViewById(R.id.repeatLayout);
        backButton = findViewById(R.id.backButton);
        repeatTextView = findViewById(R.id.repeatTextView);
        reminderTextView = findViewById(R.id.reminderTextView);
        reminderSwitch = findViewById(R.id.materialSwitchReminder);
        dateTextView = findViewById(R.id.dateTextView);
        datePickerButton = findViewById(R.id.materialDatePickerLayout);
        timeTextView = findViewById(R.id.timeTextView);
        timePickerButton = findViewById(R.id.materialTimePickerLayout);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        databaseHelper = new DatabaseHelper(AddTaskActivity.this);

        saveTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                if (taskName.length() > 0 && taskDescription.length() > 0) {

                    // If repeat is "Every day", start from today
                    if ("Every day".equals(repeatOption)) {
                        selectedDateMillis = System.currentTimeMillis();
                    } else if (selectedDateMillis == -1) {
                        Toast.makeText(AddTaskActivity.this, "Please select a date!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (timeTextView.getText().toString().isEmpty()) {
                        Toast.makeText(AddTaskActivity.this, "Please set a time!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (currentUser != null) {
                        // **Check Internet before saving to Firebase**
                        if (!isNetworkAvailable()) {
                            showThemedSnackbar(findViewById(android.R.id.content), "No internet connection!");
                            return;
                        }

                        boolean isReminder = reminderSwitch.isChecked();
                        String userId = currentUser.getUid();
                        String firebaseId = UUID.randomUUID().toString();
                        String repeat = repeatTextView.getText().toString().trim();

                        TaskItModel task = new TaskItModel(
                                0,
                                firebaseId,
                                taskName.getText().toString(),
                                taskDescription.getText().toString(),
                                dateTextView.getText().toString(),
                                timeTextView.getText().toString(),
                                false, // Task completion status
                                isReminder,
                                repeat
                        );

                        db.collection("users").document(userId)
                                .collection("tasks").document(firebaseId)
                                .set(task)
                                .addOnSuccessListener(documentReference -> {
                                    if (reminderSwitch.isChecked()) {
                                        firebasescheduleNotification(firebaseId);
                                    }
                                    Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();
                                    TaskUtils.updateTaskStatsInFirestore();
                                    taskName.setText("");
                                    taskDescription.setText("");
                                    Intent intent = new Intent(AddTaskActivity.this, TaskActivity.class);
                                    intent.putExtra("repeat", repeat);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(AddTaskActivity.this, "Failed to save task!", Toast.LENGTH_SHORT).show());

                    } else {
                        boolean isReminder = reminderSwitch.isChecked();
                        String repeat = repeatTextView.getText().toString();
                        databaseHelper.insertData(taskName.getText().toString(), taskDescription.getText().toString(), dateTextView.getText().toString(), timeTextView.getText().toString(), isReminder, repeat);
                        if (reminderSwitch.isChecked()) {
                            scheduleNotification();
                        }
                        Toast.makeText(AddTaskActivity.this, "Task added successfully!", Toast.LENGTH_SHORT).show();

                        taskName.setText("");
                        taskDescription.setText("");
                        Intent intent = new Intent(AddTaskActivity.this, TaskActivity.class);
                        intent.putExtra("repeat", repeat);
                        startActivity(intent);
                        finish();
                    }
                } else {
                    Toast.makeText(AddTaskActivity.this, "Please enter the task details!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        repeatDialogLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRadioButtonDialog();
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent intent = new Intent(AddTaskActivity.this, TaskActivity.class);
                startActivity(intent);
                finish();
            }
        });

        reminderSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                        performHapticFeedback();
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(AddTaskActivity.this, Manifest.permission.POST_NOTIFICATIONS)
                                    != PackageManager.PERMISSION_GRANTED) {

                        ActivityCompat.requestPermissions(AddTaskActivity.this,
                                new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);

                        buttonView.setChecked(false);
                        return;
                    }
                    reminderTextView.setText("On");
                } else {
                    SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                        performHapticFeedback();
                    }
                    reminderTextView.setText("Off");
                }
            }
        });

        datePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker();
            }
        });

        timePickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });
    }

    private void showRadioButtonDialog() {
        String[] options = {"No", "Every day"};
        TextView repeatTextView = findViewById(R.id.repeatTextView);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Repeat")
                .setSingleChoiceItems(options, repeatOption.equals("Every day") ? 1 : 0, (dialog, which) -> {
                    repeatOption = options[which];
                    repeatTextView.setText(repeatOption);

                    // Fade and disable the date picker layout
                    if ("Every day".equals(repeatOption)) {
                        datePickerButton.setAlpha(0.5f);
                        datePickerButton.setEnabled(false);
                        datePickerButton.setClickable(false);
                        dateTextView.setText("");
                    } else {
                        datePickerButton.setAlpha(1f);
                        datePickerButton.setEnabled(true);
                        datePickerButton.setClickable(true);
                    }

                    dialog.dismiss();
                })
                .show();
    }


    private void showTimePicker() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTitleText("Select Time :")
                .setHour(12)
                .setMinute(0)
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .build();

        timePicker.show(getSupportFragmentManager(), "TIME_PICKER");
        timePicker.addOnPositiveButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int hour = timePicker.getHour();
                int minute = timePicker.getMinute();
                String formattedTime = formatTime(hour, minute);
                timeTextView.setText(formattedTime);
            }
        });
    }

    private String formatTime(int hour, int minute) {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        return sdf.format(calendar.getTime()).toUpperCase(Locale.getDefault());
    }

    private void showDatePicker() {
        CalendarConstraints constraints = new CalendarConstraints.Builder()
                .setValidator(DateValidatorPointForward.now())
                .build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select Date:")
                .setCalendarConstraints(constraints)
                .build();

        datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
        datePicker.addOnPositiveButtonClickListener(new MaterialPickerOnPositiveButtonClickListener<Long>() {
            @Override
            public void onPositiveButtonClick(Long selection) {
                if (selection != null) {
                    selectedDateMillis = selection;
                    dateTextView.setText(formatDate(selection));
                }
            }
        });
    }

    private String formatDate(Long timeStamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        sdf.setTimeZone(TimeZone.getDefault());
        return sdf.format(new Date(timeStamp));
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

    private void scheduleNotification() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            return;
        }

        if (selectedDateMillis == -1 || timeTextView.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select a date and time!", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar timeCalendar = Calendar.getInstance();
        try {
            timeCalendar.setTime(sdf.parse(timeTextView.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);

        long alarmTime = calendar.getTimeInMillis();


        int taskId = databaseHelper.getLastInsertedTaskId();
        int uniqueRequestCode = taskId;  // ✅ Use taskId as request code (Consistent)

        // Save uniqueRequestCode for later cancellation
        SharedPreferences preferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE);
        preferences.edit().putInt("requestCode_" + taskId, uniqueRequestCode).apply();

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", taskId);
        intent.putExtra("taskName", taskName.getText().toString());
        intent.putExtra("taskDescription", taskDescription.getText().toString());
        intent.putExtra("repeatOption", repeatOption);
        intent.putExtra("taskTime", timeTextView.getText().toString());
        intent.putExtra("requestCode", uniqueRequestCode);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmTime, pendingIntent), pendingIntent);
        }

        Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show();
    }

    private void firebasescheduleNotification(String firebaseId) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_CODE);
            return;
        }
        if (selectedDateMillis == -1 || timeTextView.getText().toString().isEmpty()) {
            Toast.makeText(this, "Please select a date and time!", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        Calendar timeCalendar = Calendar.getInstance();
        try {
            timeCalendar.setTime(sdf.parse(timeTextView.getText().toString()));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(selectedDateMillis);
        calendar.set(Calendar.HOUR_OF_DAY, timeCalendar.get(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, timeCalendar.get(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, 0);

        long alarmTime = calendar.getTimeInMillis();

        int uniqueRequestCode = firebaseId.hashCode();

        // Save request code for future cancellation
        SharedPreferences preferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE);
        preferences.edit().putInt("requestCode_" + firebaseId, uniqueRequestCode).apply();

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", firebaseId); // sending Firebase ID
        intent.putExtra("taskName", taskName.getText().toString());
        intent.putExtra("taskDescription", taskDescription.getText().toString());
        intent.putExtra("repeatOption", repeatOption);
        intent.putExtra("taskTime", timeTextView.getText().toString());
        intent.putExtra("requestCode", uniqueRequestCode);


        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setAlarmClock(new AlarmManager.AlarmClockInfo(alarmTime, pendingIntent), pendingIntent);
        }

        Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            SharedPreferences sharedPreferences = getSharedPreferences("PermissionPrefs", MODE_PRIVATE);
            int denialCount = sharedPreferences.getInt("denialCount", 0);

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scheduleNotification();
            } else {
                denialCount++;
                sharedPreferences.edit().putInt("denialCount", denialCount).apply();

                if (denialCount >= 2) {
                    new MaterialAlertDialogBuilder(this)
                            .setTitle("Enable Notifications")
                            .setMessage("Task reminders require notification access. Please enable it in Settings.")
                            .setPositiveButton("Open Settings", (dialog, which) -> {
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                startActivity(intent);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                            .show();
                } else {
                    Toast.makeText(this, "Notification permission is required for reminders.", Toast.LENGTH_SHORT).show();
                }
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


    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(AddTaskActivity.this, TaskActivity.class);
        startActivity(intent);
        finish();
    }
}
