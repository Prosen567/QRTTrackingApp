package com.ceo.example.qrttracking;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ceo.example.qrttracking.Interface.OnItemClickListener;
import com.ceo.example.qrttracking.adapter.SearchAdapter;
import com.ceo.example.qrttracking.data.PartInfo;
import com.ceo.example.qrttracking.mapdirectiondata.DirectionResponses;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.ceo.example.qrttracking.LocationTable.LocationDetails;
import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.maps.android.PolyUtil;
import com.shockwave.pdfium.PdfDocument;

import org.json.JSONException;
import org.json.JSONObject;
import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapPrimitive;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;

import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;
import retrofit2.http.Query;

public class MainActivity extends FragmentActivity implements View.OnClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapReadyCallback, OnPageChangeListener, OnLoadCompleteListener {
    private static final long MAX_WAIT_TIME = 5000;
    private LocationRequest mLocationRequest;
    private Location mLastLocation;
    private Marker mCurrLocationMarker;
    private static final long UPDATE_INTERVAL = 20000;
    private static final long FASTEST_UPDATE_INTERVAL = 10000;
    private GoogleApiClient mGoogleApiClient;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private FusedLocationProviderClient mFusedLocationClient;
    Button btnStop, btnLogout;
    FrameLayout btnStart;
    TextView tvCurrentLocation, tvDistName, tvpcNo, tvPcName, tvassemblyId, tvassemblyName, tvSectorRole, tvsectorId, tvVersion;
    GoogleMap mGoogleMap;
    ArrayAdapter<String> arrayAdapter;
    SupportMapFragment supportMapFragment;
    ImageView ivPageView, btnSync;
    FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;
    public static final String SAMPLE_FILE = "sop.pdf";
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    EditText et_searchlocation;
    RecyclerView rc_locationlist;
    private ArrayList<PartInfo> dataList;
    private SearchAdapter searchAdapter;
    int lastpos = -1;
    boolean search = false;
    Polyline previousPolyline = null;
    private LatLng startLocation;
    private LatLng endLocation;
    private LatLng current_Location;
    private Location previousLocation;

    public static MainActivity getInstance() {
        return new MainActivity();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!checkPermissions()) {
            requestPermissions();
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnLogout = findViewById(R.id.btnLogout);
        btnSync = findViewById(R.id.btnSync);
        tvCurrentLocation = findViewById(R.id.tvCurrentLocation);
        tvDistName = findViewById(R.id.tvDistName);
        tvpcNo = findViewById(R.id.tvpcNo);
        tvPcName = findViewById(R.id.tvPcName);
        ivPageView = findViewById(R.id.ivPageView);
        tvassemblyId = findViewById(R.id.tvassemblyId);
        tvassemblyName = findViewById(R.id.tvassemblyName);
        tvsectorId = findViewById(R.id.tvsectorId);
        tvSectorRole = findViewById(R.id.tvSectorRole);
        tvVersion = findViewById(R.id.tvVersion);
        et_searchlocation = findViewById(R.id.et_search);
        rc_locationlist = findViewById(R.id.rc_locationlist);
        /*if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "sectorname", "").equals("")) {
            tvSecName.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "sectorname", ""));
        }*/
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dist_name", "").equals("")) {
            tvDistName.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dist_name", ""));
        }
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "acno", "").equals("")) {
            tvassemblyId.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "acno", ""));
        }
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "ac_name", "").equals("")) {
            tvassemblyName.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "ac_name", ""));
        }
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "secno", "").equals("")) {
            tvsectorId.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "secno", "") + " - " + HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "mobileno", ""));
        }
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "person_role", "").equals("")) {
            tvSectorRole.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "person_role", "") + " :   ");
        }
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "pc_no", "").equals("")) {
            tvpcNo.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "pc_no", ""));
        }
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "pc_name", "").equals("")) {
            tvPcName.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "pc_name", ""));
        }
        tvVersion.setText("Ver:  " + getVersion());
        arrayAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.select_dialog_singlechoice);

        //btnStop.startAnimation(startAnimation());
        // btnStart.startAnimation(startAnimation());

        supportMapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.googleMap);
        supportMapFragment.getMapAsync(this);

        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        buildGoogleApiClient();
        if (mGoogleApiClient.isConnected()) {
            if (isServiceRunning(LocationService.class)) {
                System.out.println("MainActivity.onCreate");
                stopService(new Intent(this, LocationService.class));
                startLocationService();
            }
        }
        if (HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this, "isIssueRaised", false)) {
            btnStart.setVisibility(View.GONE);
            btnStart.clearAnimation();
            btnStop.setVisibility(View.VISIBLE);
            // btnStop.startAnimation(startAnimation());
        } else {
            btnStop.setVisibility(View.GONE);
            btnStop.clearAnimation();
            btnStart.setVisibility(View.VISIBLE);
            //btnStart.startAnimation(startAnimation());
        }
        // sendOfflineData();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //  polylinePoints = new ArrayList<>();


        mLocationRequest = LocationRequest.create();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(20 * 1000);
      //  mLocationRequest.setFastestInterval(5000); // 5 seconds
        if (!HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "currentlocationText", "").equalsIgnoreCase("")) {
            tvCurrentLocation.setText(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "currentlocationText", ""));
        }
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

            //    String lat = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dest_lat", "");

              ///  String lon = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dest_lon", "");

                float distance= 0;

                for (Location location : locationResult.getLocations()) {

                    HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "lat", String.valueOf(location.getLatitude()));
                    HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "lon", String.valueOf(location.getLongitude()));

                    current_Location = new LatLng(location.getLatitude(), location.getLongitude());


                    if ( mGoogleMap!= null) {

                        mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current_Location, 15));


                    }



                    if (previousLocation != null) {
                        distance = location.distanceTo(previousLocation);
                        // Compare distance, or do other comparison with previous locatio

                        if (distance > 50) {

                            if (HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dest_lat", "").length() > 0 && HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dest_lon", "").length() > 0) {

                                getupdatedpolyline();

                            }


                        }else{

                            Toast.makeText(MainActivity.this, "Distance from previous location: " + distance + " meters", Toast.LENGTH_SHORT).show();
                        }

                    }
                    previousLocation = location;





