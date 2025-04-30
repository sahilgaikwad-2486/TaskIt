package com.studiox.taskit;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.HistoryModel.HistoryModel;
import com.studiox.taskit.Notification.NotificationReceiver;

public class TaskDetailsActivity extends AppCompatActivity {

    LinearLayout backButton, deleteTaskButton;
    TextView reminderTextView, dateTextView, timeTextView, nameTextView, descriptionTextView, repeatTextView;
    DatabaseHelper databaseHelper;
    FirebaseFirestore db;
    FirebaseAuth mAuth;
    FirebaseUser currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_task_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        nameTextView = findViewById(R.id.taskNameTextView);
        descriptionTextView = findViewById(R.id.taskDescriptionTextView);
        dateTextView = findViewById(R.id.dateTextView);
        timeTextView = findViewById(R.id.timeTextView);
        reminderTextView = findViewById(R.id.reminderTextView);
        backButton = findViewById(R.id.backButton);
        repeatTextView = findViewById(R.id.repeatTextView);
        deleteTaskButton = findViewById(R.id.deleteTaskButton);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        databaseHelper = new DatabaseHelper(this);

        Intent intent = getIntent();

        if (intent != null && intent.getExtras() != null) {
            nameTextView.setText(intent.getStringExtra("name"));
            descriptionTextView.setText(intent.getStringExtra("description"));
            timeTextView.setText(intent.getStringExtra("time"));
            dateTextView.setText(intent.getStringExtra("date"));
            String repeatValue = intent.getStringExtra("repeat");
            if (repeatValue == null || repeatValue.isEmpty()) {
                repeatTextView.setText("No");
            } else {
                repeatTextView.setText(repeatValue);
            }
            reminderTextView.setText(intent.getStringExtra("reminder"));
        }

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                Intent backIntent = new Intent(TaskDetailsActivity.this, TaskActivity.class);
                startActivity(backIntent);
                finish();
            }
        });

        deleteTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                if (sharedPreferences.getBoolean("haptic_feedback", true)) {
                    performHapticFeedback();
                }

                new MaterialAlertDialogBuilder(TaskDetailsActivity.this)
                        .setTitle("Delete Task")
                        .setMessage("Are you sure you want to delete this task?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            String taskName = getIntent().getStringExtra("name");
                            String taskDescription = getIntent().getStringExtra("description");

                            if (currentUser != null) {
                                // ✅ FIREBASE: Delete from tasks, add to history
                                String userId = currentUser.getUid();

                                db.collection("users").document(userId)
                                        .collection("tasks")
                                        .whereEqualTo("task", taskName)
                                        .whereEqualTo("description", taskDescription)
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots -> {
                                            for (DocumentSnapshot doc : queryDocumentSnapshots) {
                                                // Delete from "tasks"
                                                doc.getReference().delete();

                                                cancelFirebaseNotification(doc.getId());

                                                // Add to "history"
                                                db.collection("users").document(userId)
                                                        .collection("history")
                                                        .add(new HistoryModel(taskName));
                                            }

                                            Toast.makeText(TaskDetailsActivity.this, "Task deleted!", Toast.LENGTH_SHORT).show();
                                            TaskUtils.updateTaskStatsInFirestore();
                                            navigateToTaskActivity();
                                        })
                                        .addOnFailureListener(e ->
                                                Toast.makeText(TaskDetailsActivity.this, "Failed to delete task!", Toast.LENGTH_SHORT).show()
                                        );

                            } else {
                                // ✅ LOCAL: Delete from DB, add to local history
                                int taskId = getIntent().getIntExtra("task_id", -1);
                                if (taskId != -1 && taskName != null) {
                                    databaseHelper.insertHistory(taskName);
                                    databaseHelper.deleteData(String.valueOf(taskId));
                                    cancelNotification(taskId);
                                    Toast.makeText(TaskDetailsActivity.this, "Task deleted!", Toast.LENGTH_SHORT).show();
                                    navigateToTaskActivity();
                                } else {
                                    Toast.makeText(TaskDetailsActivity.this, "Unable to delete task.", Toast.LENGTH_SHORT).show();
                                }
                            }
                        })
                        .setNegativeButton("No", null)
                        .show();
            }
        });

    }

    // **Navigate to TaskActivity after deletion**
    private void navigateToTaskActivity() {
        Intent intent = new Intent(TaskDetailsActivity.this, TaskActivity.class);
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

    private void cancelFirebaseNotification(String firebaseId) {
        if (firebaseId == null || firebaseId.isEmpty()) {
            Toast.makeText(this, "Invalid Firebase Task ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE);
        int uniqueRequestCode = preferences.getInt("requestCode_" + firebaseId, firebaseId.hashCode()); // fallback to hashCode

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", firebaseId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "Firebase reminder canceled!", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelNotification(int taskId) {
        if (taskId == -1) {
            Toast.makeText(this, "Invalid Task ID!", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences preferences = getSharedPreferences("TaskPrefs", MODE_PRIVATE);
        int uniqueRequestCode = preferences.getInt("requestCode_" + taskId, taskId); // Retrieve stored request code

        Intent intent = new Intent(this, NotificationReceiver.class);
        intent.putExtra("taskId", taskId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            Toast.makeText(this, "Reminder canceled!", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(TaskDetailsActivity.this, TaskActivity.class);
        startActivity(intent);
        finish();
    }
}