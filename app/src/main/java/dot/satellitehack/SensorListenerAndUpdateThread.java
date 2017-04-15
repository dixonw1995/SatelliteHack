package dot.satellitehack;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.hardware.SensorEventListener;
import android.os.Handler;
import android.util.Log;

import static dot.satellitehack.MathTools.getCameraAzimuth;
import static dot.satellitehack.MathTools.getInclination;
import static dot.satellitehack.State.ACTIVE;

class SensorListenerAndUpdateThread extends Thread
        implements SensorEventListener {
    //Sensor variables
    private SensorManager sm;
    private Sensor aSensor;
    private Sensor mSensor;
    private static final int SENSOR_DELAY =
            SensorManager.SENSOR_DELAY_GAME;
    private float[] accelerometerValues = new float[3];
    private float[] magneticFieldValues = new float[3];
    private Context context;
    private Handler handler;
    private SatelliteHackGame game;

    private float[] r = new float[9];
    private float[] orientations = new float[3];
    private float azimuth, inclination;
    private final static int SKIP = 6;
    private int count = 0;
    private static final String SENSOR_TAG = "Sensor";

    SensorListenerAndUpdateThread(Context context, Handler handler,
                                  SatelliteHackGame game) {
        this.context = context;
        this.handler = handler;
        this.sm = (SensorManager)
                context.getSystemService(Context.SENSOR_SERVICE);
        this.aSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.mSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        this.game = game;
    }

    public void on() {
        sm.registerListener(this, aSensor, SENSOR_DELAY);
        sm.registerListener(this, mSensor, SENSOR_DELAY);
    }

    public void off() {
        sm.unregisterListener(this, aSensor);
        sm.unregisterListener(this, mSensor);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        //update too frequently, skip some event
        if (count-- <= 0) {
            count = SKIP;

            Log.v(SENSOR_TAG, "Sensor event occurs.");
            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
                magneticFieldValues = sensorEvent.values;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
                accelerometerValues = sensorEvent.values;
            this.execute();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        String sensorType, sensorAccuracy;
        Log.v(SENSOR_TAG, "Accuracy of sensor has changed.");
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
        Log.v(SENSOR_TAG, String.format("%s accuracy is %s now.",
                sensorType, sensorAccuracy));
    }

    private synchronized void execute() {
        this.notifyAll();
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    this.wait(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (game.getState().equals(ACTIVE)) {
                calculate();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        ((SatelliteHackActivity) context)
                                .updateUI(azimuth, inclination);
                    }
                });
            } else if (game.getState().compareTo(ACTIVE) > 0)
                return;
        }
    }

    private void calculate() {
        //get orientation to calculate accuracies
        Log.v(SENSOR_TAG, "Get azimuth and inclination for calculation.");
        SensorManager.getRotationMatrix(r, null,
                accelerometerValues, magneticFieldValues);
        SensorManager.getOrientation(r, orientations);
        azimuth = getCameraAzimuth(orientations[0], orientations[1], orientations[2]);
        inclination = getInclination(orientations[1], orientations[2]);
        game.setAccuracies(azimuth, inclination);
    }
}