//                    if (startLocation == null) {
//
//                      //  startLocation = new LatLng(location.getLatitude(), location.getLongitude());
//
//                         HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "lat", String.valueOf(location.getLatitude()));
//                         HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "lon", String.valueOf(location.getLongitude()));
//
//                      //  mGoogleMap.addMarker(new MarkerOptions().position(startLocation).title("Start"));
//                    } else {
//                        endLocation = new LatLng(Double.parseDouble(lat), Double.parseDouble(lon));
//                      //  draw_Polyline(startLocation, endLocation);
//                    }
                }





            }
        };
        /*final ValueAnimator animator = ValueAnimator.ofFloat(1.0f, 0.2f);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateInterpolator());
        animator.setDuration(9000L);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                final float progress = (float) animation.getAnimatedValue();
                final float widthPC = tvPcName.getWidth();
                final float widthAC = tvassemblyName.getWidth();
                final float widthSN = tvSecName.getWidth();
                final float translationX = widthAC * progress;
                final float translationX1 = widthSN * progress;
                final float translationX2 = widthPC * progress;
                //tvassemblyName.setTranslationX(translationX);
                tvassemblyName.setTranslationX(translationX - widthAC);
                tvSecName.setTranslationX(translationX1 - widthSN);
                tvPcName.setTranslationX(translationX2 - widthPC);
            }
        });
        animator.start();*/


        String jsonString = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "part_info", "");


//        JSONArray jsonArray= null;
//
//        try {
//            jsonArray = new JSONArray(jsonString);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }

        Gson gson = new Gson();

        Type type = new TypeToken<ArrayList<PartInfo>>() {
        }.getType();

        dataList = gson.fromJson(jsonString, type);

        Log.d("dataList", dataList.toString());

        searchAdapter = new SearchAdapter(dataList, new SearchAdapter.OnItemClickListener() {

            @Override
            public void onItemClick(PartInfo item) {


                String destinationlatitude = "";
                String destinationlongitude = "";

                et_searchlocation.setText(item.getPsName());
                destinationlatitude = item.getLat();
                destinationlongitude = item.getLng();

                rc_locationlist.setVisibility(View.GONE);

              //  getpolylineResponse();

                HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "dest_lat", destinationlatitude);

                HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "dest_lon", destinationlongitude);


                String currentLatitude = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "lat", "");
                String currentLongitude = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "lon", "");
                // LatLng currentLocation = new LatLng(Double.parseDouble(currentLatitude), Double.parseDouble(currentLongitude));


