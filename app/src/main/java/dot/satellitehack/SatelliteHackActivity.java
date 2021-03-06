package dot.satellitehack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
//import android.hardware.Camera;
import android.os.AsyncTask;
//import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
//import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.github.lzyzsd.circleprogress.DonutProgress;

//import java.io.IOException;
import java.util.List;

import static dot.satellitehack.MathTools.decimalFormat;
import static dot.satellitehack.SatelliteHackGame.BULLS_EYE;
import static dot.satellitehack.SatelliteHackGame.TIME_LIMIT;
import static dot.satellitehack.State.FAILURE;
import static dot.satellitehack.State.OVER;
import static dot.satellitehack.State.READY;
import static dot.satellitehack.State.ACTIVE;
import static dot.satellitehack.State.SUCCESS;
import static dot.satellitehack.State.StateException;

@SuppressWarnings("deprecation")
public class SatelliteHackActivity extends AppCompatActivity {
    //log tag
    private static final String GAME_TAG = "Satellite Hack";
//    private static final String GPS_TAG = "GPS";
//    private static final String SENSOR_TAG = "Sensor";
    private static final String UI_TAG = "UI";

    public static final String RESULT = "SatelliteHackResult";
    public static final String TIME = "SatelliteHackTime";

    //game managers
    private Handler handler = new Handler();
    private SatelliteHackGame game = new SatelliteHackGame();
    private StartGame startGame = new StartGame();
//    private UpdateGame updateGame = new UpdateGame(handler);
    private Intent intent;

    //GPS system variables
//    private static final int GPS_PERMISSION = 695;
//    private LocationManager locationManager;
//    private Criteria criteria = initCriteria();
//    private boolean hasSatellite = false;
//    private SatelliteFinder satelliteFinder = new SatelliteFinder();
    SatelliteFinder finder;

    //Sensor variables
//    private SensorManager sm;
//    private Sensor aSensor;
//    private Sensor mSensor;
//    private static final int SENSOR_DELAY = 60000;
//    private float[] accelerometerValues = new float[3];
//    private float[] magneticFieldValues = new float[3];
    private SensorListenerAndUpdateThread slaut;

