package dot.satellitehack;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SuppressWarnings("deprecation")
//wait until user turn on GPS and then get the satellites
class SatelliteFinder extends AsyncTask<Void, Void, Void> {
    private Activity activity;
    private Context context;
    private Handler handler;
    private LocationManager locationManager;
    private SatelliteHackGame game;
    private Toast message;
    private boolean showToast = true;
    private boolean done = false;
    static final String GPS_TAG = "GPS";
    static final int GPS_PERMISSION = 695;

    SatelliteFinder(Activity activity, Context context,
                    Handler handler, SatelliteHackGame game) {
        this.activity = activity;
        this.context = context;
        this.handler = handler;
        this.game = game;
    }

    void setShowToast(boolean showToast) {
        this.showToast = showToast;
    }

    public boolean isDone() {
        return done;
    }

    @SuppressLint("ShowToast")
    @Override
    protected void onPreExecute() {
        message = Toast.makeText(
                context,
                "Please turn on GPS...",
                Toast.LENGTH_SHORT);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        while (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.v(GPS_TAG, "GPS is off.");
            if (showToast)
                publishProgress();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }
        }
        Log.i(GPS_TAG, "GPS is on.");
        //check permission
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(context,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.READ_CONTACTS},
                    GPS_PERMISSION);
        }
        //register location and gps status listener
        final String bestProvider = locationManager.getBestProvider(initCriteria(), true);
        handler.post(new Runnable() {
            @Override
            public void run() {
                locationManager.addGpsStatusListener(gsListener);
                locationManager.requestLocationUpdates(
                        bestProvider, 2000, 1, locationListener);
            }
        });
        return null;
    }

    @Override
    protected void onProgressUpdate(Void... voids) {
        message.show();
    }

    @Override
    protected void onCancelled() {
        locationManager.removeGpsStatusListener(gsListener);
        locationManager.removeUpdates(locationListener);
    }

    //GPS status listener
    private GpsStatus.Listener gsListener = new GpsStatus.Listener() {

        @Override
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    Log.i(GPS_TAG, "GPS system has received its first fix.");
                    break;
                //GPS satellite status changed
                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.i(GPS_TAG, "GPS system report status.");
                    if (ActivityCompat.checkSelfPermission(context,
                            Manifest.permission.ACCESS_FINE_LOCATION) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(context,
                                    Manifest.permission.ACCESS_COARSE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(activity,
                                new String[]{Manifest.permission.READ_CONTACTS},
                                GPS_PERMISSION);
                    }

                    Log.i(GPS_TAG, "Set satellite list.");
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
                    Log.i(GPS_TAG, "GPS system has stopped.");
                    done = true;
                    ((SatelliteHackActivity) activity).showSatellite(game.getSatellites());
                    break;
                case GpsStatus.GPS_EVENT_STARTED:
                    Log.i(GPS_TAG, "GPS system has started.");
                    break;
                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.i(GPS_TAG, "GPS system has stopped.");
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
                    Log.v(GPS_TAG, "GPS system is available");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.v(GPS_TAG, "GPS system is out of service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.v(GPS_TAG, "GPS system is temporarily unavailable");
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
}