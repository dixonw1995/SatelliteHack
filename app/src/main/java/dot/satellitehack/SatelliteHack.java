package dot.satellitehack;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
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
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static dot.satellitehack.SatelliteHackGame.*;
import static dot.satellitehack.State.*;
import static java.lang.Math.*;
import static dot.satellitehack.MathTools.*;
import static dot.satellitehack.UITools.*;

@SuppressWarnings("deprecation")
public class SatelliteHack extends AppCompatActivity {
    //log tag
    private static final String MAIN_TAG = "Satellite Hack";
    private static final String GPS_TAG = "GPS";
    private static final String SENSOR_TAG = "Sensor";
    private static final String UI_TAG = "UI";

    public static final String RESULT = "SatelliteHackResult";
    public static final String TIME = "SatelliteHackTime";

    //game elements
    private SatelliteHackGame game = new SatelliteHackGame();
    private class UpdateGame extends AsyncTask<Void, Void, Void> {
        private float[] r = new float[9];
        private float[] orientations = new float[3];
        private float azimuth, inclination;

        @Override
        protected Void doInBackground(Void... voids) {
            SensorManager.getRotationMatrix(r, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(r, orientations);
            azimuth = getCameraAzimuth(orientations[0], orientations[1], orientations[2]);
            inclination = getInclination(orientations[1], orientations[2]);
            game.setAccuracies(azimuth, inclination);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (!game.getState().equals(STARTED)) return;
            updateSight();
            updateSound(game.getAvgAccuracy());
            satelliteCountView.setText(String.valueOf(game.countSatellite()));
            ((TextView) findViewById(R.id.ga)).setText(
                    String.valueOf(game.getAvgAccuracy()));
            showOrientation(azimuth, inclination);
        }
    }

    //GPS system variables
    private static final int GPS_PERMISSION = 695;
    private LocationManager locationManager;
    private Criteria criteria = initCriteria();
    private boolean hasSatellites = false;
    private SatelliteFinder satelliteFinder = new SatelliteFinder();
    //wait until user turn on GPS and then get the satellites
    private class SatelliteFinder extends AsyncTask<Void, Void, Void> {
        private boolean showToast = true;

        void setShowToast(boolean showToast) {
            this.showToast = showToast;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.v(GPS_TAG, "GPS is off.");
                if (showToast)
                    publishProgress();
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i(GPS_TAG, "GPS is on.");
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            Toast.makeText(
                    SatelliteHack.this,
                    "Please turn on GPS...",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //check permission
            if (ActivityCompat.checkSelfPermission(SatelliteHack.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) !=
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(SatelliteHack.this,
                            Manifest.permission.ACCESS_COARSE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(SatelliteHack.this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        GPS_PERMISSION);
            }
            String bestProvider = locationManager.getBestProvider(criteria, true);
            locationManager.addGpsStatusListener(gsListener);
            locationManager.requestLocationUpdates(
                    bestProvider, 2000, 1, locationListener);
        }
    }

    //Sensor variables
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private static final int SENSOR_DELAY = 60000;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];

    //UI variables
    private Camera camera;
    private ImageView sight;
    private ImageView bullsEye;
    private Bitmap[] bullsEyeBmp;
    private TextView satelliteCountView;
    private Satellite hackTarget;
    private MediaPlayer noise;
    private MediaPlayer signal;
    private MediaPlayer hit;
    private Vibrator vibrator;

    //Animators
    private ObjectAnimator rotateAnimator;
    private ObjectAnimator scaleXAnimator;
    private ObjectAnimator scaleYAnimator;
    private Interpolator countDownInterpolator = new DecelerateInterpolator();
    private LinearInterpolator linearInterpolator = new LinearInterpolator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite_hack);
        initGame();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        startGame();
    }

    //register listeners when resumed
    @Override
    protected void onResume() {
        super.onResume();
        satelliteFinder.setShowToast(true);
        sm.registerListener(seListener, aSensor, SENSOR_DELAY);
        sm.registerListener(seListener, mSensor, SENSOR_DELAY);
        if (game.getState().equals(STARTED)) {
            noise.start();
            signal.start();
        }
    }