    //UI variables
//    private RelativeLayout thisLayout;
//    private CameraView cameraView;
//    private Camera camera;
//    private PingView ping;
    private Stopwatch stopwatch;
    private ImageView sight;
    private DonutProgress accuracy;
    private GalaxyView galaxy;
    private SatelliteView satellite;
//    private ImageView bullsEye;
    private TextView satelliteCountView;
    private Satellite hackTarget;
//    private MediaPlayer noise;
//    private MediaPlayer signal;
//    private MediaPlayer hit;
//    private Vibrator vibrator;
    private Effect effect;

//    //Animators
//    private ObjectAnimator rotateAnimator;
////    private ObjectAnimator scaleXAnimator;
////    private ObjectAnimator scaleYAnimator;
////    private ObjectAnimator alphaAnimator;
////    private Interpolator countDownInterpolator = new DecelerateInterpolator();
//    private static LinearInterpolator linearInterpolator = new LinearInterpolator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = new Intent();
        Log.v(UI_TAG, "Create activity.");
        setContentView(R.layout.activity_satellite_hack);
        Log.d(UI_TAG, "XML done");
        initGame();
        startGame.execute();
    }

    //register listeners when resumed
    @Override
    protected void onResume() {
        super.onResume();
        Log.v(UI_TAG, "Resume activity.");
        if (game.getState().compareTo(OVER) < 0) {
//            satelliteFinder.setShowToast(true);
            finder.setShowToast(true);
//            sm.registerListener(seListener, aSensor, SENSOR_DELAY);
//            sm.registerListener(seListener, mSensor, SENSOR_DELAY);
            slaut.on();
        }
        if (game.getState().equals(ACTIVE)) {
//            noise.start();
//            signal.start();
            effect.start();
        }
    }

    //release listeners when paused
    @Override
    public void onPause() {
        super.onPause();
        Log.v(UI_TAG, "Pause activity.");
        if (game.getState().compareTo(OVER) >= 0)
            finish();
//        satelliteFinder.setShowToast(false);
        finder.setShowToast(false);
//        sm.unregisterListener(seListener);
        slaut.off();
//        noise.pause();
//        signal.pause();
        effect.pause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == SatelliteFinder.GPS_PERMISSION) {
            if (!(grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.e(SatelliteFinder.GPS_TAG, "No GPS permission.");
                setResult(RESULT_CANCELED);
                finish();
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.v(UI_TAG, "Back button is pressed");
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Give up Generator")
                .setMessage("Are you sure you want to exit?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.i(UI_TAG, "User is leaving.");
                        if (game.getState().compareTo(OVER) >= 0)
                            finish();
                        else{
                            gameOver(false, true);
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

//    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
//        for (ImageView iv : new ImageView[]{
//                galaxy, satellite, sight,
//                (ImageView) findViewById(R.id.sight_bg),
//                (ImageView) findViewById(R.id.noise),
//                (ImageView) findViewById(R.id.fail_message)}) {
//            if (null == iv) continue;
//            iv.getDrawable().setCallback(null);
//            iv.setImageDrawable(null);
//            iv.setImageBitmap(null);
//        }
//        galaxy.getDrawable().setCallback(null);
//        satellite.getDrawable().setCallback(null);
//        sight.getDrawable().setCallback(null);
//        ping.getDrawable().setCallback(null);
//        ((ImageView) findViewById(R.id.noise)).getDrawable().setCallback(null);
//        ((ImageView) findViewById(R.id.fail_message)).getDrawable().setCallback(null);
//        ((SurfaceView) findViewById(R.id.camera)).getHolder().getSurface().release();
//        this.releaseInstance();
        releaseLoadingScene();
        releaseGameContent();
        releaseFailScene();
//        thisLayout.removeAllViews();
    }

//    @Override
//    public void finish() {
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
////            finishAndRemoveTask();
////        }
////        else
//            super.finish();
//    }

//    //Sensor Listener
//    private SensorEventListener seListener = new SensorEventListener() {
//        String sensorType, sensorAccuracy;
//        final int SKIP = 10;
//        int count = 0;
//
//        @Override
//        public void onSensorChanged(SensorEvent sensorEvent) {
//            //update too frequently, skip some event
//            if (count-- <= 0) {
//                count = SKIP;
//
//                Log.v(SENSOR_TAG, "Sensor event occurs.");
//                if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
//                    magneticFieldValues = sensorEvent.values;
//                if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
//                    accelerometerValues = sensorEvent.values;
////                new UpdateGame().execute(); //use an asyncTask to update
//                updateGame.execute();
//            }
//        }
//
//        @Override
//        public void onAccuracyChanged(Sensor sensor, int i) {
//            Log.v(SENSOR_TAG, "Accuracy of sensor has changed.");
//            if (sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//                sensorType = "TYPE_MAGNETIC_FIELD";
//            } else { //if(sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                sensorType = "TYPE_ACCELEROMETER";
//            }
//            switch (i) {
//                case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
//                    sensorAccuracy = "high";
//                    break;
//                case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
//                    sensorAccuracy = "medium";
//                    break;
//                case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
//                    sensorAccuracy = "low";
//                    break;
//                case SensorManager.SENSOR_STATUS_UNRELIABLE:
//                    sensorAccuracy = "unreliable";
//                    break;
//                default:
//                    sensorAccuracy = "undefined";
//            }
//            Log.v(SENSOR_TAG, String.format("%s accuracy is %s now.", sensorType, sensorAccuracy));
//        }
//    };

    public void updateSight() {
        if (!finder.isDone()) return;
        Log.v(UI_TAG, "Update sight.");
//        //0 represent the closest, 2 represent the furthest
//        float ratio = (1 - game.getAvgAccuracy()) * 2;
//        scaleXAnimator.cancel();
//        scaleYAnimator.cancel();
//        scaleXAnimator.setFloatValues(ratio);
//        scaleYAnimator.setFloatValues(ratio);
//        scaleXAnimator.start();
//        scaleYAnimator.start();
//        alphaAnimator.cancel();
//        alphaAnimator.setFloatValues(game.getAvgAccuracy());
//        alphaAnimator.start();
        galaxy.setAlpha(game.getAvgAccuracy());

        accuracy.setProgress(game.getAvgAccuracy());
        //see if any satellite is pointed
        boolean accurate = false;
        for (Satellite sat : game.getSatellites()) {
            if (sat.getAccuracy() > 1 - BULLS_EYE) {
                Log.v(GAME_TAG, "A satellite is hackable.");
                accurate = true;
                hackTarget = sat;
//                bullsEye.setImageResource(R.drawable.button);
//                bullsEye.setEnabled(true);
                satellite.setEnabled(true);
                break;
            }
        }
        if (!accurate) {
//            bullsEye.setImageResource(R.drawable.bullseye);
//            bullsEye.setEnabled(false);
            satellite.setEnabled(false);
        }
    }

//    private void initAnimator() {
//        Log.i(UI_TAG, "Initialize animators.");
//        rotateAnimator = ObjectAnimator.ofFloat(sight, "rotation", 900f);
//        rotateAnimator.setDuration(TIME_LIMIT);
////        rotateAnimator.setInterpolator(countDownInterpolator);
//        rotateAnimator.setInterpolator(linearInterpolator);
////        scaleXAnimator = ObjectAnimator.ofFloat(sight, "scaleX", 1);
////        scaleXAnimator.setDuration(200);
////        scaleXAnimator.setInterpolator(linearInterpolator);
////        scaleYAnimator = ObjectAnimator.ofFloat(sight, "scaleY", 1);
////        scaleYAnimator.setDuration(200);
////        scaleYAnimator.setInterpolator(linearInterpolator);
////        alphaAnimator = ObjectAnimator.ofFloat(galaxy, "alpha", 0.5f);
////        alphaAnimator.setDuration(TIME_LIMIT);
////        alphaAnimator.setInterpolator(linearInterpolator);
//    }

//    private void initCamera() {
//        Log.i(UI_TAG, "Set surface view call back.");
//        SurfaceView surfaceView = (SurfaceView) findViewById(R.id.camera);
//        SurfaceHolder surfaceHolder = surfaceView.getHolder();
//        surfaceHolder.setKeepScreenOn(true);
//        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
//            @Override
//            public void surfaceCreated(SurfaceHolder surfaceHolder) {
//                Log.i(UI_TAG, "Turn on camera.");
//                camera = Camera.open();
//                camera.setDisplayOrientation(90);
//                if (null != camera) {
//                    try {
//                        camera.setPreviewDisplay(surfaceHolder);
//                        camera.startPreview();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//
//            @Override
//            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}
//
//            @Override
//            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
//                if (null != camera) {
//                    Log.i(UI_TAG, "Turn off camera.");
//                    camera.stopPreview();
//                    camera.release();
//                    surfaceHolder.getSurface().release();
//                }
//            }
//        });
//    }

//    private void initSound() {
//        Log.v(UI_TAG, "Initialize sound effect.");
//        noise = MediaPlayer.create(this, R.raw.noise);
//        noise.setLooping(true);
//        noise.setVolume(0.5f, 0.5f);
//        signal = MediaPlayer.create(this, R.raw.iwtus);
//        signal.setLooping(true);
//        signal.setVolume(0.5f, 0.5f);
//        hit = MediaPlayer.create(this, R.raw.transmit);
//        hit.setVolume(1, 1);
//    }

//    private void updateSound() {
//        updateSound(game.getAvgAccuracy());
//    }

//    private void updateSound(float accuracy) {
//        Log.v(UI_TAG, "Update sound effect.");
//        int maxVolume = 100;
//        float noiseVol = (float) (log(maxVolume - (1 - accuracy) * 100)
//                / log(maxVolume));
//        float signalVol = 1;
//        if (accuracy > 0.5) {
//            signalVol = (float) (log(maxVolume - (accuracy - 0.5) * 2 * 100)
//                    / log(maxVolume));
//        }
//        noise.setVolume(1 - noiseVol, 1 - noiseVol);
//        signal.setVolume(1 - signalVol, 1 - signalVol);
//    }

    private void initGame() {
        if (null != game.getState())
            throw new StateException("Game is ready.");
        game.setState(READY);
        Log.i(GAME_TAG, "Loading Satellite Hack.");

        //create loading scene by Glide
        Glide.with(this)
                .load(R.drawable.dot_loading_img)
                .asGif()
                .crossFade()
                .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                .into((ImageView) findViewById(R.id.loading));

        //get level and set satellite list
        game.setLevel(
                getIntent().getIntExtra("level", 2)
        );
//        locationManager =
//                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//        satelliteFinder.execute();

        finder = new SatelliteFinder(this, this, handler, game);
        finder.execute();

        //initialize sensor listener
//        sm = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        slaut = new SensorListenerAndUpdateThread(
                SatelliteHackActivity.this, handler, game);

//        //initialize camera
//        initCamera();

        //prepare view objects
//        ping = (PingView) findViewById(R.id.ping);
//        thisLayout = (RelativeLayout) findViewById(R.id.activity_satellite_hack);
//        cameraView = (CameraView) findViewById(R.id.camera);
        stopwatch = (Stopwatch) findViewById(R.id.stopwatch);
        sight = (ImageView) findViewById(R.id.sight);
//        bullsEye = (ImageView) findViewById(R.id.bulls_eye);
        galaxy = (GalaxyView) findViewById(R.id.galaxy);
        satellite = (SatelliteView) findViewById(R.id.satellite);
        accuracy = (DonutProgress) findViewById(R.id.accuracy);
        satelliteCountView = (TextView) findViewById(R.id.satellite_count);

        //initialize effects
//        initAnimator();
//        initSound();
//        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        effect = new Effect(SatelliteHackActivity.this).setAnimationTarget(sight);

        satellite.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hack(null);
            }
        });
    }

    private class StartGame extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            if (game.getState().compareTo(ACTIVE) >= 0)
                throw new StateException("Game has been started.");
            Log.i(GAME_TAG, "Wait til satellites are ready.");
            while (!finder.isDone()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
            slaut.start();
            game.setState(ACTIVE);
            Log.i(GAME_TAG, "Start Satellite Hack.");
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            //start game UI
//            rotateAnimator.start();
//            alphaAnimator.start();
//            noise.start();
//            signal.start();
            effect.start();
            stopwatch.setListener(new Stopwatch.Listener() {
                @Override
                public void onTimesUp() {
                    try {
                        Log.i(GAME_TAG, "Satellite Hack fail.(timer)");
                        gameOver(false);
                    } catch (StateException e) {
                        Log.v(GAME_TAG, "Game is over");
                    }
                }

                @Override
                public void onEvery100ms(long time) {
                    Log.v(GAME_TAG, String.format("%dms left", time));
                    if (time < TIME_LIMIT / 2)
                        stopwatch.post(new Runnable() {
                            @Override
                            public void run() {
                                stopwatch.setTextColor(
                                        getResources().getColor(R.color.red));
                            }
                        });
                }
            }).start(TIME_LIMIT);
//            //start counting time  *fix delay and last tick bug
//            new CountDownTimer(TIME_LIMIT + 400, 1000) {
//                @Override
//                public void onTick(long l) {
//                    Log.v(GAME_TAG, String.format("%dms left", l));
////                    ping.decreasePing();
//                }
//
//                @Override
//                public void onFinish() {
//                    try {
//                        Log.i(GAME_TAG, "Satellite Hack fail.(timer)");
//                        gameOver(false);
//                    } catch (StateException e) {
//                        Log.v(GAME_TAG, "Game is over");
//                    }
//                }
//            }.start();
            game.startTimer();
            //remove loading scene
//            ImageView loading = ((ImageView) findViewById(R.id.loading));
//            loading.getDrawable().setCallback(null);
//            loading.setImageDrawable(null);
//            findViewById(R.id.loading_bg).setVisibility(View.GONE);
//            findViewById(R.id.loading).setVisibility(View.GONE);
            releaseLoadingScene();
        }

    }

