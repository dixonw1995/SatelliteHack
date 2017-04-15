package dot.satellitehack;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;

import static dot.satellitehack.SatelliteHackGame.TIME_LIMIT;
import static java.lang.Math.log;

class Effect {
    //sound & vibration
    private MediaPlayer noise;
    private MediaPlayer signal;
    private MediaPlayer hit;
    private Vibrator vibrator;

    //Animators
    private ObjectAnimator rotate;
    private static LinearInterpolator linearInterpolator = new LinearInterpolator();

    private static final String UI_TAG = "UI";

    Effect(Context context) {
        init(context);
    }

    private void init(Context context) {
        Log.v(UI_TAG, "Initialize sound effect.");
        noise = MediaPlayer.create(context, R.raw.noise);
        noise.setLooping(true);
        noise.setVolume(0.5f, 0.5f);
        signal = MediaPlayer.create(context, R.raw.iwtus);
        signal.setLooping(true);
        signal.setVolume(0.5f, 0.5f);
        hit = MediaPlayer.create(context, R.raw.transmit);
        hit.setVolume(1, 1);
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        Log.i(UI_TAG, "Initialize animators.");
        rotate = ObjectAnimator.ofFloat(null, "rotation", 900f);
        rotate.setDuration(TIME_LIMIT);
        rotate.setInterpolator(linearInterpolator);
    }

    Effect setAnimationTarget(View view) {
        rotate.setTarget(view);
        return Effect.this;
    }

    void pause() {
        noise.pause();
        signal.pause();
    }

    void start() {
        noise.start();
        signal.start();
        if (null != rotate.getTarget() && !rotate.isStarted())
            rotate.start();
    }

    void updateSound(float accuracy) {
        Log.v(UI_TAG, "Update sound effect.");
        int maxVolume = 100;
        float noiseVol = (float) (log(maxVolume - (1 - accuracy) * 100)
                / log(maxVolume));
        float signalVol = 1;
        if (accuracy > 0.5) {
            signalVol = (float) (log(maxVolume - (accuracy - 0.5) * 2 * 100)
                    / log(maxVolume));
        }
        noise.setVolume(1 - noiseVol, 1 - noiseVol);
        signal.setVolume(1 - signalVol, 1 - signalVol);
    }

    void hit() {
        vibrator.vibrate(200);
        hit.start();
    }
}
