package com.studiox.taskit;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class TaskUtils {
    public static void updateTaskStatsInFirestore() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        String todayDate = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Calendar.getInstance().getTime());

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

                            if (taskDate != null && taskDate.equals(todayDate)) {
                                currentCount++;
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("TaskUtils", "Failed to update task stats", e));
    }
}
