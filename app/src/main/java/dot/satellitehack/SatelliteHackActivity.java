package dot.satellitehack;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
    private static final String UI_TAG = "UI";

    public static final String RESULT = "SatelliteHackResult";
    public static final String TIME = "SatelliteHackTime";

    //game managers
    private Handler handler = new Handler();
    private SatelliteHackGame game = new SatelliteHackGame();
    private StartGame startGame = new StartGame();
    private Intent intent;

    //GPS system variables
    SatelliteFinder finder;

    //Sensor variables
    private SensorListenerAndUpdateThread slaut;

    //UI variables
    private Stopwatch stopwatch;
    private ImageView sight;
    private DonutProgress accuracy;
    private GalaxyView galaxy;
    private SatelliteView satellite;
    private TextView satelliteCountView;
    private Satellite hackTarget;
    private Effect effect;

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
            finder.setShowToast(true);
            slaut.on();
        }
        if (game.getState().equals(ACTIVE)) {
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
        finder.setShowToast(false);
        slaut.off();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(UI_TAG, "Destroy activity");
        releaseLoadingScene();
        releaseGameContent();
        releaseFailScene();
    }

    public void updateSight() {
        if (!finder.isDone()) return;
        Log.v(UI_TAG, "Update sight.");
        galaxy.setAlpha(game.getAvgAccuracy());

        accuracy.setProgress(game.getAvgAccuracy());
        //see if any satellite is pointed
        boolean accurate = false;
        for (Satellite sat : game.getSatellites()) {
            if (sat.getAccuracy() > 1 - BULLS_EYE) {
                Log.v(GAME_TAG, "A satellite is hackable.");
                accurate = true;
                hackTarget = sat;
                satellite.setEnabled(true);
                break;
            }
        }
        if (!accurate) {
            satellite.setEnabled(false);
        }
    }

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
        finder = new SatelliteFinder(this, this, handler, game);
        finder.execute();

        //initialize sensor listener
        slaut = new SensorListenerAndUpdateThread(
                SatelliteHackActivity.this, handler, game);

        //prepare view objects
        stopwatch = (Stopwatch) findViewById(R.id.stopwatch);
        sight = (ImageView) findViewById(R.id.sight);
//        bullsEye = (ImageView) findViewById(R.id.bulls_eye);
        galaxy = (GalaxyView) findViewById(R.id.galaxy);
        satellite = (SatelliteView) findViewById(R.id.satellite);
        accuracy = (DonutProgress) findViewById(R.id.accuracy);
        satelliteCountView = (TextView) findViewById(R.id.satellite_count);

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
            game.startTimer();
            releaseLoadingScene();
        }

    }

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
        if (game.getState().compareTo(OVER) >= 0)
            return;
        Log.i(GAME_TAG, "Satellite Hack is over.");
        game.setState(OVER);
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
        Log.i(UI_TAG, "Release loading scene memory");
        findViewById(R.id.loading_bg).setVisibility(View.GONE);
        ImageView loading = ((ImageView) findViewById(R.id.loading));
        if (null == loading || null == loading.getDrawable()) return;
        loading.setVisibility(View.GONE);
        loading.getDrawable().setCallback(null);
        loading.setImageDrawable(null);
        if (null != finder && !finder.isCancelled())
            finder.cancel(false);
        if (null != startGame && !startGame.isCancelled())
            startGame.cancel(false);
    }

    private void releaseGameContent() {
        Log.i(UI_TAG, "Release loading scene memory");
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
        Log.i(UI_TAG, "Release loading scene memory");
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