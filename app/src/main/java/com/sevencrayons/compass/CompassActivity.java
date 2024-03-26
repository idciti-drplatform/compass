package com.sevencrayons.compass;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class CompassActivity extends AppCompatActivity {

    private static final String TAG = "CompassActivity";
    private Compass compass;
    private ImageView compassView;
    private TextView sotwLabel;  // SOTW is for "side of the world"
    private float currentAzimuth;
    private SOTWFormatter sotwFormatter;
    private Button positionButton;
    private Button applyButton;
    private EditText distField;
    private EditText angleField;
    private EditText latField;
    private EditText lngField;
    private LocationManager locationManager;
    private Location currentLocation;
    private TextView latLabel;
    private TextView lngLabel;
    private TextView currentLoc;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compass);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        sotwFormatter = new SOTWFormatter();
        compassView = findViewById(R.id.main_image_dial);
        setupCompass();
        distField = findViewById(R.id.dist);
        angleField = findViewById(R.id.angle);
        latField = findViewById(R.id.lat);
        lngField = findViewById(R.id.lng);
        latLabel = findViewById(R.id.latlabel);
        lngLabel = findViewById(R.id.lnglabel);
        currentLoc = findViewById(R.id.currentlocStatus);
        applyButton = findViewById(R.id.apply_button);
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clickApply();
            }
        });
        sotwLabel = findViewById(R.id.sotw_label);
        sotwLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setAngleField();
            }
        });

        positionButton = findViewById(R.id.position_btn);
        positionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setPosition();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        compass.start();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        currentLoc.setText("--- ---");
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        compass.stop();
        locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        compass.start();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한 요청
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        currentLoc.setText("--- ---");
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (isGPSEnabled) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        locationManager.removeUpdates(locationListener);
        compass.stop();
    }

    private void setupCompass() {
        compass = new Compass(this);
        Compass.CompassListener cl = getCompassListener();
        compass.setListener(cl);
    }

    private void adjustCompass(float azimuth) {
        Animation an = new RotateAnimation(-currentAzimuth, -azimuth,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        currentAzimuth = azimuth;

        an.setDuration(500);
        an.setRepeatCount(0);
        an.setFillAfter(true);

        compassView.startAnimation(an);
    }

    private void adjustSotwLabel(float azimuth) {
        sotwLabel.setText(sotwFormatter.format(azimuth));
    }

    private Compass.CompassListener getCompassListener() {
        return new Compass.CompassListener() {
            @Override
            public void onNewAzimuth(final float azimuth) {
                // UI updates only in UI thread
                // https://stackoverflow.com/q/11140285/444966
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adjustCompass(azimuth);
                        adjustSotwLabel(azimuth);
                    }
                });
            }
        };
    }
    public void setAngleField() {
        float azimuth = currentAzimuth;
        angleField.setText(String.valueOf((int) azimuth));
    }

    public void setPosition() {
        latField.setText("---");
        lngField.setText("---");
        if(currentLocation != null) {
            latField.setText(String.format("%.6f", currentLocation.getLatitude()));
            lngField.setText(String.format("%.6f", currentLocation.getLongitude()));
        } else {
            latField.setText("failed");
            lngField.setText("failed");
        }
    }

    private final LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            if(location != null) {
                currentLocation = location;
                String latitudeLongitude = String.format("%.6f", currentLocation.getLatitude()) + " " + String.format("%.6f",currentLocation.getLongitude());
                currentLoc.setText(latitudeLongitude);
            } else {
                currentLocation = null;
                currentLoc.setText("--- ---");
            }
        }
        public void onProviderDisabled(String provider) {
            latField.setText("Provider disabled");
            lngField.setText("Provider disabled");
        }
        public void onProviderEnabled(String provider) {
        }
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    };

    public void clickApply() {
        String latText = latField.getText().toString();
        String lngText = lngField.getText().toString();
        String distText = distField.getText().toString();
        String angleText = angleField.getText().toString();

        Log.d(TAG, "start Latitude: " + latText);
        Log.d(TAG, "start Longitude: " + lngText);
        Log.d(TAG, "start Altitude: " + angleText);

        double startLatitude = Double.parseDouble(latText);
        double startLongitude = Double.parseDouble(lngText);
        double startAltitude = 0;
        double distanceToDestination = Double.parseDouble(distText);
        double bearingToDestination = Double.parseDouble(angleText);

        double[] destinationCoordinates = calculateDestinationCoordinates(startLatitude, startLongitude, startAltitude, distanceToDestination, bearingToDestination);

        double destinationLatitude = destinationCoordinates[0];
        double destinationLongitude = destinationCoordinates[1];
        double destinationAltitude = destinationCoordinates[2];

        latLabel.setText("Lat.Ref " + String.format("%.6f",destinationLatitude));
        lngLabel.setText("Long.Ref " + String.format("%.6f",destinationLongitude));
        Log.d(TAG, "Destination Latitude: " + destinationLatitude);
        Log.d(TAG, "Destination Longitude: " + destinationLongitude);
        Log.d(TAG, "Destination Altitude: " + destinationAltitude);
    }

    private double[] calculateDestinationCoordinates(double startLat, double startLon, double startAlt, double distanceMeters, double bearingDeg) {
        // Calculate destination coordinates using Haversine formula
        double EARTH_RADIUS = 6371000.0; // 지구 반지름 (미터)

        double destLat = Math.asin(Math.sin(Math.toRadians(startLat)) * Math.cos(distanceMeters / EARTH_RADIUS) +
                Math.cos(Math.toRadians(startLat)) * Math.sin(distanceMeters / EARTH_RADIUS) *
                        Math.cos(Math.toRadians(bearingDeg)));

        double destLon = Math.toRadians(startLon) + Math.atan2(Math.sin(Math.toRadians(bearingDeg)) * Math.sin(distanceMeters / EARTH_RADIUS) * Math.cos(Math.toRadians(startLat)),
                Math.cos(distanceMeters / EARTH_RADIUS) - Math.sin(Math.toRadians(startLat)) * Math.sin(destLat));

        // 위에서 계산된 값을 라디안에서 도로 변환
        destLat = Math.toDegrees(destLat);
        destLon = Math.toDegrees(destLon);

        double destAlt = startAlt; // 시작점과 동일한 고도

        return new double[]{destLat, destLon, destAlt};
    }
}
