package com.ceo.example.qrttracking;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import android.widget.Toast;

public class LocationServiceReceiver extends BroadcastReceiver {
    public static final String ACTION_PROCESS_UPDATES =
            "com.google.android.gms.location.sample.backgroundlocationupdates.action" +
                    ".PROCESS_UPDATES";
    NotificationManager foregroundNotificationManager;
    NotificationCompat.Builder foregroundNotificationBuilder;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            Toast.makeText(context, "BROADCAST RECEIVER STARTED", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                System.out.println("LocationServiceReceiver.onReceive upload");
                context.startForegroundService(new Intent(context, LocationService.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } else {
                context.startService(new Intent(context, LocationService.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
            buildAppForegroundNotification(context);
            System.out.println("LocationServiceReceiver.onReceive BROADCAST");
        }

    }
    private void buildAppForegroundNotification(Context context) {
        System.out.println("LocationService.buildNotification");
        foregroundNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        foregroundNotificationBuilder = new NotificationCompat.Builder(context, "default")
                .setSmallIcon(R.drawable.group)
                .setContentTitle(context.getString(R.string.app_name))
                .setOngoing(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.group))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(resultPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foregroundNotificationBuilder.setColor(context.getColor(R.color.colorGreen));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "FOREGROUND CHANNEL",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("CHANNEL_DISCRIPTION");
            if (foregroundNotificationManager != null) {
                foregroundNotificationManager.createNotificationChannel(channel);
            }
            //context.startForeground(3, foregroundNotificationBuilder.build());
        }
        System.out.println("LocationServiceReceiver.buildAppForegroundNotification NOTIFYBROADCAST");

        foregroundNotificationManager.notify(3, foregroundNotificationBuilder.build());
        //startForeground(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
    }
}
