package com.ceo.example.qrttracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.BitmapFactory;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ceo.example.qrttracking.LocationTable.LocationDetails;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

public class LocationService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = LocationService.class.getSimpleName();
    public static final String STATUS_INTENT = "status";
    private GoogleApiClient mGoogleApiClient;
    private LinkedList<Map<String, Object>> mTransportStatuses = new LinkedList<>();
    private NotificationManager mNotificationManager, batteryNotificationManager, foregroundNotificationManager;
    private NotificationCompat.Builder mNotificationBuilder, batteryNotificationBuilder, foregroundNotificationBuilder;
    Notification notification = null;
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    public static final String URL = "http://wbceo.in/wb-gis/GISService.asmx?WSDL";  //"http://localhost:13981/GISService.asmx?WSDL";
    private PowerManager.WakeLock mWakelock;
    public static String serviceStatus = "";
    Context mContext;
    Location location = null; // location
    double latitude; // latitude
    double longitude; // longitude
    private String provider;
    // flag for GPS status
    boolean isGPSEnabled = false;
    CountDownTimer countDownTimer = null;
    Timer timer = null, timer1 = null;
    Handler handler = null, handler1 = null;
    // flag for network status
    boolean isNetworkEnabled = false;
    boolean isPassiveNetworkEnabled = false;
    private static String gpsStatus = "", previousGpsStatus = "";

    protected LocationManager locationManager;
    private KeyguardManager.KeyguardLock kl;

    public LocationService(Context context) {
        this.mContext = context;
    }

    public LocationService() {

    }

    public static LocationService getInstance() {
        return new LocationService();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        location=null;
        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "default";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();


            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(6, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            }else {
                startForeground(6, notification);
            }

          //  startForeground(6, notification);
        }
        buildNotification();
        batteryNotification();
        //buildAppForegroundNotification();

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        boolean isScreenOn = Build.VERSION.SDK_INT >= 20 ? powerManager.isInteractive() : powerManager.isScreenOn(); // check if screen is on

        if (!isScreenOn) {
            mWakelock = powerManager.newWakeLock(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE, getClass().getCanonicalName());

            mWakelock.acquire();
        }
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        kl = km.newKeyguardLock("name");
        kl.disableKeyguard();
        HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "isIDSent", false);
        HelperSharedPreferences.putSharedPreferencesInt(LocationService.this, "sessioncount", HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0) + 1);
        locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        if (isGPSENABLED()) {
            System.out.println("LocationService.isGPSENABLED");
            gpsStatus = "on";
        } else {
            gpsStatus = "off";
        }

        //startServiceOreoCondition();
        countDownTimer = null;
        startLocationTracking();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private static String getCurrentLocation = "";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        setOnLocationSendListener(new OnLocationSendListener() {
            @Override
            public void onCompleted(boolean status, String id) {
                if (status) {
                    HelperSharedPreferences.putSharedPreferencesInt(LocationService.this, "fetchlocationIndex", (HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0)) + 1);
                    HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "isIDSent", false);
                    if (countDownTimer != null) {
                        countDownTimer = null;
                    }
                }
            }
        });

        handler = new Handler();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        startLocationTracking();
                        if (getBatteryLevel() < 25) {
                            if (getBatteryLevel() < 10) {
                                setBatteryMessage(String.valueOf((int) getBatteryLevel()), true);
                            } else {
                                setBatteryMessage(String.valueOf((int) getBatteryLevel()), false);
                            }
                        } else {
                            clearNotification();
                        }
                        // setBatteryMessage(String.valueOf((int) getBatteryLevel()));

                        if (isGPSENABLED()) {
                            System.out.println("LocationService.isGPSENABLED");
                            gpsStatus = "on";
                        } else {
                            gpsStatus = "off";
                        }
                        if (checkNetworkStatus(LocationService.this)) {
                            if (!previousGpsStatus.equals("")) {
                                if (!previousGpsStatus.equals(gpsStatus)) {
                                    previousGpsStatus = gpsStatus;
                                    sendLocationStatustoServer(previousGpsStatus);
                                }
                            } else {
                                previousGpsStatus = gpsStatus;
                                sendLocationStatustoServer(previousGpsStatus);
                            }
                        }
                        if (getLocation() != null) {
                            if (getCurrentLocation.equals(getLocation().toString())) {
                                System.out.println("LocationService.run:  SAME LOCATION");

                            } else {
                                setStatusMessage(String.valueOf(getLocation().getLatitude()) + " , " + String.valueOf(getLocation().getLongitude()));

                                System.out.println("LocationService.getLocation123 ONLINE: " + getLocation().getLatitude() + " , " + getLocation().getLongitude());


                                System.out.println("LocationService.run:  DIFFERENT LOCATION:  " + getLocation());
                                if (checkNetworkStatus(LocationService.this)) {
                                    System.out.println("LocationService.ONLINENETWORK  " + String.valueOf(getLocation().getLatitude()) + "  " + String.valueOf(getLocation().getLongitude()));
                                    if (locationIsAtStatusOnline(getLocation())) {
                                        System.out.println("LocationService.location. getLocation");
                                        //2019-04-24 10:34:28
                                        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                                        dateFormatter.setLenient(false);
                                        Date today = new Date();
                                        String s = dateFormatter.format(today);
                                        networkCount(String.valueOf(getLocation().getLatitude()), String.valueOf(getLocation().getLongitude()), dateFormatter.format(today), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "isSOSUpdate", "0"));
                                           /* setStatusMessage(String.valueOf(getLocation().getLatitude()) + " , " + String.valueOf(getLocation().getLongitude()));

                                            System.out.println("LocationService.getLocation123 ONLINE: " + getLocation().getLatitude() + " , " + getLocation().getLongitude());*/
                                    } else {
                                        locationIsAtStatusOnline(getLocation());
                                    }
                                    getCurrentLocation = getLocation().toString();

                                } else {
                                    if (new LocationDetails(LocationService.this).getSpecificLatLong()[0] != null && new LocationDetails(LocationService.this).getSpecificLatLong()[1] != null) {

                                        System.out.println("LocationService.getSpecificLatLong  " + new LocationDetails(LocationService.this).getSpecificLatLong()[0]);
                                        if (locationIsAtStatus(getLocation())) {
                                            System.out.println("LocationService.location. getLocation");
                                            //2019-04-24 10:34:28
                                            DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
                                            dateFormatter.setLenient(false);
                                            Date today = new Date();
                                            String s = dateFormatter.format(today);

                                            if (serviceStatus.equalsIgnoreCase("s")) {
                                                System.out.println("MainActivity.onConnected  sessioncount:   " + HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0));

                                                new LocationDetails(LocationService.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "IMEI", ""), String.valueOf(getLocation().getLatitude()), String.valueOf(getLocation().getLongitude()), "", "s", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0)), "0", HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "isSOSUpdate", "0"));
                                                serviceStatus = "r";
                                            } else {
                                                new LocationDetails(LocationService.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "IMEI", ""), String.valueOf(getLocation().getLatitude()), String.valueOf(getLocation().getLongitude()), "", "r", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0)), "0", HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "isSOSUpdate", "0"));
                                            }

                                        } else {
                                            locationIsAtStatus(getLocation());
                                        }
                                        // setStatusMessage(String.valueOf(getLocation().getLatitude()) + " , " + String.valueOf(getLocation().getLongitude()));


                                        System.out.println("LocationService.getLocation123: " + getLocation().getLatitude() + " , " + getLocation().getLongitude());
                                        getCurrentLocation = getLocation().toString();
                                    } else {
                                        if (serviceStatus.equalsIgnoreCase("s")) {
                                            System.out.println("MainActivity.onConnected  sessioncount:   " + HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0));
                                            new LocationDetails(LocationService.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "IMEI", ""), String.valueOf(getLocation().getLatitude()), String.valueOf(getLocation().getLongitude()), "", "s", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0)), "0", HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "isSOSUpdate", "0"));
                                            serviceStatus = "r";
                                        } else {
                                            new LocationDetails(LocationService.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "IMEI", ""), String.valueOf(getLocation().getLatitude()), String.valueOf(getLocation().getLongitude()), "", "r", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0)), "0", HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "isSOSUpdate", "0"));
                                        }
                                    }
                                }


                            }
                        }

                        if (checkNetworkStatus(LocationService.this)) {
                            if (!HelperSharedPreferences.getSharedPreferencesBoolean(LocationService.this, "isIDSent", false)) {
                                Hashtable<String, String> hashtableList = new LocationDetails(LocationService.this).getSpecificData(LocationService.this, "0", HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0));

                                System.out.println("LocationService.run125: " + hashtableList);
                                if (hashtableList != null) {
                                    if (Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "tablesize", "0")) > 0) {
                                        System.out.println("LocationService.Tablesize11  " + HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "tablesize", "0"));
                                        if (HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0) < Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "tablesize", "0"))) {
                                            //System.out.println("LocationMonitoringService.hashtableList2255:  " + hashtableList.get(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this,"fetchlocationIndex",0)));
                                            int index;
                                            if (HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0) > 0) {
                                                index = HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0) - 1;
                                            } else {
                                                index = HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0);
                                            }
                                            System.out.println("LocationService.INDEX  " + index);
                                            String id = hashtableList.get("id");
                                            String dist = hashtableList.get("dist");
                                            String ac = hashtableList.get("ac");
                                            String secno = hashtableList.get("secno");
                                            String secmobile = hashtableList.get("secmobile");
                                            String imei = hashtableList.get("imei");
                                            String lat = hashtableList.get("lat");
                                            String lon = hashtableList.get("long");
                                            String upload_date = hashtableList.get("upload_date");
                                            String flag = hashtableList.get("flag");
                                            String session = hashtableList.get("session");
                                            String sos_update = hashtableList.get("sos_update");
                                            //HelperSharedPreferences.putSharedPreferencesString(LocationService.this,"tablesize", hashtableList.get(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this,"fetchlocationIndex",0)).get("tablesize"));
                               /* SendLocationAsyncParams params = new SendLocationAsyncParams(id, dist, ac, secno, secmobile, imei, lat, lon, upload_date, flag, session,isStopped);
                                SendLocationAsync myTask = new SendLocationAsync();
                                myTask.execute(params);*/
                                            System.out.println("LocationService.Tablesize: " + HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "tablesize", "0") + "  " + HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0));
                                            networkCountOffline(lat, lon, id, upload_date, secmobile, sos_update);
                                            //SendLocationtoServer(dist, ac, secno, secmobile, imei, lat, lon, upload_date, flag, session, id);
                                        } else {
                                            System.out.println("LocationService.fetchlocationIndex  11");
                                            if (HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0) > Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "tablesize", "0"))) {
                                                HelperSharedPreferences.putSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0);
                                            }
                                        }
                                    }
                                } else {
                                    hashtableList = new LocationDetails(LocationService.this).getSpecificData(LocationService.this, "0", HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0));

                                }

                            /*for (int i = 0; i < hashtableList.size(); i++) {
                                System.out.println("LocationMonitoringService.hashtableList2255:  " + hashtableList.get(i));
                                if (i==0) {
                                    String id = hashtableList.get(i).get("id");
                                    String dist = hashtableList.get(i).get("dist");
                                    String ac = hashtableList.get(i).get("ac");
                                    String secno = hashtableList.get(i).get("secno");
                                    String secmobile = hashtableList.get(i).get("secmobile");
                                    String imei = hashtableList.get(i).get("imei");
                                    String lat = hashtableList.get(i).get("lat");
                                    String lon = hashtableList.get(i).get("long");
                                    String upload_date = hashtableList.get(i).get("upload_date");
                                    String flag = hashtableList.get(i).get("flag");
                                    String session = hashtableList.get(i).get("session");
                                    *//*SendLocationAsyncParams params = new SendLocationAsyncParams(id, dist, ac, secno, secmobile, imei, lat, lon, upload_date, flag, session,isStopped);
                                    SendLocationAsync myTask = new SendLocationAsync();
                                    myTask.execute(params);*//*
                                    networkCount(lat, lon, id, upload_date);
                                    //SendLocationtoServer(dist, ac, secno, secmobile, imei, lat, lon, upload_date, flag, session, id);
                                }

                            }*/
                            } else {
                                networkSuspendCountdown();
                            }
                        } else {
                            Toast.makeText(LocationService.this, "No Network connection available", Toast.LENGTH_LONG).show();
                        }

                        //stopForeground(false);
                    }
                });
                //Call function
            }
        }, 0, 10000);


       /* handler1 = new Handler();
        timer1 = new Timer();
        timer1.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                setForegroundMessage("Open your EVM Tracking app for tracking");
            }
        },10,2*60*1000);*/
        return START_STICKY;
    }


    public boolean isGPSENABLED() {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        return statusOfGPS;
    }

    public void sendLocationStatustoServer(String status) {
        JSONObject jsonObject = new JSONObject();
        PackageInfo pInfo = null;
        String version = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        try {

            Date date = new Date(new Date().getTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            // return formatter.format(date);

            jsonObject.put("version", version);
            jsonObject.put("gps_status", status);
            jsonObject.put("imei", HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", ""));
            jsonObject.put("mobileserial", HelperSharedPreferences.getSharedPreferencesString(this, "mobileno", ""));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Request<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST, /*"http://10.173.46.82/track/receive/"*/"http://wbceo.in/qrttrackapi/gpsstatus", jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {


                        System.out.println("Background Service .onResponse GPSSTATUS: " + response);
                        try {
                            JSONObject data = response.getJSONObject("data");
                            String receive_status = data.getString("receive_status");
                            if (receive_status.equalsIgnoreCase("Successful")) {
                                HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "invalidVersion", false);

                            } else if (receive_status.equalsIgnoreCase("Wrong details")) {
                                logout();
                                HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "invalidVersion", true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // __listener.onCompleted(false,id);
                        }


                    }
                }, new Response.ErrorListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();

                //appendLog("NETWORK CALL :  " + error.toString());
                System.out.println("NetworkCall1.onErrorResponse: " + error);

            }
        });
        // requestQueue.add(jsonRequest);
        App.getInstance().addToRequestQueue(jsonRequest);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 24,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    public void networkSuspendCountdown() {
        if (HelperSharedPreferences.getSharedPreferencesBoolean(LocationService.this, "isIDSent", false)) {
            if (countDownTimer == null) {
                countDownTimer = new CountDownTimer(2 * 60 * 1000, 1000) { // adjust the milli seconds here

                    public void onTick(long millisUntilFinished) {
                    }

                    public void onFinish() {
                        HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "isIDSent", false);
                        countDownTimer = null;
                    }
                }.start();
            } else return;
        } else return;
    }

    private float getBatteryLevel() {
        Intent batteryStatus = registerReceiver(null,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int batteryLevel = -1;
        int batteryScale = 1;
        if (batteryStatus != null) {
            batteryLevel = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, batteryLevel);
            batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, batteryScale);
        }
        return batteryLevel / (float) batteryScale * 100;
    }

    @Override
    public void onDestroy() {
        if (mWakelock != null) {
            mWakelock.release();
        }
        /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); //true will remove notification
        }*/
        /* stopSelf();
        Intent restartService = new Intent("RestartService");
        sendBroadcast(restartService);*/
        super.onDestroy();

        stopSelf();
        startAlarmBroadcastReceiver(this, 5000);
        if (isGPSENABLED()) {
            System.out.println("LocationService.isGPSENABLED");
            gpsStatus = "on";
        } else {
            gpsStatus = "off";
        }
       /* if (timer!=null)
            timer.cancel();
        handler.removeCallbacksAndMessages(null);
        if (mNotificationManager != null) {
            mNotificationManager.cancelAll();
        }*/
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true);
           // stopForeground(Service.STOP_FOREGROUND_REMOVE);
        } else {*/
        System.out.println("LocationService.onDestroy");
        //  stopForeground(true);
        // }
        // stopSelf();
    }

   /* @Override
    public void onTaskRemoved(Intent rootIntent) {
        System.out.println("service in onTaskRemoved");
        long ct = System.currentTimeMillis(); //get current time
        Intent restartService = new Intent(getApplicationContext(),
                LocationService.class);
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 0, restartService,
                0);

        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        mgr.setRepeating(AlarmManager.RTC_WAKEUP, ct, 1 * 1000, restartServicePI);
    }*/

    public static void startAlarmBroadcastReceiver(Context context, long delay) {
        Intent _intent = new Intent("RestartService");
       // PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, _intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent resultPendingIntent = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resultPendingIntent = PendingIntent.getActivity(context, 0,  _intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            resultPendingIntent = PendingIntent.getActivity(context, 0, _intent, PendingIntent.FLAG_UPDATE_CURRENT);

        }


        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // Remove any previous pending intent.
        alarmManager.cancel(resultPendingIntent);
        alarmManager.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delay, resultPendingIntent);
    }

    private void buildAppForegroundNotification() {
        System.out.println("LocationService.buildNotification");
        foregroundNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        foregroundNotificationBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.group)
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(false)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.group))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(resultPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foregroundNotificationBuilder.setColor(getColor(R.color.colorGreen));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "FOREGROUND CHANNEL",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("CHANNEL_DISCRIPTION");
            if (foregroundNotificationManager != null) {
                foregroundNotificationManager.createNotificationChannel(channel);
            }
            startForeground(3, foregroundNotificationBuilder.build());
        }

        System.out.println("LocationService.buildAppForegroundNotification SERVICE");
        foregroundNotificationManager.notify(3, foregroundNotificationBuilder.build());
        //startForeground(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
    }

    /**
     * Sets the current status message (connecting/tracking/not tracking).
     */
    private void setForegroundMessage(String stringId) {

        foregroundNotificationBuilder.setContentText(stringId)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(false)
                .setOnlyAlertOnce(false)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foregroundNotificationBuilder.setColor(getColor(R.color.colorGreen));
        }
        foregroundNotificationManager.notify(3, foregroundNotificationBuilder.build());

    }

    public void networkCount(final String lat, final String lon, String uploadDate, String mobile, String sos_update) {
        JSONObject jsonObject = new JSONObject();
        PackageInfo pInfo = null;
        String version = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        try {

            Date date = new Date(new Date().getTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            // return formatter.format(date);

            //System.out.println("LocationService.networkCount  " + id + "  " + lat + "  " + lon);
            jsonObject.put("lat", lat);
            jsonObject.put("lng", lon);
            jsonObject.put("datetime", uploadDate);
            jsonObject.put("battarystatus", getBatteryLevel() + " :L");
            jsonObject.put("version", version);
            jsonObject.put("mobileserial", HelperSharedPreferences.getSharedPreferencesString(this, "mobileno", ""));
            jsonObject.put("sos", sos_update);
            jsonObject.put("sos_datetime", uploadDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("BackgroundTimeService.networkCount145:  " + jsonObject);

        //  HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "isIDSent", true);
        // new LocationDetails(LocationService.this).updateUploadingData(id, "1");

        HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLatitude", lat);
        HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLongitude", lon);
        //System.out.println("LocationService.onResponse:  status " + id);
        final Request<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST, /*"http://10.173.46.82/track/receive/"*/"http://wbceo.in/qrttrackapi/receive", jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        System.out.println("Background Service .onResponse LOCATION: " + response);


                        try {
                            JSONObject data = response.getJSONObject("data");
                            String receive_status = data.getString("receive_status");
                            if (receive_status.equalsIgnoreCase("Successful")) {
                                /*new LocationDetails(LocationService.this).updateData(id, "1");
                                if (__listener != null)
                                    __listener.onCompleted(true, id);*/
                                HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "invalidVersion", false);


                                System.out.println("LocationService11.onResponse:  networkCount " + lat + "  " + lon);
                            } else if (receive_status.equalsIgnoreCase("Wrong details")) {
                                HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "invalidVersion", true);
                                logout();
/*
                                if (__listener != null)
                                    __listener.onCompleted(false, id);*/
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // __listener.onCompleted(false,id);
                        }


                    }
                }, new Response.ErrorListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();

                //appendLog("NETWORK CALL :  " + error.toString());
                System.out.println("NetworkCall1.onErrorResponse: " + error);

            }
        });
        // requestQueue.add(jsonRequest);
        App.getInstance().addToRequestQueue(jsonRequest);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    public void networkCountOffline(String lat, String lon, final String id, String uploadDate, String mobile, String sos_update) {
        JSONObject jsonObject = new JSONObject();
        PackageInfo pInfo = null;
        String version = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        try {

            Date date = new Date(new Date().getTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            // return formatter.format(date);

            //System.out.println("LocationService.networkCount  " + id + "  " + lat + "  " + lon);
            jsonObject.put("lat", lat);
            jsonObject.put("lng", lon);
            jsonObject.put("datetime", uploadDate);
            jsonObject.put("battarystatus", getBatteryLevel() + " :H");
            jsonObject.put("version", version);
            jsonObject.put("mobileserial", HelperSharedPreferences.getSharedPreferencesString(this, "mobileno", ""));
            jsonObject.put("sos", sos_update);
            jsonObject.put("sos_datetime", uploadDate);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("BackgroundTimeService.HISTORY  :  " + jsonObject);

        HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "isIDSent", true);
        // new LocationDetails(LocationService.this).updateUploadingData(id, "1");

        System.out.println("LocationService.onResponse:  status " + id);
        final Request<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST, /*"http://10.173.46.82/track/receive/"*/"http://wbceo.in/qrttrackapi/receive_history", jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {


                        System.out.println("Background Service .onResponse HISTORY: " + response);
                        try {
                            JSONObject data = response.getJSONObject("data");
                            String receive_status = data.getString("receive_status");
                            if (receive_status.equalsIgnoreCase("Successful")) {
                                new LocationDetails(LocationService.this).deleteData(id);
                                if (__listener != null)
                                    __listener.onCompleted(true, id);
                                HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "invalidVersion", false);

                                System.out.println("LocationService11.onResponse:  status " + id + "  " + HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "fetchlocationIndex", 0));
                            } else if (receive_status.equalsIgnoreCase("Wrong details")) {
                                logout();
                                if (__listener != null)
                                    __listener.onCompleted(false, id);
                                HelperSharedPreferences.putSharedPreferencesBoolean(LocationService.this, "invalidVersion", true);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // __listener.onCompleted(false,id);
                        }


                    }
                }, new Response.ErrorListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();

                //appendLog("NETWORK CALL :  " + error.toString());
                System.out.println("NetworkCall1.onErrorResponse: " + error);

            }
        });
        // requestQueue.add(jsonRequest);
        App.getInstance().addToRequestQueue(jsonRequest);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 24,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    private void logout() {
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "IMEI");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "pc_no");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "pc_name");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "secno");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "ac_name");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "acno");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "dist_name");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "distno");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "sectorname");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "server_time");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "mobileno");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "tablesize");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "isIDsent");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "isIssueRaised");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "servicerunning");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "sessioncount");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "fetchlocationIndex");
        HelperSharedPreferences.removeSharedPreferencesBoolean(this, "isUserVerified");
        try {
            stopService(new Intent(this, LocationService.class));
            stopForeground(true);
            System.out.println("LocationService.logout");
            stopSelf();
//            unregisterReceiver(new LocationServiceReceiver());
        } catch (Exception e) {
            e.printStackTrace();
//            unregisterReceiver(new LocationServiceReceiver());
        }

    }

    private void batteryNotification() {
        System.out.println("LocationService.batteryNotification");
        batteryNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent resultPendingIntent= null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resultPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            resultPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        }


        batteryNotificationBuilder = new NotificationCompat.Builder(this, "default")
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setSmallIcon(R.drawable.group)
                //.setContentText("Your battery is "+batteryPercent+"% Charged.")
                .setContentText("Please connect the Mobile Charger")
                .setOngoing(false)
                .setContentIntent(null)
                .setAutoCancel(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "Battery Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("CHANNEL_DISCRIPTION");
            if (batteryNotificationManager != null) {
                batteryNotificationManager.createNotificationChannel(channel);
            }
           // startForeground(2, batteryNotificationBuilder.build());

            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(2, batteryNotificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
            }else {
                startForeground(2, batteryNotificationBuilder.build());
            }

        }
        //batteryNotificationManager.notify(2, batteryNotificationBuilder.build());
    }

    private void setBatteryMessage(String stringStatus, boolean b) {
        if (batteryNotificationManager != null) {


            batteryNotificationBuilder.setSmallIcon(R.drawable.group)
                    //.setContentText("Your battery is "+batteryPercent+"% Charged.")
                    .setContentText("Please connect the Mobile Charger")
                    .setOngoing(false)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(null)
                    .setAutoCancel(true);
            if (b) {
                batteryNotificationBuilder.setOnlyAlertOnce(false)
                        .setContentTitle("EMERGENCY BATTERY ALERT  " + stringStatus + "%");
            } else {
                batteryNotificationBuilder.setOnlyAlertOnce(true)
                        .setContentTitle("LOW BATTERY ALERT  " + stringStatus + "%");
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                batteryNotificationBuilder.setColor(getColor(R.color.red));
            }
            batteryNotificationManager.notify(2, batteryNotificationBuilder.build());
            /*if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel("default",
                        "Battery Notification",
                        NotificationManager.IMPORTANCE_HIGH);
                channel.setDescription("CHANNEL_DISCRIPTION");
                if (batteryNotificationManager != null) {
                    batteryNotificationManager.createNotificationChannel(channel);
                }
                startForeground(2, batteryNotificationBuilder.build());
            }*/ /*else {
                batteryNotificationManager.notify(2, batteryNotificationBuilder.build());
            }*/


        }

    }

    public void clearNotification() {
        batteryNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        stopForeground(true);
        batteryNotificationManager.cancel(2);
    }

    public void networkCountOnline(String lat, String lon) {
        JSONObject jsonObject = new JSONObject();
        PackageInfo pInfo = null;
        String version = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        try {

            Date date = new Date(new Date().getTime());
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            // return formatter.format(date);
            HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLatitude", lat);
            HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLongitude", lon);

            jsonObject.put("lat", lat);
            jsonObject.put("lng", lon);
            jsonObject.put("datetime", formatter);
            jsonObject.put("battarystatus", "100");
            jsonObject.put("version", "1.1");
            jsonObject.put("mobileserial", "9876543210");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        System.out.println("BackgroundTimeService.networkCount145:  " + jsonObject);

        // new LocationDetails(LocationService.this).updateUploadingData(id, "1");
        //System.out.println("LocationService.onResponse:  status " + id);
        final Request<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST, decryptIt("uZWcoiXKz0MNRljYt27r2QjMNgOQk1NXBfmqIy94Bw4="), jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        System.out.println("Background Service .onResponse LOCATION: " + response);
                        try {
                            if (response.getString("status").equalsIgnoreCase("true")) {
                                //__listener.onCompleted(true,id);
                                // new LocationDetails(LocationService.this).updateData(id, "1");
                                // System.out.println("LocationService.onResponse:  status " + id);
                                // HelperSharedPreferences.putSharedPreferencesInt(LocationService.this,"fetchlocationIndex",HelperSharedPreferences.getSharedPreferencesInt(LocationService.this,"fetchlocationIndex",0)+1);
                            } else {
                                //__listener.onCompleted(false,id);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            // __listener.onCompleted(false,id);
                        }


                    }
                }, new Response.ErrorListener() {
            @SuppressLint("ShowToast")
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
                System.out.println("NetworkCall1.onErrorResponse: " + error);

            }
        });
        // requestQueue.add(jsonRequest);
        App.getInstance().addToRequestQueue(jsonRequest);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(60000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    private OnLocationSendListener __listener = null;

    public void setOnLocationSendListener(OnLocationSendListener listener) {
        __listener = listener;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    public interface OnLocationSendListener {
        public abstract void onCompleted(boolean status, String id);
    }

    private OnVersionUpdateListener __updateListener = null;

    public void setOnVersionUpdateListener(OnVersionUpdateListener listener) {
        __updateListener = listener;
    }

    public interface OnVersionUpdateListener {
        public abstract void onCompleted(boolean status);
    }


    public void SendLocationtoServer(final String dist, final String ac, final String secno, final String secmobile, final String imei, final String lat, final String lon, final String upload_date, final String flag, final String session, final String Id) {


        String description = "";
        try {

            SoapProperties sobj = new SoapProperties();
            sobj.setActionName("InsertEVMTrackingDetails");
            sobj.setMethodName("InsertEVMTrackingDetails");
            String method = sobj.getMethodName();
            String action = sobj.getActionName();
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, method);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;

            envelope.setOutputSoapObject(request);
            request.addProperty("DISTRICT", dist);
            request.addProperty("AC_NO", ac);
            request.addProperty("SECNO", secno);
            request.addProperty("SEC_MOBILE_NO", secmobile);
            request.addProperty("IMEI", imei);
            request.addProperty("Lat", lat);
            request.addProperty("Long", lon);
            request.addProperty("time", upload_date);
            request.addProperty("flag", flag);
            request.addProperty("session", session);
            System.out.println("LocationMonitoringService.SendLocationtoServer  SoapObjec:  " + dist + "  " + ac + "  " + secno + "  " + secmobile + "   " + imei + "   " + lat + "   " + lon + "   " + upload_date + "   " + flag + "  " + session);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
//


            androidHttpTransport.call(action, envelope);

            //SoapObject response = (SoapObject) envelope.bodyIn;
            //SoapObject response = (SoapObject) envelope.getResponse();

            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            System.out.println("LocationMonitoringService.SendLocationtoServer  SoapObjec:  " + response);
            // SoapObject object1 = (SoapObject) response.getProperty(0);
            //SoapObject obj2 = (SoapObject) object1.getProperty(0);
            // int prop = obj2.getPropertyCount();
            /*list.add(obj2.getProperty(1).toString());
            list.add(obj2.getProperty(2).toString());
            list.add(obj2.getProperty(4).toString());
            list.add(obj2.getProperty(7).toString());
            list.add(obj2.getProperty(8).toString());
            list.add(obj2.getProperty(10).toString());*/


            Log.e("value of result", " result" + response);
            if (!response.equals("Failure")) {
                new LocationDetails(LocationService.this).updateData(Id, "1");
            }
        } catch (Exception e) {
            System.out.println(e + "this is exception");
        }
        //list = new ArrayList<>();


    }

    public static boolean checkNetworkStatus(final Context context) {
        boolean isNetworkToastShowing = false;
        final ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting()) {
            //Toast.makeText(context, "Network Online", Toast.LENGTH_LONG).show();
            if (internetConnectionAvailable(5000)) {
                isNetworkToastShowing = true;
            } else {
                isNetworkToastShowing = false;
            }
        } else if (mobile.isConnectedOrConnecting()) {
            if (internetConnectionAvailable(5000)) {
                isNetworkToastShowing = true;
            } else {
                isNetworkToastShowing = false;
            }
        } else {
            isNetworkToastShowing = false;
        }

        return isNetworkToastShowing;
    }

    public static boolean internetConnectionAvailable(int timeOut) {
        InetAddress inetAddress = null;
        try {
            Future<InetAddress> future = Executors.newSingleThreadExecutor().submit(new Callable<InetAddress>() {
                @Override
                public InetAddress call() {
                    try {
                        return InetAddress.getByName("www.google.com");
                    } catch (UnknownHostException e) {
                        return null;
                    }
                }
            });
            inetAddress = future.get(timeOut, TimeUnit.MILLISECONDS);
            future.cancel(true);
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        } catch (TimeoutException e) {
        }
        return inetAddress != null && !inetAddress.equals("");
    }

    private Location getLastBestLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                Location locationNet = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                long GPSLocationTime = 0;
                if (null != locationGPS) {
                    GPSLocationTime = locationGPS.getTime();
                }

                long NetLocationTime = 0;

                if (null != locationNet) {
                    NetLocationTime = locationNet.getTime();
                }

                if (0 < GPSLocationTime - NetLocationTime) {
                    return locationGPS;
                } else {
                    return locationNet;
                }
            }
        } else {
            return null;
        }
        return null;
    }

    /*private Location distanceLocation(){

    }*/
    public double getLatitude() {
        if (location != null) {
            latitude = location.getLatitude();
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
        if (location != null) {
            longitude = location.getLongitude();
        }

        // return longitude
        return longitude;
    }

    public Location getLocation() {
        try {
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setSpeedRequired(false);
            criteria.setCostAllowed(true);
            criteria.setBearingRequired(false);

            //API level 9 and up
            criteria.setHorizontalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setVerticalAccuracy(Criteria.ACCURACY_HIGH);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
            provider = locationManager.getBestProvider(criteria, true);
            System.out.println("LocationService.getLocation PROVIDER:  " + provider);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            //onLocationChanged(location);
            // getting GPS status
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            isPassiveNetworkEnabled = locationManager.isProviderEnabled(LocationManager.PASSIVE_PROVIDER);

            Location net_loc = null, gps_loc = null;

            System.out.println("LocationService.getLocation:  " + isPassiveNetworkEnabled + "  " + isGPSEnabled + "  " + isNetworkEnabled);


            // locationManager.requestLocationUpdates(provider, 1000, 5, this);
            /*if (isPassiveNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.PASSIVE_PROVIDER, 1000, 5, this);
                location = locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
                System.out.println("LocationService.PASSIVE PROVIDER:  "+location.getLatitude()+"   "+location.getLongitude());
            } else*/
            if (isGPSEnabled) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, this);
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
               /* if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }*/
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5, this);
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
               /* if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                }*/
            }

          /*  if (gps_loc != null && net_loc != null) {

                //smaller the number more accurate result will
                if (gps_loc.getAccuracy() > net_loc.getAccuracy())
                    location = net_loc;
                else
                    location = gps_loc;

                // I used this just to get an idea (if both avail, its upto you which you want to take as I've taken location with more accuracy)

            } else {
*/
                /*if (gps_loc != null) {
                    location = gps_loc;
                } else if (net_loc != null) {
                    location = net_loc;
                }*/
            //}
            /*locationManager.requestLocationUpdates( 1000, 10, criteria,this,null);
            location = locationManager.getLastKnownLocation(provider);*/

           /* if (location != null) {
                Log.e("Provider ", provider + " has been selected." + location.getLatitude() + "===" + location.getLongitude());
                if (serviceStatus.equalsIgnoreCase("s")) {
                    System.out.println("MainActivity.onConnected  sessioncount:   " + HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0));
                    new LocationDetails(LocationService.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "IMEI", ""), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), "", "s", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0)), "0");

                    serviceStatus = "r";
                } else {
                    new LocationDetails(LocationService.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(LocationService.this, "IMEI", ""), String.valueOf(location.getLatitude()), String.valueOf(location.getLongitude()), "", "r", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this, "sessioncount", 0)), "0");
                }
                setStatusMessage(String.valueOf(location.getLatitude()) + " , " + String.valueOf(location.getLongitude()));

                //onLocationChanged(location);
            }*/

            // getting network status
