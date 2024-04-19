package com.ceo.example.qrttracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;

import androidx.ads.identifier.AdvertisingIdInfo;
import androidx.annotation.NonNull;

import com.google.android.gms.ads.identifier.AdvertisingIdClient;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.iid.FirebaseInstanceId;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserLogin extends AppCompatActivity {
    TextInputEditText etPhoneNo, etOTPNo;
    Button btnResent, btnSubmit;
    private String IMEI = "";
    private ArrayList<String> userDetails;
    String version = "";
    private String generateotp;
    private final BroadcastReceiver mybroadcast = new SmsReceiver();
    private final int MY_PERMISSIONS_REQUEST_RECEIVE_SMS = 15;
    public static final String OTP_REGEX = "[0-9]{1,6}";
    String loginStatus = "";
    static String token = "";
    static String phoneNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_login);
        etPhoneNo = findViewById(R.id.etPhoneNo);
        etOTPNo = findViewById(R.id.etOTPNo);
        etOTPNo.setVisibility(View.GONE);
        etPhoneNo.setVisibility(View.VISIBLE);
        btnResent = findViewById(R.id.btnResent);
        btnResent.setVisibility(View.GONE);
        btnSubmit = findViewById(R.id.btnSubmit);
        determineAdvertisingInfo();
       // checkPermissions();
        //new LocationDetails(this).insertDataDistMast("", "", "", "", HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", ""), "", "", "", "e", "14", "0");
        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (HelperSharedPreferences.getSharedPreferencesBoolean(UserLogin.this, "isUserVerified", false)){
            startActivity(new Intent(getBaseContext(), MainActivity.class));
            finish();
        }

            System.out.println("checkservice.doInBackgroundIMEI:  "+IMEI);
        //receiveSMSMessage();
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btnSubmit.getText().toString().equalsIgnoreCase("Submit")) {
                    if (etPhoneNo.getText().toString().trim().length() > 0) {
                        if (isValidMobile(etPhoneNo.getText().toString())) {
                            if (HelperSharedPreferences.getSharedPreferencesString(UserLogin.this, "IMEI", "").trim().length() > 0) {
                                if (checkNetworkStatus(UserLogin.this)) {
                                    Login();
                                } else {
                                    NoInternetAlert(UserLogin.this, "mobileNo");
                                }
                            } else {
                                AlertDialog("First give Phone state permission", "Error", UserLogin.this);
                                requestPermission();
                            }
                        } else {
                            etPhoneNo.setError("Invalid Mobile Number");
                            etPhoneNo.setText("");
                        }


                    } else {
                        etPhoneNo.setError("First enter Mobile Number");
                    }
                } else {
                    if (etOTPNo.getText().toString().trim().length() > 0) {
                        VerifyOTP(etOTPNo.getText().toString(),token);
                       /* if (etOTPNo.getText().toString().equals(generateotp)) {
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "sectorname", userDetails.get(0));

                            System.out.println("UserLogin.onClick: " + HelperSharedPreferences.getSharedPreferencesString(UserLogin.this, "sectorname","" ));
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "distno", userDetails.get(1));
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "acno", userDetails.get(3));
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "secno", userDetails.get(4));
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "mobileno", userDetails.get(5));
                            startActivity(new Intent(getBaseContext(), MainActivity.class));
                            finish();
                        } else {
                            Toast.makeText(getBaseContext(), "Wrong OTP", Toast.LENGTH_LONG).show();
                        }*/
                    } else {
                        etPhoneNo.setError("First enter your OTP");
                    }

                }
            }
        });
        btnResent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkNetworkStatus(UserLogin.this)) {
                   // new Sendotp().execute();
                    ResentOTP();
                } else {
                    NoInternetAlert(UserLogin.this, "otp");
                }
            }
        });
    }

    private void determineAdvertisingInfo() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //Context applicationContext = getApplicationContext();
                    AdvertisingIdClient.Info adInfo = AdvertisingIdClient.getAdvertisingIdInfo(getApplicationContext());
                    String advertisingId = adInfo.getId();
                    boolean isLimitAdTrackingEnabled = adInfo.isLimitAdTrackingEnabled();
                    Log.d("Advertising ID: " , advertisingId);
                    Log.d("Limit Ad Tracking: " , String.valueOf(isLimitAdTrackingEnabled));
                    HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "IMEI", advertisingId);

                } catch (IOException | GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
                    e.printStackTrace();
                }
            }
        }).start();



    }




    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }
    @Override
    protected void onStart() {
        super.onStart();
        //receiveSMSMessage();
    }
    private String getVersion() {
        PackageInfo pInfo = null;
        String version = "";
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            int verCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return version;
    }
    private void Login(){
        JSONObject jsonObject = new JSONObject();
        if (IMEI.equals("")) {
            IMEI = HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", "");
        }
        try {
            jsonObject.put("mobile_no", etPhoneNo.getText().toString());
            jsonObject.put("version_name", getVersion());
            if (!IMEI.equals("")) {
                jsonObject.put("imei", IMEI);
            } else {
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        NetworkCall networkCall = new NetworkCall();
        networkCall.postJsonResponse("http://wbceo.in/qrttrackapi/login", jsonObject, this,"");
        networkCall.setOnOnNetworkCallListener(new NetworkCall.OnNetworkCallListener() {
            @Override
            public void onCompleted(boolean status, JSONObject jsonObject) {
                System.out.println("UserLogin.on LOGIN  "+jsonObject);
                if (status) {
                    try {
                        JSONObject data = jsonObject.getJSONObject("data");
                        String loginStatus = data.getString("login_status");
                        if (loginStatus.length() > 0 && loginStatus.equalsIgnoreCase("successful")) {

                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "mobileno", etPhoneNo.getText().toString());
                            phoneNumber=etPhoneNo.getText().toString();
                            token = data.getString("token");
                            etPhoneNo.setText("");
                            etOTPNo.setText("");
                            etPhoneNo.setVisibility(View.GONE);
                            etOTPNo.setVisibility(View.VISIBLE);
                            btnResent.setVisibility(View.VISIBLE);
                            resentOTPTimer();
                            btnSubmit.setText("VERIFY OTP");
                            receiveSMSMessage();
                            HelperSharedPreferences.putSharedPreferencesBoolean(UserLogin.this, "invalidVersion", false);

                        }else if (loginStatus.length() > 0 && loginStatus.equalsIgnoreCase("invalid version")){
                            HelperSharedPreferences.putSharedPreferencesBoolean(UserLogin.this, "invalidVersion", true);
                            VersionAlert("To proceed further, you have to update the version of your application", loginStatus.toUpperCase(), UserLogin.this);
                        } else{
                            AlertDialog(loginStatus, "Error!!", UserLogin.this);
                            HelperSharedPreferences.putSharedPreferencesBoolean(UserLogin.this, "invalidVersion", false);
                            phoneNumber="";
                            etPhoneNo.setText("");
                            etOTPNo.setText("");
                            etPhoneNo.setVisibility(View.VISIBLE);
                            etOTPNo.setVisibility(View.GONE);
                            btnResent.setVisibility(View.GONE);
                            btnSubmit.setText("SUBMIT");
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        phoneNumber="";
                        etOTPNo.setText("");
                        etPhoneNo.setText("");
                        etPhoneNo.setVisibility(View.VISIBLE);
                        etOTPNo.setVisibility(View.GONE);
                        btnResent.setVisibility(View.GONE);
                        btnSubmit.setText("SUBMIT");
                    }
                }

            }
        });
    }

    private void resentOTPTimer(){
        new CountDownTimer(30000, 1000) {

            public void onTick(long millisUntilFinished) {
                btnResent.setEnabled(false);
                btnResent.setBackgroundResource(R.drawable.buttonborder_greyshadow);
                btnResent.setText("Resend (" + millisUntilFinished / 1000+")");
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                btnResent.setBackgroundResource(R.drawable.buttonbordershadow);
                btnResent.setEnabled(true);
                btnResent.setText("Resend");
            }

        }.start();
    }

    private void ResentOTP() {
        //HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", "");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("token", token);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        NetworkCall networkCall = new NetworkCall();
        networkCall.postJsonResponse("https://wbceo.in/qrttrackapi/resendotp", jsonObject, this,"");
        networkCall.setOnOnNetworkCallListener(new NetworkCall.OnNetworkCallListener() {
            @Override
            public void onCompleted(boolean status, JSONObject jsonObject) {
                if (status) {
                    try {
                        JSONObject data = jsonObject.getJSONObject("data");
                        String otpText = data.getString("otp_status");
                        resentOTPTimer();
                        receiveSMSMessage();
                        AlertDialog(otpText, "", UserLogin.this);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private void VerifyOTP(String otp, String Token) {
        //HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", "");
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("token", Token);
            jsonObject.put("otp", otp);
            jsonObject.put("mobile_no", phoneNumber);
            jsonObject.put("android_version", Build.VERSION.RELEASE);
            jsonObject.put("imei", HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", ""));
            if (HelperSharedPreferences.getSharedPreferencesString(this, "firebasetoken", "").equals("")){
                jsonObject.put("firebasetoken", FirebaseInstanceId.getInstance().getToken());
            }else {
                jsonObject.put("firebasetoken", HelperSharedPreferences.getSharedPreferencesString(this, "firebasetoken", ""));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        NetworkCall networkCall = new NetworkCall();
        networkCall.postJsonResponse("http://wbceo.in/qrttrackapi/otpverification", jsonObject, this,"");
        networkCall.setOnOnNetworkCallListener(new NetworkCall.OnNetworkCallListener() {
            @Override
            public void onCompleted(boolean status, JSONObject jsonObject) {
                if (status) {
                    System.out.println("UserLogin.   OTP: "+jsonObject);
                    try {
                      /*  HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "sectorname", userDetails.get(0));

                        System.out.println("UserLogin.onClick: " + HelperSharedPreferences.getSharedPreferencesString(UserLogin.this, "sectorname","" ));
                        HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "distno", userDetails.get(1));
                        HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "acno", userDetails.get(3));
                        HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "secno", userDetails.get(4));
                        HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "mobileno", userDetails.get(5));
                        startActivity(new Intent(getBaseContext(), MainActivity.class));
                        finish();*/
                        JSONObject data = jsonObject.getJSONObject("data");
                        JSONObject metadata = jsonObject.getJSONObject("meta");
                        String server_time = metadata.getString("server_time");
                        HelperSharedPreferences.putSharedPreferencesString(UserLogin.this,"server_time",server_time);
                        String otpText = data.getString("otp_status");
                        if (otpText.equalsIgnoreCase("matched")) {
                            String sec_no = data.getString("qrt_no");
                            String ac_no = data.getString("ac_no");
                            String ac_name = data.getString("ac_name");
                            String dist_no = data.getString("dist_no");
                            String dist_name = data.getString("dist_name");
                            String pc_no = data.getString("pc_no");
                            String pc_name = data.getString("pc_name");
                            String sec_officer_name = data.getString("qrt_officer_name");
                            String person_role = data.getString("person_role");
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "sectorname", sec_officer_name);
                            System.out.println("UserLogin.onClick: " + HelperSharedPreferences.getSharedPreferencesString(UserLogin.this, "sectorname","" ));
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "distno", dist_no);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "dist_name", dist_name);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "acno", ac_no);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "ac_name", ac_name);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "secno", sec_no);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "person_role", person_role);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "pc_name", pc_name);
                            HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "pc_no", pc_no);
                            HelperSharedPreferences.putSharedPreferencesBoolean(UserLogin.this, "isUserVerified", true);
                            //HelperSharedPreferences.putSharedPreferencesString(UserLogin.this, "mobileno", userDetails.get(5));

                            AlertDialog(otpText, "", UserLogin.this);
                            startActivity(new Intent(getBaseContext(), MainActivity.class));
                            if (dialog.isShowing()){
                                dialog.dismiss();
                            }
                            UserLogin.this.finish();

                        }else {
                            AlertDialog(otpText, "", UserLogin.this);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

            }
        });
    }

    private String GenerateOTP(int size) {
        StringBuilder generatedToken = new StringBuilder();


        try {
            SecureRandom number = SecureRandom.getInstance("SHA1PRNG");
            // Generate 20 integers 0..20

            for (int i = 0; i < size; i++) {
                generatedToken.append(number.nextInt(9));
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return generatedToken.toString();
    }


    private void SMSSender() {
        try {
            SmsReceiver.bindListener(new SmsReceiver.SmsListener() {
                @Override
                public void messageReceived(String messageText) {

                    //From the received text string you may do string operations to get the required OTP
                    //It depends on your SMS format
                    Log.e("Message", messageText);

                    // If your OTP is six digits number, you may use the below code

                    Pattern pattern = Pattern.compile(OTP_REGEX);
                    Matcher matcher = pattern.matcher(messageText);
                    String otp = "";
                    while (matcher.find()) {
                        otp = matcher.group();
                    }

                    //Toast.makeText(Login.this,"OTP: "+ otp ,Toast.LENGTH_LONG).show();

                    etOTPNo.setText(otp);
                }
            });
        } catch (Exception e) {
            Log.d("Error OTP Auto Fetch : ", e.toString());
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();
        filter.addAction("android.provider.Telephony.SMS_RECEIVED");
        if (mybroadcast!=null){
            registerReceiver(mybroadcast, filter);
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mybroadcast != null) {
            unregisterReceiver(mybroadcast);
        }
    }

    private class checkservice extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (userDetails.get(2).equalsIgnoreCase("OTP send successfully")) {
                etPhoneNo.setText("");
                etOTPNo.setText("");
                etPhoneNo.setVisibility(View.GONE);
                etOTPNo.setVisibility(View.VISIBLE);
                btnResent.setVisibility(View.VISIBLE);
                btnSubmit.setText("Verify");
                generateotp = GenerateOTP(6);
                new Sendotp().execute();
            } else {
                AlertDialog(userDetails.get(4).toString(), "ERROR !!!", UserLogin.this);
            }

        }

        @SuppressLint("WrongThread")
        @Override
        protected Void doInBackground(Void... params) {
            userDetails = new NetworkCall().FetchLogin(etPhoneNo.getText().toString(), IMEI, version);
            return null;
        }
    }

    private class Sendotp extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            receiveSMSMessage();
        }

        @Override
        protected Void doInBackground(Void... params) {
            new NetworkCall().GenerateOtp01(userDetails.get(5), generateotp);
            return null;
        }
    }











//    public void checkPermissions() {
//
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (ContextCompat.checkSelfPermission(getApplicationContext(),
//                    Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                    IMEI = mngr.getImei();
//                } else {
//                    IMEI = mngr.getDeviceId();
//                }
//                HelperSharedPreferences.putSharedPreferencesString(this, "IMEI", IMEI);
//            } else {
//                requestPermission();
//            }
//        } else {
//            TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                IMEI = mngr.getImei();
//            } else {
//                IMEI = mngr.getDeviceId();
//            }
//            HelperSharedPreferences.putSharedPreferencesString(this, "IMEI", IMEI);
//        }
//    }

    protected void receiveSMSMessage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECEIVE_SMS},
                        MY_PERMISSIONS_REQUEST_RECEIVE_SMS);
            } else {
                SMSSender();
            }
        } else {
            SMSSender();
        }
    }

    public void requestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_PHONE_STATE)
                    != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this
                        , new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.ACCESS_FINE_LOCATION}, 1234);
            }
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1234:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
                    TelephonyManager mngr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        IMEI = mngr.getImei();
                    } else {
                        IMEI = mngr.getDeviceId();
                    }
                    HelperSharedPreferences.putSharedPreferencesString(this, "IMEI", IMEI);
                    System.out.println("UserLogin.onRequestPermissionsResult:  "+IMEI);
                }else if (grantResults[1] == PackageManager.PERMISSION_GRANTED){

                } else {
                    requestPermission();
                }

            case MY_PERMISSIONS_REQUEST_RECEIVE_SMS:

                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    SMSSender();
                }else {
                    //Toast.makeText(this, "Enter OTP manually", Toast.LENGTH_LONG).show();
                }
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
    AlertDialog dialog=null;
    public void AlertDialog(String message, String title, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        builder.setMessage(message);
        builder.setTitle(title);
        dialog = builder.create();
        dialog.show();
    }

    public void VersionAlert(String message, String title, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        builder.setMessage(message);
        builder.setTitle(title);
        builder.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(UserLogin.this, Downloadapk.class));
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
                System.exit(0);
                dialogInterface.dismiss();

            }
        });
        builder.show();
    }

    private boolean isValidMobile(String phone) {
        return android.util.Patterns.PHONE.matcher(phone).matches();
    }

    public boolean checkNetworkStatus(final Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting()) {
            //Toast.makeText(context, "Network Online", Toast.LENGTH_LONG).show();
            if (internetConnectionAvailable(5000)) {
                return true;
            } else {
                return false;
            }
        } else if (mobile.isConnectedOrConnecting()) {
            if (internetConnectionAvailable(5000)) {
                return true;
            } else {
                return false;
            }
        } else {
            Toast.makeText(context, "No Network connection available", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public void NoInternetAlert(final Context context, final String type) {
        android.app.AlertDialog.Builder dialog = new android.app.AlertDialog.Builder(context);
        dialog.setTitle("Please check your internet connection");
        final android.app.AlertDialog alertDialog = dialog.create();
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                if (type.equalsIgnoreCase("mobileNo")){
                    //new checkservice().execute();
                    Login();
                }else if (type.equalsIgnoreCase("otp")){
                    //new Sendotp().execute();
                    if (etOTPNo.getText().toString().trim().length() > 0) {
                        VerifyOTP(etOTPNo.getText().toString(),token);
                    }
                }
                alertDialog.dismiss();
            }
        });
        alertDialog.setButton(Dialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                alertDialog.dismiss();
            }
        });
        alertDialog.show();
    }

    private boolean internetConnectionAvailable(int timeOut) {
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

}