//    private class UpdateGame extends AsyncTask<Void, Void, Void> {
//        private float[] r = new float[9];
//        private float[] orientations = new float[3];
//        private float azimuth, inclination;
//
//        @Override
//        protected Void doInBackground(Void... voids) {
//            //get orientation to calculate accuracies
//            Log.v(SENSOR_TAG, "Get azimuth and inclination for calculation.");
//            SensorManager.getRotationMatrix(r, null, accelerometerValues, magneticFieldValues);
//            SensorManager.getOrientation(r, orientations);
//            azimuth = getCameraAzimuth(orientations[0], orientations[1], orientations[2]);
//            inclination = getInclination(orientations[1], orientations[2]);
//            game.setAccuracies(azimuth, inclination);
//            return null;
//        }
//
//        @Override
//        protected void onPostExecute(Void aVoid) {
//            if (!game.getState().equals(ACTIVE)) return;
//            Log.v(UI_TAG, "Update UI to show accuracy");
//            updateSight();
////            updateSound(game.getAvgAccuracy());
//            effect.updateSound(game.getAvgAccuracy());
//            satelliteCountView.setText(String.valueOf(game.countSatellite()));
//             ((TextView) findViewById(R.id.ga)).setText(
//                    String.valueOf(game.getAvgAccuracy()));
//            showOrientation(azimuth, inclination);
//        }
//    }

