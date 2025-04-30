package com.studiox.taskit.Notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.studiox.taskit.DatabaseHelper.DatabaseHelper;
import com.studiox.taskit.TaskUtils;

public class TaskCompletionReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int taskId = intent.getIntExtra("taskId", -1);
        String taskName = intent.getStringExtra("taskName");
        String taskDescription = intent.getStringExtra("taskDescription");

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        if (currentUser != null) {
            // **User is logged in → Update task in Firebase**
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            String userId = currentUser.getUid();

            db.collection("users").document(userId)
                    .collection("tasks")
                    .whereEqualTo("task", taskName)
                    .whereEqualTo("description", taskDescription)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (var doc : queryDocumentSnapshots) {
                            doc.getReference().update("completed", true);
                        }
                        TaskUtils.updateTaskStatsInFirestore(); // ✅ Update Firebase stats
                        showToastAndCancelNotification(context, taskId, "Task completed!");
                    })
                    .addOnFailureListener(e ->
                            showToastAndCancelNotification(context, taskId, "Failed to update task!")
                    );

        } else if (taskId != -1) {
            // **User is offline → Update task in SQLite**
            databaseHelper.updateTaskCompletion(taskId, true);
            showToastAndCancelNotification(context, taskId, "Task completed!");
        }
    }

    private void showToastAndCancelNotification(Context context, int taskId, String message) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(taskId);
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
