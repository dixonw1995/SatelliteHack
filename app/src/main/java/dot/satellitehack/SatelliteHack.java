package dot.satellitehack;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static java.lang.Double.NaN;
import static java.lang.Math.*;

@SuppressWarnings("deprecation")
public class SatelliteHack extends AppCompatActivity {
    //log tag
    private static final String MAIN_TAG = "Satellite Hack";
    private static final String GPS_TAG = "GPS";
    private static final String SENSOR_TAG = "Sensor";

    public static final String RESULT = "SatelliteHackResult";

    //main variables
    private int satelliteCount;
    private List<GpsSatellite> satellites;
    private double accuracy;

    //GPS system variables
    private static final int GPS_PERMISSION = 695;
    private LocationManager locationManager;
    private Criteria criteria = new Criteria();
    private boolean hasSatellites = false;

    //Sensor variables
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private static final int SENSOR_DELAY = 60000;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private float[] r = new float[9];
    private float[] orientations = new float[3];
    private double azimuth;
    private double inclination;

    /*/SurfaceView/Camera variables
    private Camera camera;
    private int viewWidth, viewHeight;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView imageView;*/

    //tools
    DecimalFormat df = new DecimalFormat("0");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite_hack);
        Log.i(MAIN_TAG, "Starting Satellite Hack");
        //get level and set satellite list
        satelliteCount = getIntent().getIntExtra("level", 1);
        satellites = new ArrayList<>();
        locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this,
                    "Please turn on GPS...", Toast.LENGTH_LONG).show();
        }
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);

        //start sensor listener
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //update UI
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == GPS_PERMISSION) {
            if (!(grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    //release sensor when paused
    @Override
    public void onPause() {
        //check permission
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    GPS_PERMISSION);
        }
        locationManager.removeGpsStatusListener(gsListener);
        locationManager.removeUpdates(locationListener);

        sm.unregisterListener(seListener);

        super.onPause();
    }

    //register sensor when resumed
    @Override
    protected void onResume() {
        if (!hasSatellites) {
            //check permission
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        GPS_PERMISSION);
            }
            String bestProvider = locationManager.getBestProvider(criteria, true);
            locationManager.requestLocationUpdates(
                    bestProvider, 3000, 1, locationListener);
            locationManager.addGpsStatusListener(gsListener);
        }

        sm.registerListener(seListener, aSensor, SENSOR_DELAY);
        sm.registerListener(seListener, mSensor, SENSOR_DELAY);

        super.onResume();
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.i(GPS_TAG, "GPS system is available");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.i(GPS_TAG, "GPS system is out of service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.i(GPS_TAG, "GPS system is temporarily unavailable");
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
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
                    if (ActivityCompat.checkSelfPermission(SatelliteHack.this,
                            Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(SatelliteHack.this,
                                    Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(SatelliteHack.this,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                GPS_PERMISSION);
                    }
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
                    Iterator<GpsSatellite> satIterator =
                            gpsStatus.getSatellites().iterator();
                    int count = 0;
                    while (satIterator.hasNext() && count <= maxSatellites) {
                        if (satelliteRandomIndex.contains(count)) {
                            GpsSatellite sat = satIterator.next();
                            satellites.add(sat);
                            Log.i(GPS_TAG, String.format(
                                    "Satellite%d: Azi%d Ele%d Prn%d",
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
                    showSatellite();
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
    public void showSatellite() {
        if (hasSatellites) {
            TableLayout developerView =
                    (TableLayout) findViewById(R.id.developerView);
            TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                    TableRow.LayoutParams.MATCH_PARENT,
                    TableRow.LayoutParams.WRAP_CONTENT);
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
                eleCell.setText(
                        getString(R.string.sat_cell, "Ele", (int) sat.getElevation()));

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
            // TODO: 6/2/2017 use asyncTask instead
            SensorManager.getRotationMatrix(r, null,
                    accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(r, orientations);
            inclination = getInclination(orientations[1], orientations[2]);
            azimuth = getCameraAzimuth(orientations[0], orientations[1], orientations[2]);
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
        double azi, inc, aziAccuracy, incAccuracy;
        List<Double> aziAccuracies = new ArrayList<>();
        List<Double> incAccuracies = new ArrayList<>();
        showOrientation();
        for (GpsSatellite sat: satellites) {
            /*
            The best absolute difference of 2 azimuths is 180.
            The worst is 0 or 360 which means the user points at opposite direction.
            The equation gives the rate of how close the user's pointed at the satellite.
            accuracy rate = (180 - | | azimuth - satellite_azimuth | - 180 |) / 180
             */
            aziAccuracy =
                    (180 - abs(abs(azimuth - sat.getAzimuth()) - 180)) / 180;
            aziAccuracies.add(aziAccuracy);
            ((TextView) findViewById(R.id.ga1)).setText(
                    String.valueOf(aziAccuracy));
            /*
            The best sum of inclination and elevation is 90.
            The worst is when the user points at the floor(180 + X = 180 ~ 270)
            The equation gives the rate of how close the user's pointed at the satellite
            accuracy rate = (180 - | inclination + elevation - 90 |) / 180
            */
            incAccuracy = (180 - abs((inclination + sat.getElevation()) - 90)) / 180;
            incAccuracies.add(incAccuracy);
            ((TextView) findViewById(R.id.ga2)).setText(
                    String.valueOf(incAccuracy));
        }
        accuracy = (average(aziAccuracies) + average(incAccuracies)) / 2;
        ((TextView) findViewById(R.id.ga)).setText(
                String.valueOf(accuracy));
    }

    private void showOrientation() {
        ((TextView) findViewById(R.id.azimuth)).setText(
                df.format(toDegrees(orientations[0])));
        if (azimuth == NaN) {
            Log.w("Azi/a", df.format(toDegrees(orientations[0])));
            Log.w("Azi/p", df.format(toDegrees(orientations[1])));
            Log.w("Azi/r", df.format(toDegrees(orientations[2])));
        }
        ((TextView) findViewById(R.id.converted_azimuth)).setText(
                df.format(azimuth));
        ((TextView) findViewById(R.id.pitch)).setText(
                df.format(toDegrees(orientations[1])));
        ((TextView) findViewById(R.id.roll)).setText(
                df.format(toDegrees(orientations[2])));
        ((TextView) findViewById(R.id.inclination)).setText(
                df.format(toDegrees(SensorManager.getInclination(r))));
        ((TextView) findViewById(R.id.converted_inclination)).setText(
                df.format(inclination));
    }

    private double getCameraAzimuth(float azimuth, float pitch, float roll) {
        /*
        This function will first calculate magnitude of camera azimuth with device's y-axis as
        reference, which means it ignores magnetic direction.
        It then combines the magnitude with device's azimuth to return camera azimuth in degree.
        Camera azimuth range from 0 to 360
        let r = 1, P = pitch, R = roll
        equation1:  (x/r)^2 + (y/r/sin(P))^2 = 1
                =>        x^2 + (y/sin(P))^2 = 1
                =>        (x sin(P))^2 + y^2 = sin^2(P)
                =>                       y^2 = sin^2(P) - (x sin(P))^2
                =>                       y^2 = sin^2(P)(1 - x^2)
        equation2:   (x/r/sin(R))^2 + (y/r)^2 = 1
                =>        (x/sin(R))^2 + y^2 = 1
                =>        x^2 + (y sin(R))^2 = sin^2(R)
                =>                       x^2 = sin^2(R) - (y sin(R))^2
                =>                       x^2 = sin^2(R)(1 - y^2)
        sub 1 to 2:                      x^2 = sin^2(R)(1 - sin^2(P)(1 - x^2))
                =>                         0 = ((sin(R)sin(P))^2-1)x^2 + sin^2(R)(1-sin^2(P))
        quadratic form:  a = (sin(R)sin(P))^2-1
                         b = 0
        */
        double a = pow(sin(roll) * sin(pitch), 2) - 1;
        double c = pow(sin(roll), 2) * (1 - pow(sin(pitch), 2));
        double[] xs = quadraticFormula(a, 0, c);
        if (xs != null) {
            double x = xs[0];
            //sub x to equation1: y^2 = sin^2(P)(1 - x^2)
            double y = sqrt(pow(sin(pitch), 2) * (1 - x * x));
            if (y != NaN) {
                double cameraAzimuth;
                /*
                this azimuth is screen
                tan(azimuth) = x / y
                azimuth = 90 when y = 0
                 */
                if (!isZero(y))
                    cameraAzimuth = abs(atan(x / y));
                else cameraAzimuth = PI/2;
                //convert azimuth according to ASTC quadrants
                //consider the camera direction instead of screen
                if (roll >= 0 && pitch >= 0) { //T
                    cameraAzimuth = PI + cameraAzimuth;
                }
                else if (roll >= 0 && pitch < 0) { //S
                    cameraAzimuth = PI * 2 - cameraAzimuth;
                }
                else if (roll < 0 && pitch < 0) { //A
                    //newAzimuth = newAzimuth;
                }
                else if (roll < 0 && pitch >= 0) { //C
                    cameraAzimuth = PI - cameraAzimuth;
                }
                //adjust when face-down
                if (abs(roll) >= PI/2)
                    cameraAzimuth = -cameraAzimuth - PI;
                //increase by magnetic azimuth
                cameraAzimuth += azimuth;
                //give positive angle between 0 and 360
                cameraAzimuth = positiveMod(cameraAzimuth, (PI * 2));
                return toDegrees(cameraAzimuth);
            }
        }
        return NaN;
    }

    private double[] quadraticFormula(double a, double b, double c) {
        double d = discriminant(a, b, c);
        if (d < 0) return null;
        if (isZero(d)) {
            double root1 = -b / (2 * a);
            return new double[] {root1};
        }
        //if (d > 0) {
        double root1 = (-b + sqrt(d)) / (2 * a);
        double root2 = (-b - sqrt(d)) / (2 * a);
        return new double[] {root1, root2};
    }

    private double discriminant(double a, double b, double c) {
        return b * b - 4 * a * c;
    }

    private boolean isZero(double value) {
        final double threshold = 0.000001;
        return value >= -threshold && value <= threshold;
    }

    private double positiveMod(double dividend, double divisor) {
        double remainder = dividend % divisor;
        if (remainder < 0) remainder += divisor;
        return remainder;
    }

    private double getInclination(float pitch, float roll) {
        /*
        This function return device's inclination in degree
        range of inclination depends on roll
        roll: -90~90(screen face-up) -> inclination: 90~180(further)**
        roll: -180~-90,90~180(screen face-down) -> inclination: 0~90(closer)**
        cos(inclination) = |cos(pitch)| + |cos(roll)| - 1
        **find the operational satellites, not the fallen
        */
        double inclination = acos(cos(pitch) + abs(cos(roll)) - 1);
        if (abs(roll) < PI/2)
            inclination = PI - inclination;
        return toDegrees(inclination);
    }

    private double average(List<Double> numbers) {
        if (numbers.size() == 0)
            return 0;
        return average(numbers, 0, 0);
    }

    private double average(List<Double> numbers, double sum, int length) {
        if (numbers.size() == 0)
            return sum/length;
        sum += numbers.remove(0);
        return average(numbers, sum, length + 1);
    }

    //update UI:open camera, display and update sight
    private void initCamera() {}

    private void drawSight() {}

    private void updateSight() {}

    //return result to main activity
    private void gameOver(boolean success) {
        Intent intent = new Intent();
        intent.putExtra(RESULT, success);
        setResult(RESULT_OK, intent);
        finish();
    }
}