//    private class UpdateGame extends Thread {
//        private float[] r = new float[9];
//        private float[] orientations = new float[3];
//        private float azimuth, inclination;
//        private Handler handler;
//
//        public UpdateGame(Handler handler) {
//            this.handler = handler;
//        }
//
//        public synchronized void execute() {
//            this.notifyAll();
//        }
//
//        @Override
//        public void run() {
//            while(true) {
//                synchronized (this) {
//                    try {
//                        this.wait(1000);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//                if (game.getState().equals(ACTIVE)) {
//                    calculate();
//                    handler.post(updateUI);
//                }
//                else if (game.getState().compareTo(ACTIVE) > 0)
//                    return;
//            }
//        }
//
//        private void calculate() {
//            //get orientation to calculate accuracies
//            Log.v(SENSOR_TAG, "Get azimuth and inclination for calculation.");
//            SensorManager.getRotationMatrix(r, null, accelerometerValues, magneticFieldValues);
//            SensorManager.getOrientation(r, orientations);
//            azimuth = getCameraAzimuth(orientations[0], orientations[1], orientations[2]);
//            inclination = getInclination(orientations[1], orientations[2]);
//            game.setAccuracies(azimuth, inclination);
//        }
//
//        private Runnable updateUI = new Runnable() {
//            @Override
//            public void run() {
//                Log.v(UI_TAG, "Update UI to show accuracy");
//                updateSight();
//                effect.updateSound(game.getAvgAccuracy());
//                satelliteCountView.setText(String.valueOf(game.countSatellite()));
//                ((TextView) findViewById(R.id.ga)).setText(
//                        String.valueOf(game.getAvgAccuracy()));
//                showOrientation(azimuth, inclination);
//            }
//        };
//    }

    public void updateUI(float azimuth, float inclination) {
        Log.v(UI_TAG, "Update UI to show accuracy");
        updateSight();
        effect.updateSound(game.getAvgAccuracy());
        satelliteCountView.setText(String.valueOf(game.countSatellite()));
        ((TextView) findViewById(R.id.ga)).setText(
                String.valueOf(game.getAvgAccuracy()));
        showOrientation(azimuth, inclination);
    }

    public void hack(View view) {
//        if (game.getState().equals(STARTED))
//            throw new StateOverException("Game is over or not started yet");
//        if (null == hackTarget)
//            throw new NoTargetException("No hacking target");
//        //try to remove target satellite
//        if (game.removeSatellite(hackTarget)) {
//            vibrator.vibrate(200);
//            hit.start();
//            int satelliteCount = game.countSatellite();
//            Log.i(GAME_TAG, String.format(
//                    "Satellite hack success. %d satellite(s)' left.", satelliteCount));
//            satelliteCountView.setText(String.valueOf(game.countSatellite()));
//            if (satelliteCount == 0) {
//                Log.i(GAME_TAG, "All satellites are hacked. Stage clear.");
//                gameOver(true);
//            }
//            hackTarget = null;
//        }
//        new Hack().execute();
        new AsyncTask<Void, Integer, Void>() {

            @Override
            protected void onPreExecute() {
                satellite.setEnabled(false);
            }

            @Override
            protected Void doInBackground(Void... aVoid) {
                if (game.getState().equals(READY))
                    throw new StateException("Game not started yet");
                if (null == hackTarget)
                    throw new NoTargetException("No hacking target");
                //try to remove target satellite
                if (game.removeSatellite(hackTarget)) {
//                    vibrator.vibrate(200);
//                    hit.start();
                    effect.hit();
                    int satelliteCount = game.countSatellite();
                    Log.i(GAME_TAG, String.format(
                            "Satellite hack success. %d satellite(s)' left.", satelliteCount));
                    publishProgress(game.countSatellite());
                    if (satelliteCount == 0) {
                        Log.i(GAME_TAG, "All satellites are hacked. Stage clear.");
                        gameOver(true);
                    }
                    hackTarget = null;
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                satelliteCountView.setText(String.valueOf(values[0]));
            }
        }.execute();
    }

//    private class Hack extends AsyncTask<Void, Integer, Void> {
//
//        @Override
//        protected void onPreExecute() {
//            satellite.setEnabled(false);
//        }
//
//        @Override
//        protected Void doInBackground(Void... aVoid) {
//            if (game.getState().equals(READY))
//                throw new StateException("Game not started yet");
//            if (null == hackTarget)
//                throw new NoTargetException("No hacking target");
//            //try to remove target satellite
//            if (game.removeSatellite(hackTarget)) {
//                vibrator.vibrate(200);
//                hit.start();
//                int satelliteCount = game.countSatellite();
//                Log.i(GAME_TAG, String.format(
//                        "Satellite hack success. %d satellite(s)' left.", satelliteCount));
//                publishProgress(game.countSatellite());
//                if (satelliteCount == 0) {
//                    Log.i(GAME_TAG, "All satellites are hacked. Stage clear.");
//                    gameOver(true);
//                }
//                hackTarget = null;
//            }
//            return null;
//        }
//
//        @Override
//        protected void onProgressUpdate(Integer... values) {
//            satelliteCountView.setText(String.valueOf(values[0]));
//        }
//    }

    //Exception when no hackable satellite but try to hack
    private static class NoTargetException extends RuntimeException {
        NoTargetException(String message){
            super(message);
        }
    }

    //return result to main activity
    private void gameOver(boolean success) {
        gameOver(success, false);
    }

    private void gameOver(boolean success, boolean finish) {
//        if (game.getState().compareTo(STARTED) < 0)
//            throw new StateException("Game is over.");
        if (game.getState().compareTo(OVER) >= 0)
            return;
        Log.i(GAME_TAG, "Satellite Hack is over.");
        game.setState(OVER);
//        sm.unregisterListener(seListener);
        game.stopTimer();

        if (!success) {
            Log.i(GAME_TAG, "User failed to find all satellites.");
            game.setState(FAILURE);
            intent.putExtra(RESULT, false);
            intent.putExtra(TIME, game.getTimeUsed());
            setResult(RESULT_OK, intent);
            if (finish) {
                finish();
                return;
            }

            //display noise scene to tell failure
            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    Glide.with(SatelliteHackActivity.this)
                                            .load(R.drawable.noise_gif)
                                            .asGif()
                                            .placeholder(R.color.black)
                                            .crossFade()
                                            .diskCacheStrategy(DiskCacheStrategy.SOURCE)
                                            .into((ImageView) findViewById(R.id.noise));
                                    effect.updateSound(0f);
                                    findViewById(R.id.failure).setVisibility(View.VISIBLE);
                                    Toast.makeText(SatelliteHackActivity.this,
                                            "Game Over. Press BACK to leave",
                                            Toast.LENGTH_LONG).show();
                                }
                            });
        }
        else {
            Log.i(GAME_TAG, "User succeeds.");
            game.setState(SUCCESS);
            intent.putExtra(RESULT, true);
            intent.putExtra(TIME, game.getTimeUsed());
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void releaseLoadingScene() {
        findViewById(R.id.loading_bg).setVisibility(View.GONE);
        ImageView loading = ((ImageView) findViewById(R.id.loading));
        if (null == loading || null == loading.getDrawable()) return;
        loading.setVisibility(View.GONE);
        loading.getDrawable().setCallback(null);
        loading.setImageDrawable(null);
//        if (null != satelliteFinder && !satelliteFinder.isCancelled())
//            satelliteFinder.cancel(false);
        if (null != finder && !finder.isCancelled())
            finder.cancel(false);
        if (null != startGame && !startGame.isCancelled())
            startGame.cancel(false);
    }

    private void releaseGameContent() {
        stopwatch.stop();
        for (ImageView iv : new ImageView[]{
                galaxy, satellite, sight,
                (ImageView) findViewById(R.id.sight_bg)}) {
            if (null == iv || null == iv.getDrawable()) continue;
            iv.setVisibility(View.GONE);
            iv.getDrawable().setCallback(null);
            iv.setImageDrawable(null);
            iv.setImageBitmap(null);
        }
    }

    private void releaseFailScene() {
        for (ImageView iv : new ImageView[]{
                (ImageView) findViewById(R.id.noise),
                (ImageView) findViewById(R.id.fail_message)}) {
            if (null == iv || null == iv.getDrawable()) continue;
            iv.setVisibility(View.GONE);
            iv.getDrawable().setCallback(null);
            iv.setImageDrawable(null);
            iv.setImageBitmap(null);
        }
    }

    //debug display
    int count = 0;
    boolean listening = false;

    public void showDebug(View view) {
        count++;
        if (count > 20) gameOver(true);
        if (count > 3)
            findViewById(R.id.developerView)
                    .setVisibility(View.VISIBLE);
        if (!listening && count >= 2) {
            view.setOnLongClickListener(removeCamera);
            listening = true;
        }
    }

    public View.OnLongClickListener removeCamera =
            new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            ((RelativeLayout)
                    (findViewById(R.id.camera).getParent()
            )).removeView(findViewById(R.id.camera));
            new SurfaceView(null).
            findViewById(R.id.author).setOnLongClickListener(null);
            return false;
        }
    };

    //test satellite fetching
    public void showSatellite(List<Satellite> satellites) {
        if (!finder.isDone()) return;
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
    public void showOrientation(float azimuth, float inclination) {
        ((TextView) findViewById(R.id.converted_azimuth)).setText(
                decimalFormat.format(azimuth));
        ((TextView) findViewById(R.id.converted_inclination)).setText(
                decimalFormat.format(inclination));
    }

}