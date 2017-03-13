package dot.satellitehack;

import android.location.GpsSatellite;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Math.abs;

@SuppressWarnings("deprecation")
class Satellite{
    private int prn;
    private float azimuth;
    private float elevation;
    private float accuracy;

    Satellite(GpsSatellite satellite) {
        prn = satellite.getPrn();
        azimuth = satellite.getAzimuth();
        elevation = satellite.getElevation();
    }

    int getPrn() {
        return prn;
    }

    float getAzimuth() {
        return azimuth;
    }

    float getElevation() {
        return elevation;
    }

    float getAccuracy() {
        return accuracy;
    }

    void setAccuracy(float azimuth, float inclination) {
        /*
        The best difference is 180. The worst is 0 or 360 which means the opposite direction.
        The equation gives the rate of how close the user's pointed at the satellite.
        accuracy rate = (180 - | | azimuth - satellite_azimuth | - 180 |) / 180
        */
        float aziAccuracy = (180 - abs(abs(azimuth - this.azimuth) - 180)) / 180;
        /*
        The best sum is 90. The worst is when user points at the floor(180 + X = 180 ~ 270)
        The equation gives the rate of how close the user's pointed to the satellite
        accuracy rate = (180 - | inclination + elevation - 90 |) / 180
        */
        float incAccuracy = (180 - abs((inclination + elevation) - 90)) / 180;
        accuracy = (aziAccuracy + incAccuracy) / 2;
    }

    static List<Float> getAccuracies(List<Satellite> satellites) {
        List<Float> accuracies = new ArrayList<>();
        for (Satellite sat: satellites) {
            accuracies.add(sat.accuracy);
        }
        return accuracies;
    }
}