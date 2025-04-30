package com.studiox.taskit.Notification;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.studiox.taskit.R;
import com.studiox.taskit.TaskActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "NotificationChannel";

    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String taskName = intent.getStringExtra("taskName");
        String taskDescription = intent.getStringExtra("taskDescription");
        String repeatOption = intent.getStringExtra("repeatOption");
        String taskTime = intent.getStringExtra("taskTime");
        int taskId = intent.getIntExtra("taskId", -1);
        int requestCode = intent.getIntExtra("requestCode", 0);

        // Launching activity on click
        Intent activityIntent = new Intent(context, TaskActivity.class);
        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Done button
        Intent doneIntent = new Intent(context, TaskCompletionReceiver.class);
        doneIntent.putExtra("taskId", taskId);
        doneIntent.putExtra("taskName", taskName);
        doneIntent.putExtra("taskDescription", taskDescription);
        PendingIntent donePendingIntent = PendingIntent.getBroadcast(
                context,
                taskId,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(taskName)
                .setContentText(taskDescription)
                .setSmallIcon(R.drawable.notification_icon)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Add "Done" button only if task is not repeating
        if (repeatOption == null || !repeatOption.equals("Every day")) {
            builder.addAction(0, "Done", donePendingIntent);
        }

        Notification notification = builder.build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(taskId, notification);
        }

        // 🔁 Reschedule if repeating
        if ("Every day".equals(repeatOption)) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

            Intent newIntent = new Intent(context, NotificationReceiver.class);
            newIntent.putExtras(intent); // carry all extras again

            Calendar nextAlarm = Calendar.getInstance();
            nextAlarm.add(Calendar.DATE, 1); // tomorrow

            try {
                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                Calendar originalTime = Calendar.getInstance();
                originalTime.setTime(sdf.parse(taskTime));
                nextAlarm.set(Calendar.HOUR_OF_DAY, originalTime.get(Calendar.HOUR_OF_DAY));
                nextAlarm.set(Calendar.MINUTE, originalTime.get(Calendar.MINUTE));
                nextAlarm.set(Calendar.SECOND, 0);
            } catch (Exception e) {
                e.printStackTrace();
            }

            PendingIntent newPendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    newIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            alarmManager.setAlarmClock(
                    new AlarmManager.AlarmClockInfo(nextAlarm.getTimeInMillis(), newPendingIntent),
                    newPendingIntent
            );

            Log.d("NotificationReceiver", "Rescheduled for: " + nextAlarm.getTime());
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reminders", NotificationManager.IMPORTANCE_HIGH
            );
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
