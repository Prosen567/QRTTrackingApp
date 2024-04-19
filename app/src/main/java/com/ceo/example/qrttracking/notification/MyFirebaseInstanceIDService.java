package com.ceo.example.qrttracking.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Build;
import androidx.core.app.NotificationCompat;

import com.ceo.example.qrttracking.HelperSharedPreferences;
import com.ceo.example.qrttracking.MainActivity;
import com.ceo.example.qrttracking.R;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseInstanceIDService extends FirebaseMessagingService {
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder;
    Notification notification = null;

    public MyFirebaseInstanceIDService() {
        super();
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if(remoteMessage.getData().size() > 0){
            //handle the data message here
        }

        //getting the title and the body
        String title = remoteMessage.getNotification().getTitle();
        String body = remoteMessage.getNotification().getBody();
        System.out.println("MyFirebaseInstanceIDService.onMessageReceived:  "+title+"  "+body);

        buildNotification(title,body);
    }

    @Override
    public void onMessageSent(String s) {
        super.onMessageSent(s);
    }

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        System.out.println("MyFirebaseInstanceIDService.onNewToken: "+s);
        HelperSharedPreferences.putSharedPreferencesString(this,"firebasetoken",s);
    }


    private void buildNotification(String title, String message) {

        System.out.println("MyFirebaseInstanceIDService.buildNotification");
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        mNotificationBuilder = new NotificationCompat.Builder(this, "default1")
                .setSmallIcon(R.drawable.group)
                .setContentTitle(title)
                .setOngoing(false)
                .setAutoCancel(true)
                .setContentText(message)
                .setOnlyAlertOnce(false)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.group))
                .setPriority(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(resultPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNotificationBuilder.setColor(getColor(R.color.colorPrimary));
        }
        notification = mNotificationBuilder.build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default11",
                    "TRACKING CHANNEL11",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("CHANNEL_DISCRIPTION111");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            startForeground(5, mNotificationBuilder.build());
        }

        mNotificationManager.notify(5, notification);
        //startForeground(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
    }
}
