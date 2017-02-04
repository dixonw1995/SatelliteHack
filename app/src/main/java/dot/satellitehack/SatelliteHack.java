package dot.satellitehack;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SatelliteHack extends AppCompatActivity {
    //log tag
    private static final String MAIN_TAG = "Satellite Hack";
    private static final String GPS_TAG = "GPS";
    private static final String SENSOR_TAG = "Sensor";
    private static final String DEBUG_TAG = "Debug";

    //main variables
    private int satelliteCount;
    private float accuracy;

    //GPS system variables
    private LocationManager locationManager;
    private List<GpsSatellite> satellites;
    private boolean hasSatellites = false;

    //Sensor variables
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private static final int SENSOR_DELAY = 600000;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float[] orientations = new float[3];
    private float inclination;
    private float[] r = new float[9];

    //SurfaceView/Camera variables
    private Camera camera;
    private int viewWidth, viewHeight;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView imageView;

    //tools
    DecimalFormat df = new DecimalFormat("0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(MAIN_TAG, "Starting Satellite Hack");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite_hack);
        //get level and set satellite list
        satelliteCount = getIntent().getIntExtra("level", 1);
        satellites = new ArrayList<>();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "Please turn on GPS...", Toast.LENGTH_LONG).show();
        }
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        String bestProvider = locationManager.getBestProvider(criteria, true);
        locationManager.requestLocationUpdates(bestProvider, 3000, 1, locationListener);
        locationManager.addGpsStatusListener(gsListener);

        //start sensor listener
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //update UI
    }

    //release sensor when paused
    @Override
    public void onPause(){
        sm.unregisterListener(seListener);
        super.onPause();
    }

    //register sensor when resumed
    @Override
    protected void onResume() {
        //sm.registerListener(seListener, aSensor, SENSOR_DELAY);
        //sm.registerListener(seListener, mSensor, SENSOR_DELAY);
        super.onResume();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {}

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.i(GPSTAG, "GPS system is available");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(GPSTAG, "GPS system is out of service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(GPSTAG, "GPS system is temporarily unavailable");
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {}

        @Override
        public void onProviderDisabled(String provider) {}
    };

    //GPS status listener
    GpsStatus.Listener gsListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(GPS_TAG, "GPS system has received its first fix");
                    break;
                //GPS satellite status changed
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.i(GPS_TAG, "GPS system report status");
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    List<Integer> satelliteRandomIndex = new ArrayList<>();
                    Random random = new Random();
                    int index;
                    for (int i = 0; i < satelliteCount && i < maxSatellites; i++) {
                        do {
                            index = random.nextInt(maxSatellites);
                        } while (satelliteRandomIndex.contains(index));
                        satelliteRandomIndex.add(index);
                    }
                    Log.i(GPS_TAG, "Set satellite list");
                    Iterator<GpsSatellite> satIter = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (satIter.hasNext() && count <= maxSatellites) {
                        if (satelliteRandomIndex.contains(count)) {
                            GpsSatellite sat = satIter.next();
                            satellites.add(sat);
                            Log.i(GPS_TAG, String.format("Satellite%d: Azi%d Ele%d Prn%d",
                                    count + 1,
                                    (int) sat.getAzimuth(),
                                    (int) sat.getElevation(),
                                    sat.getPrn()));
                        }
                        count++;
                    }
                    //get satellites once only
                    locationManager.removeGpsStatusListener(gsListener);
                    locationManager.removeUpdates(locationListener);
                    Log.i(GPS_TAG, "GPS system has stopped");
                    hasSatellites = true;
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i(GPS_TAG, "GPS system has started");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i(GPS_TAG, "GPS system has stopped");
                    break;
            }
        }
    };

    //test satellite fetching
    public void showSatellite(View v) {
        if (hasSatellites) {
            TableLayout developerView = (TableLayout) findViewById(R.id.developerView);
            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
            for (int i = 0; i < satellites.size(); i++) {
                GpsSatellite sat = satellites.get(i);
                TableRow satelliteRow = new TableRow(this);
                satelliteRow.setLayoutParams(layoutParams);

                TextView indexCell = new TextView(this);
                indexCell.setLayoutParams(layoutParams);
                indexCell.setText(String.valueOf(i));

                TextView aziCell = new TextView(this);
                aziCell.setLayoutParams(layoutParams);
                aziCell.setText(getString(R.string.sat_cell, "Azi", (int) sat.getAzimuth()));

                TextView eleCell = new TextView(this);
                eleCell.setLayoutParams(layoutParams);
                eleCell.setText(getString(R.string.sat_cell, "Ele", (int) sat.getElevation()));

                TextView prnCell = new TextView(this);
                prnCell.setLayoutParams(layoutParams);
                prnCell.setText(getString(R.string.sat_cell, "Prn", sat.getPrn()));

                satelliteRow.addView(indexCell);
                satelliteRow.addView(aziCell);
                satelliteRow.addView(eleCell);
                satelliteRow.addView(prnCell);

                developerView.addView(satelliteRow);
            }
        }
    }

    //Sensor Listener
    private SensorEventListener seListener = new SensorEventListener() {
        TextView sensorAccuracyView;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            Log.i(SENSOR_TAG, "Sensor event occurs.");
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            updateAccuracy();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            Log.i(SENSOR_TAG, "Accuracy of sensor has changed");
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                sensorAccuracyView = (TextView) findViewById(R.id.ma);
            }
            else{ //if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                sensorAccuracyView = (TextView) findViewById(R.id.aa);
            }
            switch (i) {
                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                    sensorAccuracyView.setText(R.string.high);
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    sensorAccuracyView.setText(R.string.medium);
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    sensorAccuracyView.setText(R.string.low);
                    break;
                case SensorManager.SENSOR_STATUS_UNRELIABLE:
                    sensorAccuracyView.setText(R.string.unreliable);
                    break;
                default:
                    sensorAccuracyView.setText(R.string.undefined);
            }
        }
    };

    //calculate accuracy orientation with satellite position
    private void updateAccuracy() {
        SensorManager.getRotationMatrix(r, null,
                accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(r, orientations);
        inclination = SensorManager.getInclination(r);
        Log.i(SENSOR_TAG,
                "Azimuth " + df.format(Math.toDegrees(orientations[0])));
        //Log.i(SENSOR_TAG, "Pitch " + df.format(Math.toDegrees(orientations[1])));
        Log.i(SENSOR_TAG,
                "Roll " + df.format(Math.toDegrees(orientations[2])));
        Log.i(SENSOR_TAG,
                "Inclination " + df.format(Math.toDegrees(inclination)));
    }

    //update UI:open camera, display and update sight
    private void initCamera() {}

    private void drawSight() {}

    private void updateSight() {}
}

