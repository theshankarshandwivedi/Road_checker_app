package com.example.roadchecker;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class ForegroundService extends Service implements SensorEventListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 123;
    private double latitude = 0.0; // Define as class fields
    private double longitude = 0.0;

    private FusedLocationProviderClient fusedLocationClient;
    private DatabaseReference DataRef;
    private DatabaseReference databaseRef;

    private LocationCallback locationCallback;
    private boolean isTracking = false;

    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "ForegroundServiceChannel";


    @Override
    public void onCreate() {
        super.onCreate();
        try {

            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


        } catch (Exception e) {
            e.printStackTrace();
            // Handle any exceptions that might occur during initialization
            Log.e("ForegroundService", "Error initializing fusedLocationClient: " + e.getMessage());
        }
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPreferences = getSharedPreferences("my_shared_prefs", MODE_PRIVATE);
        String key = sharedPreferences.getString("key", "not working!!!");

        Log.d("key", "onStartCommand: "+key);
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Tracking")
                .setContentText("Tracking your location")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        } else {
            Toast.makeText(this, "Sensor service not available", Toast.LENGTH_SHORT).show();
        }

        databaseRef = FirebaseDatabase.getInstance().getReference();

        DataRef = databaseRef.child("data").child(key);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdates();

        // Unregister the accelerometer and gyroscope sensor listeners
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.unregisterListener(this, accelerometer);
            }
        }
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Foreground Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }


    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            isTracking = false;
        }
    }



    @Override
    public void onSensorChanged(SensorEvent event) {
        double xValue=0, yValue=0, zValue=0;
        double maxValue =0.0d;
        String currentDateTime="";
        if (event != null) {
            Sensor sensor = event.sensor;
            xValue = event.values[0];
            yValue = event.values[1];
            zValue = event.values[2];
            maxValue = (xValue >= yValue) ? (Math.max(xValue, zValue)) : Math.max(yValue, zValue);
            if (maxValue >= 2.0d) {
                System.out.println("Service Started");

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    LocalDateTime now = null;
                    now = LocalDateTime.now();
                    DateTimeFormatter formatter = null;
                    formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                    currentDateTime = now.format(formatter);
                    System.out.println("Date and time updated");
                }
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
                fusedLocationClient.getLastLocation()
                        .addOnSuccessListener(new OnSuccessListener<Location>() {
                            @Override
                            public void onSuccess(Location location) {
                                if (location != null) {
                                    latitude = location.getLatitude();
                                    longitude = location.getLongitude();
                                }
                            }
                        });

                Data data = new Data(latitude, longitude, xValue, yValue, zValue, maxValue);
                DataRef.child(currentDateTime).setValue(data);
                System.out.println("INSERTED");

            }
            Intent intent = new Intent("data-update");
            intent.putExtra("latitude", latitude);
            intent.putExtra("longitude", longitude);
            intent.putExtra("xValue", xValue);
            intent.putExtra("yValue", yValue);
            intent.putExtra("zValue", zValue);
            intent.putExtra("maxValue", maxValue);
            intent.putExtra("time", currentDateTime);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);


        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}

