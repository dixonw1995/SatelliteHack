package dot.satellitehack;

import android.Manifest;
import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
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
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Time;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import static java.lang.Float.NaN;
import static java.lang.Math.*;

@SuppressWarnings("deprecation")
public class SatelliteHack extends AppCompatActivity {
    //log tag
    private static final String MAIN_TAG = "Satellite Hack";
    private static final String GPS_TAG = "GPS";
    private static final String SENSOR_TAG = "Sensor";
    private static final String UI_TAG = "UI";

    public static final String RESULT = "SatelliteHackResult";
    public static final String TIME = "SatelliteHackTime";

    //main variables
    private int level;
    private List<Satellite> satellites = new ArrayList<>();
    private static final int TIME_LIMIT = 20000;
    private static final float BULLS_EYE = 0.15f;
    private Time startTime = new Time("GMT+8");

    //GPS system variables
    private static final int GPS_PERMISSION = 695;
    private LocationManager locationManager;
    private Criteria criteria = initCriteria();
    private boolean hasSatellites = false;

    //Sensor variables
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private static final int SENSOR_DELAY = 60000;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];

    //UI variables
    private Camera camera;
    private int viewWidth, viewHeight;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;
    private ImageView sight;
    private ImageView bullsEye;
    private Bitmap[] bullsEyeBmp;
    private TextView satelliteCount;
    private Satellite hackTarget;
    private boolean initialized = false;

    //Animators
    private ObjectAnimator rotateAnimator;
    private ObjectAnimator scaleXAnimator;
    private ObjectAnimator scaleYAnimator;
    private DecelerateInterpolator decelerateInterpolator = new DecelerateInterpolator();
    private LinearInterpolator linearInterpolator = new LinearInterpolator();

    //tools
    private DecimalFormat df = new DecimalFormat("0.0");
    private static Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite_hack);
        Log.i(MAIN_TAG, "Starting Satellite Hack");

        //get level and set satellite list
        level = getIntent().getIntExtra("level", 2);

        locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        //initialize sensor listener
        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        //initialize camera
        initCamView();

        //prepare sight views
        sight = (ImageView) findViewById(R.id.sight);
        bullsEye = (ImageView) findViewById(R.id.bulls_eye);
        satelliteCount = (TextView) findViewById(R.id.satellite_count);
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
                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    List<Integer> satelliteRandomIndex = new ArrayList<>();
                    int index;
                    for (int i = 0; i < level && i < maxSatellites; i++) {
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
                            satellites.add(new Satellite(sat));
                            Log.i(GPS_TAG, String.format(
                                    "Satellite%d. Azi %d, Ele %d, Prn% d",
                                    count + 1,
                                    (int) sat.getAzimuth(),
                                    (int) sat.getElevation(),
                                    sat.getPrn()));
                        }
                        count++;
                    }
                    //get satellites once only
                    locationManager.removeGpsStatusListener(gsListener);
                    //locationManager.removeUpdates(locationListener);
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

    //Sensor Listener
    private SensorEventListener seListener = new SensorEventListener() {
        String sensorType, sensorAccuracy;

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            //Log.v(SENSOR_TAG, "Sensor event occurs.");
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            new UpdateTask().execute(); //use an asyncTask to update
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

    //calculate accuracy orientation with satellite position
    private void updateAccuracy(float azimuth, float inclination) {
        for (Satellite sat : satellites) {
            sat.setAccuracy(azimuth, inclination);
        }
    }

    //update UI:display and update sight
    private void initSight(int width, int height) {
        Log.i(UI_TAG, "Initialize sight.");
        int shorterSide = min(width, height);

        //draw sight, initial radius represent 50% accuracy
        float radius = shorterSide * 0.3f;
        sight.setImageBitmap(drawSight(width, height, radius));
        //draw bull's eye
        bullsEyeBmp = new Bitmap[]{
                drawBullsEye(width, height, radius * 2 * BULLS_EYE),
                drawBullsEye(width, height, radius * 2 * BULLS_EYE, true)};
        bullsEye.setImageBitmap(bullsEyeBmp[0]);
        updateSatelliteCount(satellites.size());

        //initialize animator
        initAnimator();
        rotateAnimator.start();
    }

    public void updateSightOrStartGame() {
        if (hasSatellites) {
            if (!initialized) {
                initSight(sight.getWidth(), sight.getHeight());
                //start count-down for time limit
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
                startTime.setToNow();
                initialized = true;
            }
            Log.v(UI_TAG, "Update sight.");
            //0 represent the closest, 2 represent the furthest
            float ratio = (1 - average(Satellite.getAccuracies(satellites))) * 2;
            scaleXAnimator.cancel();
            scaleYAnimator.cancel();
            scaleXAnimator.setFloatValues(ratio);
            scaleYAnimator.setFloatValues(ratio);
            scaleXAnimator.start();
            scaleYAnimator.start();
            bullsEye.setImageBitmap(bullsEyeBmp[0]);
            bullsEye.setClickable(false);
            for (Satellite sat : satellites) {
                if (sat.getAccuracy() > 1 - BULLS_EYE) {
                    hackTarget = sat;
                    bullsEye.setImageBitmap(bullsEyeBmp[1]);
                    bullsEye.setClickable(true);
                    break;
                }
            }
        }
    }

    private void updateSatelliteCount(int count) {
        satelliteCount.setText(String.valueOf(count));
    }

    //return result to main activity
    private void gameOver(boolean success) {
        Time endTime = new Time("GMT+8");
        endTime.setToNow();
        int second = (endTime.minute - startTime.minute) * 60
                + endTime.second - startTime.second;
        Log.i(MAIN_TAG, "Satellite Hack is over. Go back to main game.");
        Intent intent = new Intent();
        intent.putExtra(RESULT, success);
        intent.putExtra(TIME, second);
        setResult(RESULT_OK, intent);
        finish();
    }

    //register listeners when resumed
    @Override
    protected void onResume() {
        super.onResume();
        if (!waitUntilGpsOn.isExecuted()) {
            waitUntilGpsOn.execute();
        }
        sm.registerListener(seListener, aSensor, SENSOR_DELAY);
        sm.registerListener(seListener, mSensor, SENSOR_DELAY);
    }

    private WaitUntilGpsOn waitUntilGpsOn = new WaitUntilGpsOn();

    //wait until user turn on GPS and then get the satellites
    private class WaitUntilGpsOn extends AsyncTask<Void, Void, Void> {
        private boolean isExecuted = false;

        boolean isExecuted() {
            return isExecuted;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isExecuted = true;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            while (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.v(GPS_TAG, "GPS is off.");
                publishProgress();
                try {
                    Thread.sleep(4000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.i(GPS_TAG, "GPS is on.");
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... voids) {
            super.onProgressUpdate();
            Toast.makeText(
                    SatelliteHack.this,
                    "Please turn on GPS...",
                    Toast.LENGTH_LONG).show();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(GPS_TAG, "post");
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
            Log.d(GPS_TAG, "permitted");
            String bestProvider = locationManager.getBestProvider(criteria, true);
            locationManager.addGpsStatusListener(gsListener);
            locationManager.requestLocationUpdates(
                    bestProvider, 2000, 1, locationListener);
            Log.d(GPS_TAG, "listening");
        }
    }

    //release listeners when paused
    @Override
    public void onPause() {
        super.onPause();
        sm.unregisterListener(seListener);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (null != surfaceView) {
            viewWidth = surfaceView.getWidth();
            viewHeight = surfaceView.getHeight();
        }
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

    private float getCameraAzimuth(float azimuth, float pitch, float roll) {
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
                         c = sin^2(R)(1 - sin^2(P))
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
                /*else if (roll < 0 && pitch < 0) { //A
                    newAzimuth = newAzimuth;
                }*/
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
                return (float) toDegrees(cameraAzimuth);
            }
        }
        return NaN;
    }

    private float getInclination(float pitch, float roll) {
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
        return (float) toDegrees(inclination);
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

    private float average(List<Float> numbers) {
        if (numbers.size() == 0)
            return 0;
        return average(numbers, 0, 0);
    }

    private float average(List<Float> numbers, float sum, int length) {
        if (numbers.size() == 0)
            return sum/length;
        sum += numbers.remove(0);
        return average(numbers, sum, length + 1);
    }

    private void initCamView() {
        surfaceView = (SurfaceView) findViewById(R.id.camera);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.setKeepScreenOn(true);
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                initCamera();
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

    private void initCamera() {
        camera = Camera.open();
        camera.setDisplayOrientation(90);
        if (null != camera) {
            try {
                Camera.Parameters parameters = camera.getParameters();
                parameters.setPreviewSize(viewWidth, viewHeight);
                parameters.setPreviewFpsRange(4,10);
                parameters.setPictureFormat(ImageFormat.JPEG);
                parameters.set("jpeg-quality", 90);
                parameters.setPictureSize(viewWidth, viewHeight);
                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private Bitmap drawSight(int width, int height, float radius) {
        float[] cross = {width/2, height/2 - radius*1.5f, width/2, height/2-radius*0.5f,
                width/2 + radius*1.5f, height/2, width/2 + radius*0.5f, height/2,
                width/2, height/2 + radius*1.5f, width/2, height/2 + radius*0.5f,
                width/2 - radius*1.5f, height/2, width/2 - radius*0.5f, height/2};

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.YELLOW);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bmp);
        canvas.drawCircle(width/2, height/2, radius, paint);
        canvas.drawLines(cross, paint);

        return bmp;
    }

    private Bitmap drawBullsEye(int width, int height, float radius) {
        return drawBullsEye(width, height, radius, false);
    }

    private Bitmap drawBullsEye(int width, int height, float radius, boolean fill) {
        Paint paint = new Paint();
        if (fill)
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
        else paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setColor(Color.RED);

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444);
        Canvas canvas = new Canvas(bmp);
        canvas.drawCircle(width/2, height/2, radius, paint);

        return bmp;
    }

    private void initAnimator() {
        //initial animators
        rotateAnimator = ObjectAnimator.ofFloat(sight, "rotation", -1080f);
        rotateAnimator.setDuration(TIME_LIMIT);
        rotateAnimator.setInterpolator(decelerateInterpolator);
        //also do timer task to play safe
        rotateAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {}

            //lose mini-game and go back to main game when rotation is over
            @Override
            public void onAnimationEnd(Animator animator) {
                Log.i(MAIN_TAG, "Satellite Hack fail.(animator)");
                gameOver(false);
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
        scaleXAnimator = ObjectAnimator.ofFloat(sight, "scaleX", 1);
        scaleXAnimator.setDuration(300);
        scaleXAnimator.setInterpolator(linearInterpolator);
        scaleYAnimator = ObjectAnimator.ofFloat(sight, "scaleY", 1);
        scaleYAnimator.setDuration(300);
        scaleYAnimator.setInterpolator(linearInterpolator);
    }

    //test satellite fetching
    public void showSatellite() {
        if (hasSatellites) {
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
    }

    //test orientation calculation
    private void showOrientation(float azimuth, float inclination) {
        ((TextView) findViewById(R.id.converted_azimuth)).setText(
                df.format(azimuth));
        ((TextView) findViewById(R.id.converted_inclination)).setText(
                df.format(inclination));
    }

    private class UpdateTask extends AsyncTask<Void, Void, Void> {
        private float[] r = new float[9];
        private float[] orientations = new float[3];
        private float azimuth, inclination;

        @Override
        protected Void doInBackground(Void... voids) {
            SensorManager.getRotationMatrix(r, null, accelerometerValues, magneticFieldValues);
            SensorManager.getOrientation(r, orientations);
            azimuth = getCameraAzimuth(orientations[0], orientations[1], orientations[2]);
            inclination = getInclination(orientations[1], orientations[2]);
            updateAccuracy(azimuth, inclination);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            showOrientation(azimuth, inclination);
            ((TextView) findViewById(R.id.ga)).setText(
                    String.valueOf(average(Satellite.getAccuracies(satellites))));
            updateSightOrStartGame();
        }
    }

    public void hack(View view) {
        if (null != hackTarget) {
            if (satellites.remove(hackTarget)) {
                int satelliteCount = satellites.size();
                Log.i(MAIN_TAG, String.format(
                        "Satellite hack success. %d satellite(s)' left.", satelliteCount));
                updateSatelliteCount(satelliteCount);
                if (satelliteCount == 0) {
                    Log.i(MAIN_TAG, "All satellites are hacked. Stage clear.");
                    gameOver(true);
                }
                hackTarget = null;
            }
        }
    }
}