    //release listeners when paused
    @Override
    public void onPause() {
        super.onPause();
        satelliteFinder.setShowToast(false);
        sm.unregisterListener(seListener);
        noise.pause();
        signal.pause();
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

    @Override
    public void onBackPressed() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Give up Generator")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        gameOver(false);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    //GPS status listener
    private GpsStatus.Listener gsListener = new GpsStatus.Listener() {
        boolean dummy; //Android Studio dun let me collapse it

        @Override
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

                    Log.i(GPS_TAG, "Set satellite list");
                    //get satellites from locationManager
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    Iterator<GpsSatellite> satIterator =
                            gpsStatus.getSatellites().iterator();
                    //parse satellite iterator into list
                    List<GpsSatellite> satList = new ArrayList<>();
                    int count = 0;
                    while (satIterator.hasNext() && count++ <= maxSatellites) {
                        satList.add(satIterator.next());
                    }
                    game.addSatellites(satList);

                    //get satellites once only. remove all GPS listeners
                    locationManager.removeGpsStatusListener(gsListener);
                    locationManager.removeUpdates(locationListener);
                    Log.i(GPS_TAG, "GPS system has stopped");
                    hasSatellites = true;
                    showSatellite(game.getSatellites());
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

    private Criteria initCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(false);
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(false);
        criteria.setAltitudeRequired(false);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }

    //Sensor Listener
    private SensorEventListener seListener = new SensorEventListener() {
        String sensorType, sensorAccuracy;
        final int SKIP = 0; //no skip
        int count = 0;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            //update too frequently, skip some event
            if (count-- <= 0) {
                count = SKIP;
            }
            else return;

            Log.v(SENSOR_TAG, "Sensor event occurs.");
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            new UpdateGame().execute(); //use an asyncTask to update
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {
            Log.v(SENSOR_TAG, "Accuracy of sensor has changed");
            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                sensorType = "TYPE_MAGNETIC_FIELD";
            } else { //if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                sensorType = "TYPE_ACCELEROMETER";
            }
            switch (i) {
                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                    sensorAccuracy = "high";
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                    sensorAccuracy = "medium";
                    break;
                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                    sensorAccuracy = "low";
                    break;
                case SensorManager.SENSOR_STATUS_UNRELIABLE:
                    sensorAccuracy = "unreliable";
                    break;
                default:
                    sensorAccuracy = "undefined";
            }
            Log.v(SENSOR_TAG, String.format("%s accuracy is %s now", sensorType, sensorAccuracy));
        }
    };

    //update UI:display and update sight
    private void initSight() {
        Log.i(UI_TAG, "Initialize sight.");
        int width = sight.getWidth();
        int height = sight.getHeight();
        int shorterSide = min(width, height);

        //draw sight, initial radius represent 50% accuracy
        float radius = shorterSide * 0.3f;
        sight.setImageBitmap(drawSight(width, height, radius));
        //draw bull's eye
        bullsEyeBmp = new Bitmap[]{
                drawBullsEye(width, height, radius * 2 * BULLS_EYE),
                drawBullsEye(width, height, radius * 2 * BULLS_EYE, true)};
        bullsEye.setImageBitmap(bullsEyeBmp[0]);
    }

    public void updateSight() {
        if (!hasSatellites) return;
        Log.v(UI_TAG, "Update sight.");
        //0 represent the closest, 2 represent the furthest
        float ratio = (1 - game.getAvgAccuracy()) * 2;
        scaleXAnimator.cancel();
        scaleYAnimator.cancel();
        scaleXAnimator.setFloatValues(ratio);
        scaleYAnimator.setFloatValues(ratio);
        scaleXAnimator.start();
        scaleYAnimator.start();
        boolean accurate = false;
        for (Satellite sat : game.getSatellites()) {
            if (sat.getAccuracy() > 1 - BULLS_EYE) {
                accurate = true;
                hackTarget = sat;
                bullsEye.setImageBitmap(bullsEyeBmp[1]);
                bullsEye.setClickable(true);
                break;
            }
        }
        if (!accurate) {
            bullsEye.setImageBitmap(bullsEyeBmp[0]);
            bullsEye.setClickable(false);
        }
    }

    private void initAnimator() {
        //initial animators
        rotateAnimator = ObjectAnimator.ofFloat(sight, "rotation", -1080f);
        rotateAnimator.setDuration(TIME_LIMIT);
        rotateAnimator.setInterpolator(countDownInterpolator);
        scaleXAnimator = ObjectAnimator.ofFloat(sight, "scaleX", 1);
        scaleXAnimator.setDuration(200);
        scaleXAnimator.setInterpolator(linearInterpolator);
        scaleYAnimator = ObjectAnimator.ofFloat(sight, "scaleY", 1);
        scaleYAnimator.setDuration(200);
        scaleYAnimator.setInterpolator(linearInterpolator);
    }

