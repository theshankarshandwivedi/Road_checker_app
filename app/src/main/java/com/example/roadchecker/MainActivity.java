package com.example.roadchecker;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 999;
    private static final int BODY_SENSOR_PERMISSION_REQUEST_CODE = 1234;
    private Button startButton;
    private TextView Time;
    private TextView xaxis;
    private TextView yaxis;
    private TextView zaxis;
    private TextView Max;
    private TextView Latitude;
    private TextView Longitude;
    public String file = "File",enKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = findViewById(R.id.startButton);
        Time = findViewById(R.id.time);
        xaxis = findViewById(R.id.xaxis);
        yaxis = findViewById(R.id.yaxis);
        zaxis = findViewById(R.id.zaxis);
        Max = findViewById(R.id.max);
        Latitude = findViewById(R.id.latitude);
        Longitude = findViewById(R.id.longitude);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(startButton.getText().toString().equals("START")){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION, BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
                        checkPermission(Manifest.permission.BODY_SENSORS, BODY_SENSOR_PERMISSION_REQUEST_CODE);
                        requestPermission();
                    }
                }
                else if(startButton.getText().toString().equals("STOP")){
                    startButton.setText("START");
                    try {
                        stopForegroundService();
                        xaxis.setText(Double.toString(0.0d));
                        yaxis.setText(Double.toString(0.0d));
                        zaxis.setText(Double.toString(0.0d));
                        Max.setText(Double.toString(0.0d));
                        Latitude.setText(Double.toString(0.0d));
                        Longitude.setText(Double.toString(0.0d));
                        Time.setText(" ");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d("Button error", "error404");
                    }

                }
                else {
                    Toast.makeText(MainActivity.this, "Button not working", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    public void checkPermission(String permission, int requestCode)
    {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private void requestPermission() {
//        Log.d("permission", "requestPermission: Inside requestPermission");
        boolean hasBackgroundLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean hasBodySensorPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED;
//        Log.d("permission", "requestPermission: "+hasBackgroundLocationPermission);

        if (!hasBackgroundLocationPermission) {
//            Log.d("permission", "requestPermission: Inside if");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE);
        } else if (!hasBodySensorPermission) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BODY_SENSORS}, BODY_SENSOR_PERMISSION_REQUEST_CODE);
        } else {
            xaxis.setText(Double.toString(0.0d));
            yaxis.setText(Double.toString(0.0d));
            zaxis.setText(Double.toString(0.0d));
            Max.setText(Double.toString(0.0d));
            Latitude.setText(Double.toString(0.0d));
            Longitude.setText(Double.toString(0.0d));
            Time.setText(" ");
            startForegroundService();
            startButton.setText("STOP");
        }
    }


    private void startForegroundService() {
        File f = new File(file);
        enKey=readStringFromFile(file);
        if(enKey.length()<40) {
            writeStringToFile(file, makeAkey());
            enKey = readStringFromFile(file);
        }

        String key = encrypt(enKey);
        Log.d("key", "startForegroundService: "+key);
        SharedPreferences sharedPreferences = getSharedPreferences("my_shared_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("key", key);
        editor.apply();
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        startService(serviceIntent);
    }

    private void stopForegroundService() {
        Intent serviceIntent = new Intent(this, ForegroundService.class);
        stopService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode,
                permissions,
                grantResults);

        if (requestCode == BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Location Permission Granted", Toast.LENGTH_SHORT) .show();
            }
            else {
                Toast.makeText(MainActivity.this, "Location Permission Denied", Toast.LENGTH_SHORT) .show();
            }
        }
        else if (requestCode == BODY_SENSOR_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "Body sensor Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, "Body sensor Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            double latitude = intent.getDoubleExtra("latitude", 0.0);
            double longitude = intent.getDoubleExtra("longitude", 0.0);
            double xValue = intent.getDoubleExtra("xValue", 0.0);
            double yValue = intent.getDoubleExtra("yValue", 0.0);
            double zValue = intent.getDoubleExtra("zValue", 0.0);
            double max = intent.getDoubleExtra("maxValue", 0.0);
            String time = intent.getStringExtra("time");

            xaxis.setText(Double.toString(xValue));
            yaxis.setText(Double.toString(yValue));
            zaxis.setText(Double.toString(zValue));
            Max.setText(Double.toString(max));
            Latitude.setText(Double.toString(latitude));
            Longitude.setText(Double.toString(longitude));
            Time.setText(time);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(dataReceiver, new IntentFilter("data-update"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(dataReceiver);
    }

    private void writeStringToFile(String filename, String content) {
        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(content.getBytes());
            fos.close();
            Toast.makeText(this,"WRITTEN SUCCESSFULLY",Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    static String makeAkey() {
        StringBuilder str = new StringBuilder();
        int i = 50;
        Random random = new Random();
        while (i > 0) {

            char ch = (char) random.nextInt(256);
            char c = (ch<256)?(char)(ch+42):(char)((ch%256)+42);
            while (!Character.isLetterOrDigit(c+42) || c == '.' || c == '#' || c == '$' || c == '[' || c == ']' || c > 127||c=='/') {
                ch = (char) random.nextInt(256);
                c = (ch<256)?(char)(ch+42):(char)((ch%256)+42);
            }
            str.append(ch);
            i--;
        }
        return str.toString();
    }


    public void writeToInternalStorage(String filename, String data) {
        try {
            FileOutputStream fos = openFileOutput(filename, Context.MODE_PRIVATE);
            fos.write(data.getBytes());
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private String readStringFromFile(String filename) {
        String content = "";
        try {
            FileInputStream fis = openFileInput(filename);
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            content = new String(buffer);
            fis.close();
        } catch (IOException e) {
            Toast.makeText(this,"Failed to Read",Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return content;
    }
    static String encrypt(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int ascii = (int) c;
            ascii += 42;
            if (ascii > 256) {
                ascii = ascii % 256;
            }
            sb.append((char) ascii);
        }
        return sb.toString();
    }
    static String decrypt(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            int ascii = (int) c;
            ascii -= 42;
            if (ascii < 0) {
                ascii = 256 + ascii;
            }
            sb.append((char) ascii);
        }
        return sb.toString();
    }
    private void createFirebaseNode(String nodeName) {
        if(nodeName==null)return;
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child(nodeName);
//        databaseReference.setValue("Hello, World!"); // Set a sample value for the node
    }

}