//               String destinationlatitude = dataList.get(position).getLat();
//               String destinationlongitude = dataList.get(position).getLng();

                if (!"".equals(destinationlatitude) && !"".equals(destinationlongitude)) {

                    LatLng destinationlocation = new LatLng(Double.parseDouble(destinationlatitude), Double.parseDouble(destinationlongitude));

                    MarkerOptions markerMonas = new MarkerOptions()
                            .position(destinationlocation)
                            .title("Destination");

                    mGoogleMap.addMarker(markerMonas);
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationlocation, 11.6f));

                    String fromFKIP = currentLatitude + "," + currentLongitude;

                    String toMonas = destinationlatitude + "," + destinationlongitude;

                    ApiServices apiServices = RetrofitClient.apiServices(MainActivity.this);

                    apiServices.getDirection(fromFKIP, toMonas, getString(R.string.google_maps_key))
                            .enqueue(new Callback<DirectionResponses>() {
                                @Override
                                public void onResponse(@NonNull Call<DirectionResponses> call, @NonNull retrofit2.Response<DirectionResponses> response) {

                                    Log.d("response===>", response.toString());

                                    drawPolyline(response, destinationlocation);

                                }

                                @Override
                                public void onFailure(@NonNull Call<DirectionResponses> call, @NonNull Throwable t) {
                                    Log.e("anjir error", t.getLocalizedMessage());
                                }
                            });


                }


                //  drawPolyline(currentLocation,destinationlocation);

            }


        });

        rc_locationlist.setLayoutManager(new LinearLayoutManager(this));

        rc_locationlist.setAdapter(searchAdapter);


        et_searchlocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {


            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                if (s.length() > 0) {
                    rc_locationlist.setVisibility(View.VISIBLE);
                    searchAdapter.filter(s.toString());
                }


            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });


        et_searchlocation.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Check if the event is ACTION_UP (finger released)
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Clear the EditText text
                    et_searchlocation.setText("");
                    rc_locationlist.setVisibility(View.GONE);
                }
                // Return false to allow other touch events to be processed
                return false;
            }
        });


        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                logout();
            }
        });
        btnSync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkOfflineData();
                if (checkNetworkStatus(MainActivity.this)) {
                    if (HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this, "isIDSent", false)) {
                        HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "isIDSent", false);
                    } else {
                        Toast.makeText(MainActivity.this, "The location is syncing", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(MainActivity.this, "No Network connection available", Toast.LENGTH_LONG).show();
                }
                /*if (__listener != null)
                    __listener.onCompleted(true);*/
            }
        });

        setOnMainActivityLocationListener(new OnMainActivityLocationListener() {
            @Override
            public void onCompleted(boolean status, String id) {
                if (status) {
                    HelperSharedPreferences.putSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", (HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0)) + 1);
                    HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "isIDSent", false);
                    checkOfflineData();
                }
            }
        });
        ivPageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PDFdisplayFromAsset(SAMPLE_FILE);
            }
        });
    }

    private void getpolylineResponse() {

    }

    private void draw_Polyline(LatLng startLocation, LatLng endLocation) {


        if (previousPolyline != null) {
            previousPolyline.remove();
        }

        PolylineOptions polylineOptions = new PolylineOptions()
                .add(startLocation)
                .add(endLocation)
                .width(5)
                .color(Color.RED);

        previousPolyline = mGoogleMap.addPolyline(polylineOptions);


    }


    private void drawPolyline(@NonNull retrofit2.Response<DirectionResponses> response, LatLng destinationlocation) {
        if (response.body() != null) {

            Log.d("polyresponse==>", response.body().toString());


            if (response.body().getRoutes()!= null) {

                String shape = response.body().getRoutes().get(0).getOverviewPolyline().getPoints();
                PolylineOptions polyline = new PolylineOptions()
                        .addAll(PolyUtil.decode(shape))
                        .width(8f)
                        .color(Color.RED);

                mGoogleMap.clear();

                MarkerOptions markerMonas = new MarkerOptions()
                        .position(destinationlocation)
                        .title("Destination");

                mGoogleMap.addMarker(markerMonas);
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationlocation, 11.6f));


                Polyline newpolyline;


                if (previousPolyline == null) {

                    newpolyline = mGoogleMap.addPolyline(polyline);

                } else {


                    previousPolyline.remove();

                    newpolyline = mGoogleMap.addPolyline(polyline);
                }

                previousPolyline = newpolyline;


            } else {


                Toast.makeText(MainActivity.this, "No route found", Toast.LENGTH_LONG).show();

            }


        }
    }


    private static class RetrofitClient {
        static ApiServices apiServices(Context context) {
            Retrofit retrofit = new Retrofit.Builder()
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl(context.getResources().getString(R.string.base_url))
                    .build();

            return retrofit.create(ApiServices.class);
        }
    }


    private interface ApiServices {
        @GET("maps/api/directions/json")
        Call<DirectionResponses> getDirection(@Query("origin") String origin,
                                              @Query("destination") String destination,
                                              @Query("key") String apiKey);
    }


    private void checkOfflineData() {
        if (checkNetworkStatus(MainActivity.this)) {
            if (!HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this, "isIDSent", false)) {
                Hashtable<String, String> hashtableList = new LocationDetails(MainActivity.this).getSpecificData(MainActivity.this, "0", HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0));

                if (hashtableList != null) {
                    if (Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0")) > 0) {
                        System.out.println("LocationService.Tablesize11  " + HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0"));
                        if (HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0) < Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0"))) {
                            //System.out.println("LocationMonitoringService.hashtableList2255:  " + hashtableList.get(HelperSharedPreferences.getSharedPreferencesInt(LocationService.this,"fetchlocationIndex",0)));
                            int index;
                            if (HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0) > 0) {
                                index = HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0) - 1;
                            } else {
                                index = HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0);
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
                            System.out.println("LocationService.Tablesize: " + HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0") + "  " + HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0));
                            networkCountOffline(lat, lon, id, upload_date, secmobile, sos_update);
                            //SendLocationtoServer(dist, ac, secno, secmobile, imei, lat, lon, upload_date, flag, session, id);
                        } else {
                            System.out.println("LocationService.fetchlocationIndex  11");
                            HelperSharedPreferences.putSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0);
                            /*if (HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0) > Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0"))) {

                            }*/
                        }
                    }
                } else {
                    hashtableList = new LocationDetails(MainActivity.this).getSpecificData(MainActivity.this, "0", HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0));

                }
            }
        } else {
            Toast.makeText(MainActivity.this, "No Network connection available", Toast.LENGTH_LONG).show();
        }
    }

    public OnMainActivityLocationListener __listener = null;

    public void setOnMainActivityLocationListener(OnMainActivityLocationListener listener) {
        __listener = listener;
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(MainActivity.class.getSimpleName(), String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    public interface OnMainActivityLocationListener {
        public abstract void onCompleted(boolean status, String id);
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

        HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLatitude", lat);
        HelperSharedPreferences.putSharedPreferencesString(this, "onlinePrevLongitude", lon);
        System.out.println("BackgroundTimeService.HISTORY  :  " + jsonObject);

        HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "isIDSent", true);
        // new LocationDetails(LocationService.this).updateUploadingData(id, "1");

        System.out.println("LocationService.onResponse:  status " + id);
        final Request<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST, /*"http://10.173.46.82/track/receive/"  wbceo.in, track.southindia.cloudapp.azure.com*/"http://wbceo.in/qrttrackapi/receive_history", jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        System.out.println("Background Service .onResponse HISTORY: " + response);
                        try {
                            JSONObject data = response.getJSONObject("data");
                            String receive_status = data.getString("receive_status");
                            if (receive_status.equalsIgnoreCase("Successful")) {
                                new LocationDetails(MainActivity.this).deleteData(id);

                                HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "invalidVersion", false);

                                System.out.println("LocationService11.onResponse:  status " + id + "  " + HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0));
                            } else if (receive_status.equalsIgnoreCase("Wrong details")) {
                                logout();
/*
                                if (__listener != null)
                                    __listener.onCompleted(false, id);*/
                                HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "invalidVersion", true);
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
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 24,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // setTrackingStatus(intent.getIntExtra(getString(R.string.status), 0));
        }
    };

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

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    Dialog dialog = null;

    private void PDFdisplayFromAsset(String assetFileName) {
        dialog = new Dialog(this, android.R.style.Theme_Black);
        View views = LayoutInflater.from(this).inflate(R.layout.activity_pdf_view, null);
        pdfView = views.findViewById(R.id.pdfView);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawableResource(R.color.grey);
        dialog.setContentView(views);
        dialog.show();
        pdfFileName = assetFileName;

        pdfView.fromAsset(pdfFileName)
                .defaultPage(pageNumber)
                .enableSwipe(true)

                .swipeHorizontal(false)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(LocationService.STATUS_INTENT));
        if (HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this, "invalidVersion", false)) {
            VersionAlert("To proceed further, you have to update the version of your application", "ERROR !!", MainActivity.this);
        }
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mFusedLocationClient.removeLocationUpdates(locationCallback);
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
     //   stopLocationUpdates();
        super.onPause();
    }

    private Animation startAnimation() {
        Animation mAnimation = new AlphaAnimation(1f, 0.2f);
        mAnimation.setDuration(1000);
        mAnimation.setInterpolator(new LinearInterpolator());
        mAnimation.setRepeatCount(Animation.INFINITE);
        mAnimation.setRepeatMode(Animation.REVERSE);
        return mAnimation;
    }

    private boolean checkPermissions() {
        int permissionState = ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        return permissionState == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onStart() {

        /*LocationService.getInstance().setOnVersionUpdateListener(new LocationService.OnVersionUpdateListener() {
            @Override
            public void onCompleted(boolean status) {
                if (status){

                }
            }
        });*/
        super.onStart();
        if (HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this, "invalidVersion", false)) {
            VersionAlert("To proceed further, you have to update the version of your application", "ERROR !!", MainActivity.this);
        }
    }


    public void VersionAlert(String message, String title, Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.Theme_AppCompat_Dialog_Alert);
        builder.setMessage(message);
        builder.setTitle(title);
        builder.setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(MainActivity.this, Downloadapk.class));
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
        stopService(new Intent(this, LocationService.class));
        // startActivity(new Intent(this, UserLogin.class));
        finish();

    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @SuppressLint("ResourceType")
    private void requestPermissions() {
       /* boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);

        // Provide an additional rationale to the user. This would happen if the user denied the
        // request previously, but didn't check the "Don't ask again" checkbox.
        if (shouldProvideRationale) {
            Log.i("TAG", "Displaying permission rationale to provide additional context.");
            Snackbar.make(
                    findViewById(R.id.rlMaincontainer),
                    "Allow location permission from settings",
                    Snackbar.LENGTH_INDEFINITE)
                    .setAction("Ok", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Request permission
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_PERMISSIONS_REQUEST_CODE);
                        }
                    })
                    .show();
        } else {
            Log.i("TAG", "Requesting permission");
            // Request permission. It's possible this can be auto answered if device policy
            // sets the permission in a given state or the user denied the permission
            // previously and checked "Never ask again".
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }*/

        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                REQUEST_PERMISSIONS_REQUEST_CODE);
    }

    private void buildGoogleApiClient() {
        if (mGoogleApiClient != null) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .enableAutoManage(this, this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest(mGoogleApiClient);
    }

    private void createLocationRequest(final GoogleApiClient googleApiClient) {
        mLocationRequest = new LocationRequest();

        mLocationRequest.setInterval(UPDATE_INTERVAL);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL);
        //mLocationRequest.setSmallestDisplacement(0.01f);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        // Sets the maximum time when batched location updates are delivered. Updates may be
        // delivered sooner than this interval.
        mLocationRequest.setMaxWaitTime(MAX_WAIT_TIME);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        //**************************
        builder.setAlwaysShow(true); //this is the key ingredient
        //**************************

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                final LocationSettingsStates state = result.getLocationSettingsStates();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location
                        // requests here.
                        if (googleApiClient.isConnected()) {
                            if (!isServiceRunning(LocationService.class)) {
                                System.out.println("MainActivity.onResult");
                                //stopService(new Intent(MainActivity.this, LocationService.class));
                                startLocationService();
                            }
                        }
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        // Location settings are not satisfied. But could be fixed by showing the user
                        // a dialog.
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(
                                    MainActivity.this, 1000);
                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        // Location settings are not satisfied. However, we have no way to fix the
                        // settings so we won't show the dialog.
                        break;
                }
            }
        });
    }

    private void startLocationService() {
        // Before we start the service, confirm that we have extra power usage privileges.
        /*PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, new Intent(this, LocationService.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else {
            startService(new Intent(this, LocationService.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
    }

    public static double distance(double lat1, double lat2, double lon1, double lon2) {

        final double R = 6371008; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c; // convert to meters

        return distance;
    }

    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.i("Service status", "Running");
                return true;
            }
        }
        Log.i("Service status", "Not running");
        return false;
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length <= 0) {
                // If user interaction was interrupted, the permission request is cancelled and you
                // receive empty arrays.
                Log.i("TAG", "User interaction was cancelled.");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission was granted. Kick off the process of building and connecting
                // GoogleApiClient.
                buildGoogleApiClient();

                mGoogleMap.setMyLocationEnabled(true);
            } else {
                // Permission denied.

                // Notify the user via a SnackBar that they have rejected a core permission for the
                // app, which makes the Activity useless. In a real app, core permissions would
                // typically be best requested during a welcome-screen flow.

                // Additionally, it is important to remember that a permission might have been
                // rejected without asking the user for permission (device policy or "Never ask
                // again" prompts). Therefore, a user interface affordance is typically implemented
                // when permissions are denied. Otherwise, your app could appear unresponsive to
                // touches or interactions which have required permissions.
                Snackbar.make(
                                findViewById(R.id.rlMaincontainer),
                                "Location Permission Denied:\nAllow permission from settings",
                                Snackbar.LENGTH_INDEFINITE)
                        .setAction("Settings", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                // Build intent that displays the App settings screen.
                                Intent intent = new Intent();
                                intent.setAction(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package",
                                        BuildConfig.APPLICATION_ID, null);
                                intent.setData(uri);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                            }
                        })
                        .show();
            }
        }
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

    private Location getlastKnownLocation() {
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                Criteria criteria = new Criteria();
                String bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true)).toString();


                Location lastKnownLocationGPS = locationManager.getLastKnownLocation(bestProvider);
                System.out.println("MainActivity.lastKnownLocationGPS  " + lastKnownLocationGPS.getLatitude());
                Location lastKnownLocationNETWORK = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (lastKnownLocationGPS != null) {
                    return lastKnownLocationGPS;
                } else {
                    Location loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                /*System.out.println("1::" + loc);
                if (loc.getLatitude()==null)
                System.out.println("2::" + loc.getLatitude());*/
                    System.out.println("MainActivity.getlastKnownLocation1 " + loc.getLatitude());
                    return loc;
                }
            } else {
                System.out.println("MainActivity.getlastKnownLocation 2");
                return null;
            }

        } else {
            System.out.println("MainActivity.getlastKnownLocation 3");
            return null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Are you sure ?");
                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "isSOSUpdate", "1");
                        if (getLastBestLocation() != null) {
                            if (checkNetworkStatus(MainActivity.this)) {
                                SOSCall(String.valueOf(getLastBestLocation().getLatitude()), String.valueOf(getLastBestLocation().getLongitude()), currentDateTime(), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "isSOSUpdate", "0"));
                            } else {
                                new LocationDetails(MainActivity.this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "IMEI", ""), String.valueOf(getLastBestLocation().getLatitude()), String.valueOf(getLastBestLocation().getLongitude()), "", "s", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "sessioncount", 0)), "0", HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "isSOSUpdate", "0"));
                            }
                            btnStart.setVisibility(View.GONE);
                            btnStart.clearAnimation();
                            btnStop.setVisibility(View.VISIBLE);
                            //btnStop.startAnimation(startAnimation());
                            HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "isIssueRaised", true);
                            dialog.dismiss();
                        }
                    }
                });
                builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
                break;
            case R.id.btnStop:
                HelperSharedPreferences.putSharedPreferencesString(this, "isSOSUpdate", "0");
                if (getLastBestLocation() != null) {
                    if (checkNetworkStatus(MainActivity.this)) {
                        SOSCall(String.valueOf(getLastBestLocation().getLatitude()), String.valueOf(getLastBestLocation().getLongitude()), currentDateTime(), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "isSOSUpdate", "0"));
                    } else {
                        new LocationDetails(this).insertDataDistMast(HelperSharedPreferences.getSharedPreferencesString(this, "distno", ""), HelperSharedPreferences.getSharedPreferencesString(this, "acno", ""), HelperSharedPreferences.getSharedPreferencesString(this, "secno", ""), HelperSharedPreferences.getSharedPreferencesString(this, "mobileno", ""), HelperSharedPreferences.getSharedPreferencesString(this, "IMEI", ""), String.valueOf(getLastBestLocation().getLatitude()), String.valueOf(getLastBestLocation().getLongitude()), "", "s", String.valueOf(HelperSharedPreferences.getSharedPreferencesInt(this, "sessioncount", 0)), "0", HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "isSOSUpdate", "0"));
                    }
                    System.out.println("MainActivity.onCompleted SOS:  ");
                    btnStop.setVisibility(View.GONE);
                    btnStop.clearAnimation();
                    btnStart.setVisibility(View.VISIBLE);
                    //btnStart.startAnimation(startAnimation());
                    HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "isIssueRaised", false);


                }
                break;
        }
    }

    private String currentDateTime() {
        DateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        dateFormatter.setLenient(false);
        Date today = new Date();
        return dateFormatter.format(today);
    }

    public void SOSCall(final String lat, final String lon, String uploadDate, String mobile, String sos_update) {
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

        //System.out.println("LocationService.onResponse:  status " + id);
        final Request<JSONObject> jsonRequest = new JsonObjectRequest(Request.Method.POST, "http://wbceo.in/qrttrackapi/receive", jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {

                        System.out.println("Background Service .onResponse LOCATION: " + response);
                        try {
                            JSONObject data = response.getJSONObject("data");
                            String receive_status = data.getString("receive_status");
                            if (receive_status.equalsIgnoreCase("Successful")) {
                                System.out.println("LocationService11.onResponse:  networkCountONLINE Successfull " + lat + "  " + lon);

                                HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "invalidVersion", false);
                            } else if (receive_status.equalsIgnoreCase("Wrong details")) {
                                HelperSharedPreferences.putSharedPreferencesBoolean(MainActivity.this, "invalidVersion", true);
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
                System.out.println("NetworkCall1.onErrorResponse: " + error);
            }
        });
        // requestQueue.add(jsonRequest);
        App.getInstance().addToRequestQueue(jsonRequest);
        jsonRequest.setRetryPolicy(new DefaultRetryPolicy(DefaultRetryPolicy.DEFAULT_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

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
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

        Log.d("lat===>", String.valueOf(location.getLatitude()));
        Log.d("lon===>", String.valueOf(location.getLongitude()));

        HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "lat", String.valueOf(location.getLatitude()));

        HelperSharedPreferences.putSharedPreferencesString(MainActivity.this, "lon", String.valueOf(location.getLongitude()));


        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Your Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

        CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngZoom(latLng, 15);
        mGoogleMap.moveCamera(cameraPosition);
        mGoogleMap.animateCamera(cameraPosition);
        Hashtable<String, String> hashtableList = new LocationDetails(MainActivity.this).getSpecificData(MainActivity.this, "0", HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0));

        if (HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this, "isIDSent", false)) {
            btnSync.setVisibility(View.VISIBLE);
        } else {
            btnSync.setVisibility(View.GONE);
        }
        if (hashtableList != null) {
            if (Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0")) > 0) {
                System.out.println("LocationService.Tablesize11  " + HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0"));
                if (HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0) < Integer.parseInt(HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "tablesize", "0"))) {
                    btnSync.setVisibility(View.VISIBLE);
                } else {
                    btnSync.setVisibility(View.GONE);
                }
            } else {
                hashtableList = new LocationDetails(MainActivity.this).getSpecificData(MainActivity.this, "0", HelperSharedPreferences.getSharedPreferencesInt(MainActivity.this, "fetchlocationIndex", 0));
                btnSync.setVisibility(View.GONE);
            }
        } else {
            btnSync.setVisibility(View.GONE);
        }
        //mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

        //move map camera
        // mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,4));





    }

    private void getupdatedpolyline() {

        Log.d("updatepoly", "updatepolyline");

        String destinationlatitude = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dest_lat", "");

        String destinationlongitude = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "dest_lon", "");


        String currentLatitude = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "lat", "");
        String currentLongitude = HelperSharedPreferences.getSharedPreferencesString(MainActivity.this, "lon", "");

        // LatLng currentLocation = new LatLng(Double.parseDouble(currentLatitude), Double.parseDouble(currentLongitude));

