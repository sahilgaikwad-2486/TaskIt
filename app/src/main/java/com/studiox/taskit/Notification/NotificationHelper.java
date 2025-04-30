package com.studiox.taskit.Notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class NotificationHelper {
    public static void firebasecancelNotification(Context context, String firebaseId) {
        SharedPreferences preferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE);
        int uniqueRequestCode = preferences.getInt("requestCode_" + firebaseId, firebaseId.hashCode());

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            preferences.edit().remove("requestCode_" + firebaseId).apply();
        }
    }

    public static void cancelNotification(Context context, int taskId) {
        SharedPreferences preferences = context.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE);
        int uniqueRequestCode = preferences.getInt("requestCode_" + taskId, taskId);

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context, uniqueRequestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            preferences.edit().remove("requestCode_" + taskId).apply();
        }
    }


}

