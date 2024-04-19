package com.ceo.example.qrttracking;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
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

public class NetworkCall {

    private static RequestQueue requestQueue;
    public static final String BASEURL="http://track.southindia.cloudapp.azure.com/track_new/"/*"http://10.173.46.82:58115/"*/;
    private OnNetworkCallListener __listener = null;
    Request<JSONObject> jsonRequest;
    private Dialog dialog = null;

    ArrayList<String> list;
    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    //public static final String URL = "http://10.173.46.87/wb-gis/GISService.asmx?WSDL";
    public final String WSDL_OTP_NAMESPACE = "http://wbceo.in/wb-sms/";
    public static final String URL="http://wbceo.in/wb-gis/GISService.asmx?WSDL";
    String val;
    public static NetworkCall getInstance() {
        return new NetworkCall();
    }

    public void postJsonResponse(String urlstr, JSONObject jdata, Context context,String type) {
        if (checkNetworkStatus(context)) {
            try {
                dialog = new Dialog(context, android.R.style.Theme_Black);
                View views = LayoutInflater.from(context).inflate(R.layout.activity_dialog, null);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.getWindow().setBackgroundDrawableResource(R.color.grey_font);
                dialog.setContentView(views);
                dialog.show();

                //String url = urlstr + "?json=" + jdata;
               /* if (requestQueue == null) {
                    requestQueue = Volley.newRequestQueue(context);
                }*/

                System.out.println("NetworkCall.getJsonPostResponse: " + urlstr + "  " + jdata);
                if (jsonRequest == null) {
                    jsonRequest = new JsonObjectRequest(Request.Method.POST, urlstr, jdata,
                            new Response.Listener<JSONObject>() {
                                @SuppressLint("ShowToast")
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (dialog != null) {
                                        dialog.dismiss();
                                    }
                                    System.out.println("NetworkCall1.onResponse: " + response);
                                    if (__listener != null) {
                                        __listener.onCompleted(true, response);
                                    }
                                }
                            }, new Response.ErrorListener() {
                        @SuppressLint("ShowToast")
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            error.printStackTrace();
                            System.out.println("NetworkCall1.onErrorResponse: " + error);
                            if (dialog != null) {
                                dialog.dismiss();
                            }
                            if (__listener != null) {
                                __listener.onCompleted(false, null);
                            }
                        }
                    })
                    /*{
                     *//** Passing some request headers* *//*
                        @Override
                        public Map getHeaders() throws AuthFailureError {
                            HashMap headers = new HashMap();
                            headers.put("Content-Type", "application/json");
                            headers.put("apiKey", "xxxxxxxxxxxxxxx");
                            return headers;
                        }
                    }*/;
                }
                App.getInstance().addToRequestQueue(jsonRequest);
                jsonRequest.setRetryPolicy(new DefaultRetryPolicy(60 * 1000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            } catch (Exception e) {
                if (dialog != null) {
                    dialog.dismiss();
                }

            }
        } else {
            NoInternetAlert(context, type,urlstr, jdata);
        }
    }


    public void GenerateOtp01(String ph, String otp) {
        try {

            SoapProperties sobj = new SoapProperties();
            sobj.setActionName("sendSingleSMS");
            sobj.setMethodName("sendSingleSMS");
            String val = "http://wbceo.in/wb-sms/";
            // String method = sobj.getMethodName();
            String action = "sendSingleSMS";
            action = val + action;
            SoapObject request = new SoapObject(WSDL_OTP_NAMESPACE, "sendSingleSMS");
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;

            envelope.setOutputSoapObject(request);
            request.addProperty("username", "ceowb");
            request.addProperty("password", "#ceoelection2017");
            request.addProperty("senderid", "WBELEC");
            request.addProperty("mobileNo", ph);
            request.addProperty("message", otp + " Is your Otp for EVMTracking App");
            request.addProperty("secureKey", "20fff724-17a0-420a-9ce5-9a14eecf6c2a");


            HttpTransportSE androidHttpTransport = new HttpTransportSE("http://wbceo.in/wb-sms/SMSCDAC.asmx?WSDL");
//


            androidHttpTransport.call(action, envelope);

            SoapObject response = (SoapObject) envelope.bodyIn;
//        SoapObject object1 = (SoapObject) response.getProperty(0);
//        SoapObject obj2 = (SoapObject) object1.getProperty(0);
//        int prop = obj2.getPropertyCount();
//        Pojoforall.username1 = obj2.getProperty(6).toString();
//        Pojoforall.userdistrictcode = (obj2.getProperty(3).toString());
//        Pojoforall.Fetchac = obj2.getProperty(10).toString();

            System.out.println("Soapservice.GenerateOtp01: "+response);

            Log.e("value of result", " result" + response);
        } catch (Exception e) {

            System.out.println(e + "this is exception");
        }


    }