//         String destinationlatitude = dataList.get(position).getLat();
//         String destinationlongitude = dataList.get(position).getLng();


        if (!"".equals(destinationlatitude) && !"".equals(destinationlongitude)) {

            LatLng destinationlocation = new LatLng(Double.parseDouble(destinationlatitude), Double.parseDouble(destinationlongitude));

            MarkerOptions markerMonas = new MarkerOptions()
                    .position(destinationlocation)
                    .title("Destination");

            mGoogleMap.addMarker(markerMonas);
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationlocation, 11.6f));

            String fromFKIP = currentLatitude + "," + currentLongitude;

            String toMonas = destinationlatitude + "," + destinationlongitude;

            ApiServices apiServices = RetrofitClient.apiServices(MainActivity.this);
            apiServices.getDirection(fromFKIP, toMonas, getString(R.string.google_maps_key))
                    .enqueue(new Callback<DirectionResponses>() {
                        @Override
                        public void onResponse(@NonNull Call<DirectionResponses> call, @NonNull retrofit2.Response<DirectionResponses> response) {

                            Log.d("response===>", response.toString());

                            drawPolyline(response, destinationlocation);

                        }

                        @Override
                        public void onFailure(@NonNull Call<DirectionResponses> call, @NonNull Throwable t) {
                            Log.e("anjir error", t.getLocalizedMessage());
                        }
                    });


        }

    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        try {
            // Customise the styling of the base map using a JSON object defined
            // in a raw resource file.
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(
                            this, R.raw.style));

            if (!success) {
                Log.e(MainActivity.class.getSimpleName(), "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e(MainActivity.class.getSimpleName(), "Can't find style. Error: ", e);
        }
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);


            } else {
                //Request Location Permission
                requestPermissions();
            }
        } else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }

        startLocationUpdates();
    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        mFusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null);
    }

    private static LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(20000); // 10 seconds
       // locationRequest.setFastestInterval(5000); // 5 seconds
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }

    private void stopLocationUpdates() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
    }

    private static class SendLocationAsyncParams {
        String id, dist, ac, secno, secmobile, imei, lat, lon, upload_date, flag, session;
        boolean isStopped;

        SendLocationAsyncParams(String id, String dist, String ac, String secno, String secmobile, String imei, String lat, String lon, String upload_date, String flag, String session, boolean isStopped) {
            this.id = id;
            this.dist = dist;
            this.ac = ac;
            this.secno = secno;
            this.secmobile = secmobile;
            this.imei = imei;
            this.lat = lat;
            this.lon = lon;
            this.upload_date = upload_date;
            this.flag = flag;
            this.session = session;
            this.isStopped = isStopped;
        }
    }

    @Override
    protected void onDestroy() {
        /*if (HelperSharedPreferences.getSharedPreferencesBoolean(MainActivity.this,"isUserVerified",false)) {
            Intent restartService = new Intent("RestartService");
            sendBroadcast(restartService);
        }*/
        super.onDestroy();
    }

    class SendLocationAsync extends AsyncTask<SendLocationAsyncParams, Void, Void> {

        String id;
        boolean isStoppedasync = false;

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);


        }

        @Override
        protected Void doInBackground(MainActivity.SendLocationAsyncParams... params) {
            SendLocationtoServer(params[0].dist, params[0].ac, params[0].secno, params[0].secmobile, params[0].imei, params[0].lat, params[0].lon, params[0].upload_date, params[0].flag, params[0].session, params[0].id);
            id = params[0].id;
            isStoppedasync = params[0].isStopped;
            return null;
        }
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

    public final String WSDL_TARGET_NAMESPACE = "http://tempuri.org/";
    boolean isStopped = false;
    //public static final String URL = "http://10.173.46.87/wb-gis/GISService.asmx?WSDL";
    public static final String URL = "http://wbceo.in/wb-gis/GISService.asmx?WSDL";

    public void SendLocationtoServer(String dist, String ac, String secno, String secmobile, String imei, String lat, String lon, String upload_date, String flag, String session, String id) {

        //list = new ArrayList<>();
        String description = "";
        try {

            SoapProperties sobj = new SoapProperties();
            sobj.setActionName("InsertEVMTrackingEmergencyDetails");
            sobj.setMethodName("InsertEVMTrackingEmergencyDetails");
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
            HttpTransportSE androidHttpTransport = new HttpTransportSE(URL);
//


            androidHttpTransport.call(action, envelope);

            //SoapObject response = (SoapObject) envelope.bodyIn;
            //SoapObject response = (SoapObject) envelope.getResponse();
            SoapPrimitive response = (SoapPrimitive) envelope.getResponse();
            System.out.println("MainActivity.SoapObject.SendLocationtoServer emergency:  " + response);
            if (response.toString().equalsIgnoreCase("success")) {

                new LocationDetails(MainActivity.this).updateDataForEmergency(id, "1");
                System.out.println("SendLocationAsync.onPostExecute emergency: ");
            }

            Log.e("value of result", " result" + response);
            System.out.println("value of result emergency" + response);
        } catch (Exception e) {
            System.out.println(e + "this is exception");
        }

    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (!isServiceRunning(LocationService.class)) {
            System.out.println("MainActivity.onConnected");
            startLocationService();
        }
        HelperSharedPreferences.putSharedPreferencesBoolean(this, "servicerunning", true);
        long date = System.currentTimeMillis();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.ENGLISH);
        // SimpleDateFormat timeonly = new SimpleDateFormat("hh:mm");
        String dateString = sdf.format(date);
        System.out.println("MainActivity.btnStart.onClick:  " + dateString);
        String[] sDate = dateString.split("-");
        String day = sDate[0];
        String month = sDate[1];
        String year = sDate[2];

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mFusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations, this can be null.
                            if (location != null) {
                                //Place current location marker
                                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                                Log.d("lat",String.valueOf(location.getLatitude()));
                                Log.d("lon",String.valueOf(location.getLongitude()));

                                HelperSharedPreferences.putSharedPreferencesString(MainActivity.this,"lat",String.valueOf(location.getLatitude()));

                                HelperSharedPreferences.putSharedPreferencesString(MainActivity.this,"lon",String.valueOf(location.getLongitude()));


                                MarkerOptions markerOptions = new MarkerOptions();
                                markerOptions.position(latLng);
                                markerOptions.title("Your Position");
                                // markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));

                                // mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

                                CameraUpdate cameraPosition = CameraUpdateFactory.newLatLngZoom(latLng, 15);
                                mGoogleMap.moveCamera(cameraPosition);
                                mGoogleMap.animateCamera(cameraPosition);
                                // Logic to handle location object
                            }
                        }
                    });
        }

        Toast.makeText(this, "Connected", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection Suspended", Toast.LENGTH_SHORT).show();
        if (!mGoogleApiClient.isConnected()) {
            buildGoogleApiClient();
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!mGoogleApiClient.isConnected()) {
            buildGoogleApiClient();
        }
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }
}