//            isNetworkEnabled = locationManager
//                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);


        } catch (Exception e) {
            e.printStackTrace();
            //appendLog("LOCATION CHANGE" + e.toString());
        }

        return location;
    }

    Location mCurrentLocation;

    private boolean locationIsAtStatus(Location location) {
       /* if (mTransportStatuses.size() <= statusIndex) {
            return false;
        }*/
        // Map<String, Object> status = mTransportStatuses.get(statusIndex);
        assert new LocationDetails(this).getSpecificLatLong() != null;
        Location locationForStatus = new Location("");
        locationForStatus.setLatitude(Double.parseDouble(new LocationDetails(this).getSpecificLatLong()[0].trim()));
        locationForStatus.setLongitude(Double.parseDouble(new LocationDetails(this).getSpecificLatLong()[1].trim()));
        float distance = location.distanceTo(locationForStatus);
        System.out.println("LocationService.locationIsAtStatus distance:  " + distance);
        // Log.d(TAG, String.format("Distance from status %s is %sm", statusIndex, distance));
        return distance > 10;
    }


   /* public void appendLog(String text) {
        File logFile = new File("sdcard/log.file");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }*/


    private boolean locationIsAtStatusOnline(Location location) {
       /* if (mTransportStatuses.size() <= statusIndex) {
            return false;
        }*/
        // Map<String, Object> status = mTransportStatuses.get(statusIndex);
        float distance = 0;
        if (!HelperSharedPreferences.getSharedPreferencesString(this, "onlinePrevLatitude", "0").equals("0") || !HelperSharedPreferences.getSharedPreferencesString(this, "onlinePrevLongitude", "0").equals("0")) {
            if (location.getLatitude() != Double.parseDouble(HelperSharedPreferences.getSharedPreferencesString(this, "onlinePrevLatitude", "0")) || location.getLatitude() != Double.parseDouble(HelperSharedPreferences.getSharedPreferencesString(this, "onlinePrevLongitude", "0"))) {
                Location locationForStatus = new Location("");
                locationForStatus.setLatitude(Double.parseDouble(HelperSharedPreferences.getSharedPreferencesString(this, "onlinePrevLatitude", "0")));
                locationForStatus.setLongitude(Double.parseDouble(HelperSharedPreferences.getSharedPreferencesString(this, "onlinePrevLongitude", "0")));
                distance = location.distanceTo(locationForStatus);
                System.out.println("LocationService.locationIsAtStatusONLINE distance:  " + distance);

            }

            // Log.d(TAG, String.format("Distance from status %s is %sm", statusIndex, distance));
            return distance > 10;
        } else {
            HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLatitude", String.valueOf(location.getLatitude()));
            HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLongitude", String.valueOf(location.getLongitude()));
            return true;
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            this.location = location;
            getLatitude();
            getLongitude();
        }
        /*if (locationIsAtStatus(location, 0) && locationIsAtStatus(location, 1)) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
        }*/
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private GoogleApiClient.ConnectionCallbacks mLocationRequestCallback = new GoogleApiClient
            .ConnectionCallbacks() {

        @SuppressLint({"MissingPermission", "InvalidWakeLockTag"})
        @Override
        public void onConnected(Bundle bundle) {
            LocationRequest request = new LocationRequest();
            request.setInterval(5000);
            request.setFastestInterval(2500);
            request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
            LocationServices.getFusedLocationProviderClient(LocationService.this);

            serviceStatus = "s";

            // Hold a partial wake lock to keep CPU awake when the we're tracking location.

        }

        @Override
        public void onConnectionSuspended(int reason) {
            // TODO: Handle gracefully
            startLocationTracking();
        }
    };


    private void startLocationTracking() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(mLocationRequestCallback)
                .addApi(LocationServices.API)
                .build();
        System.out.println("LocationService.startLocationTrack123");
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
            System.out.println("LocationService.startLocationTracking");
        }
    }

    private void startServiceOreoCondition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "TRACKING CHANNEL",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("CHANNEL_DISCRIPTION");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            startForeground(1, mNotificationBuilder.build());

            NotificationChannel channel1 = new NotificationChannel("default",
                    "Battery Notification",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel1.setDescription("CHANNEL_DISCRIPTION");
            if (batteryNotificationManager != null) {
                batteryNotificationManager.createNotificationChannel(channel1);
            }
            startForeground(2, batteryNotificationBuilder.build());
        }
    }

    private void buildNotification() {
        System.out.println("LocationService.buildNotification");

        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
//        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        PendingIntent resultPendingIntent = null;
//        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0,
//                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            resultPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        }else {
            resultPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        }


        mNotificationBuilder = new NotificationCompat.Builder(this, "default")
                .setSmallIcon(R.drawable.group)
                .setContentTitle(getString(R.string.app_name))
                .setOngoing(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOnlyAlertOnce(true)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.group))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setContentIntent(resultPendingIntent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNotificationBuilder.setColor(getColor(R.color.colorPrimary));
        }
        notification = mNotificationBuilder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default",
                    "TRACKING CHANNEL",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("CHANNEL_DISCRIPTION");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            startForeground(1, mNotificationBuilder.build());
        }

        mNotificationManager.notify(1, notification);
        //startForeground(FOREGROUND_SERVICE_ID, mNotificationBuilder.build());
    }

    /**
     * Sets the current status message (connecting/tracking/not tracking).
     */
    private void setStatusMessage(String stringId) {

        mNotificationBuilder.setContentText(stringId)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mNotificationBuilder.setColor(getColor(R.color.colorPrimary));
        }
       /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
          NotificationChannel channel = new NotificationChannel("default",
                    "TRACKING CHANNEL",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("CHANNEL_DISCRIPTION");
            if (mNotificationManager != null) {
                mNotificationManager.createNotificationChannel(channel);
            }
            startForeground(1, mNotificationBuilder.build());
        }*/ /*else {
            mNotificationManager.notify(1, mNotificationBuilder.build());
        }*/
        notification = mNotificationBuilder.build();
        notification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(1, notification);

        // Also display the status message in the activity.
      /*  Intent intent1 = new Intent(STATUS_INTENT);
        intent1.putExtra("status", stringId);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent1);*/
    }


    private static String cryptoPass = "sup3rS3xy";
    public static String decryptIt(String value) {
        try {
            DESKeySpec keySpec = new DESKeySpec(cryptoPass.getBytes("UTF8"));
            SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("DES");
            SecretKey key = keyFactory.generateSecret(keySpec);

            byte[] encrypedPwdBytes = Base64.decode(value, Base64.DEFAULT);
            // cipher is not thread safe
            Cipher cipher = Cipher.getInstance("DES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypedValueBytes = (cipher.doFinal(encrypedPwdBytes));

            String decrypedValue = new String(decrypedValueBytes);
            //System.out.println("PollingDetails.decryptIt1452: "+value+" Decrypted Value "+decrypedValue);
            Log.d("TAG", "Decrypted: " + value + " -> " + decrypedValue);
            return decrypedValue;

        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        }
        // System.out.println("PollingDetails.decryptIt: "+value);
        return value;
    }

}