    public ArrayList FetchLogin(String ph, String IMEI, String ver) {
        String userac = "";

        String district = "";

        String username = "";
        list = new ArrayList<>();
        String description = "";
        try {

            SoapProperties sobj = new SoapProperties();
            sobj.setActionName("FetchLogin_EVM_Tracking");
            sobj.setMethodName("FetchLogin_EVM_Tracking");
            String method = sobj.getMethodName();
            String action = sobj.getActionName();
            SoapObject request = new SoapObject(WSDL_TARGET_NAMESPACE, method);
            SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
            envelope.dotNet = true;

            envelope.setOutputSoapObject(request);
            request.addProperty("PhNo", ph);
            request.addProperty("VersionNo", ver);
            request.addProperty("IMEI", IMEI);
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
//


            androidHttpTransport.call(action, envelope);

            SoapObject response = (SoapObject) envelope.bodyIn;
            SoapObject object1 = (SoapObject) response.getProperty(0);
            SoapObject obj2 = (SoapObject) object1.getProperty(0);
            int prop = obj2.getPropertyCount();
            list.add(obj2.getProperty(1).toString());
            list.add(obj2.getProperty(2).toString());
            list.add(obj2.getProperty(4).toString());
            list.add(obj2.getProperty(7).toString());
            list.add(obj2.getProperty(8).toString());
            list.add(obj2.getProperty(10).toString());


            Log.e("value of result", " result" + response);
        } catch (Exception e) {

            System.out.println(e + "this is exception");
        }
        return list;

    }

    public void setOnOnNetworkCallListener(OnNetworkCallListener listener) {
        __listener = listener;
    }

    public interface OnNetworkCallListener {
        public abstract void onCompleted(boolean status, JSONObject jsonObject);
    }

    public static boolean checkNetworkStatus(final Context context) {
        final ConnectivityManager connMgr = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting()) {
            //Toast.makeText(context, "Network Online", Toast.LENGTH_LONG).show();
            if (internetConnectionAvailable(5000)){
                return true;
            }else {
                return false;
            }
        } else if (mobile.isConnectedOrConnecting()) {
            if (internetConnectionAvailable(5000)){
                return true;
            }else {
                return false;
            }
        } else {
            Toast.makeText(context, "No Network connection available", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    public void NoInternetAlert(final Context context, final String message, final String urlstr, final JSONObject jdata) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(context);
        dialog.setTitle("Please check your internet connection");
        final AlertDialog alertDialog= dialog.create();
        alertDialog.setButton(Dialog.BUTTON_POSITIVE, "Retry", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                postJsonResponse(urlstr,jdata,context,message);
                alertDialog.dismiss();
            }
        });
        if (message.length()>0) {
            alertDialog.setButton(Dialog.BUTTON_NEGATIVE, "Send Message", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setData(Uri.parse("smsto:51969")); // This ensures only SMS apps respond
                    intent.putExtra("sms_body", "WB EL " + message);
                    context.startActivity(intent);
                    alertDialog.dismiss();
                }
            });
        }
        alertDialog.show();
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
           // System.out.println("PollingDetails.decryptIt1452: "+value+" Decrypted Value "+decrypedValue);
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