    //private void initCamView() {
    private void initCamera() {
        Log.i(UI_TAG, "Set surface view call back");
        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera);
        SurfaceHolder surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.i(UI_TAG, "Turn on camera");
                camera = Camera.open();
                camera.setDisplayOrientation(90);
                if (null != camera) {
                    try {
                        camera.setPreviewDisplay(surfaceHolder);
                        camera.startPreview();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                if (null != camera) {
                    camera.stopPreview();
                    camera.release();
                }
            }
        });
    }

    private void initSound() {
        Log.v(UI_TAG, "Initialize sound effect");
        noise = MediaPlayer.create(this, R.raw.noise);
        noise.setLooping(true);
        noise.setVolume(0.5f, 0.5f);
        signal = MediaPlayer.create(this, R.raw.iwtus);
        signal.setLooping(true);
        signal.setVolume(0.5f, 0.5f);
        hit = MediaPlayer.create(this, R.raw.transmit);
        hit.setVolume(1, 1);
    }

    private void updateSound(float accuracy) {
        Log.v(UI_TAG, "Update sound effect");
        int maxVolume = 100;
        float noiseVol = (float) (log(maxVolume - (1 - accuracy) * 100)
                / log(maxVolume));
        float signalVol = (float) (log(maxVolume - accuracy * 100)
                / log(maxVolume));
        noise.setVolume(1 - noiseVol, 1 - noiseVol);
        signal.setVolume(1 - signalVol, 1 - signalVol);
    }

    private void initGame() {
        if (null != game.getState()) return;
        game.setState(READY);
        Log.i(MAIN_TAG, "Loading Satellite Hack");

        //get level and set satellite list
        game.setLevel(
                getIntent().getIntExtra("level", 2)
        );
        locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        satelliteFinder.execute();

        //initialize sensor listener
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //initialize camera
        initCamera();

        //prepare sight views
        sight = (ImageView) findViewById(R.id.sight);
        bullsEye = (ImageView) findViewById(R.id.bulls_eye);
        satelliteCountView = (TextView) findViewById(R.id.satellite_count);

        //initialize effects
        initAnimator();
        initSound();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private void startGame() {
        if (game.getState().compareTo(STARTED) >= 0) return;
        game.setState(STARTED);
        Log.i(MAIN_TAG, "Start Satellite Hack");
        //display sight
        initSight();
        rotateAnimator.start();
        noise.start();
        signal.start();
        //start counting time
        new CountDownTimer(TIME_LIMIT, 5000) {
            @Override
            public void onTick(long l) {
                Log.v(MAIN_TAG, String.format("%ds left", l / 1000));
            }

            @Override
            public void onFinish() {
                Log.i(MAIN_TAG, "Satellite Hack fail.(timer)");
                gameOver(false);
            }
        }.start();
        game.startTimer();
    }

    public void hack(View view) {
        if (!game.getState().equals(STARTED) || null == hackTarget) return;
        if (game.removeSatellite(hackTarget)) {
            vibrator.vibrate(200);
            hit.start();
            int satelliteCount = game.countSatellite();
            Log.i(MAIN_TAG, String.format(
                    "Satellite hack success. %d satellite(s)' left.", satelliteCount));
            satelliteCountView.setText(String.valueOf(game.countSatellite()));
            if (satelliteCount == 0) {
                Log.i(MAIN_TAG, "All satellites are hacked. Stage clear.");
                gameOver(true);
            }
            hackTarget = null;
        }
    }

    //return result to main activity
    private void gameOver(boolean success) {
        if (game.getState().compareTo(OVER) >= 0) return;
        game.setState(OVER);

        Log.i(MAIN_TAG, "Satellite Hack is over. Go back to main game.");
        game.stopTimer();
        int second = game.getTimeUsed();
        Intent intent = new Intent();
        intent.putExtra(RESULT, success);
        intent.putExtra(TIME, second);
        setResult(RESULT_OK, intent);
        finish();
    }

    //debug display
    //test satellite fetching
    public void showSatellite(List<Satellite> satellites) {
        if (!hasSatellites) return;
        TableLayout developerView =
                (TableLayout) findViewById(R.id.developerView);
        TableRow.LayoutParams layoutParams = new TableRow.LayoutParams(
                TableRow.LayoutParams.MATCH_PARENT,
                TableRow.LayoutParams.WRAP_CONTENT);
        for (int i = 0; i < satellites.size(); i++) {
            Satellite sat = satellites.get(i);
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

    //test orientation calculation
    private void showOrientation(float azimuth, float inclination) {
        ((TextView) findViewById(R.id.converted_azimuth)).setText(
                decimalFormat.format(azimuth));
        ((TextView) findViewById(R.id.converted_inclination)).setText(
                decimalFormat.format(inclination));
    }

}