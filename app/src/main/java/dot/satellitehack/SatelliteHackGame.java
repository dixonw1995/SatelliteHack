package dot.satellitehack;

import android.text.format.Time;

import java.util.ArrayList;
import java.util.List;

import static dot.satellitehack.Tools.*;

/**
 * Created by dixon on 13/2/2017.
 */

@SuppressWarnings("deprecation")
class SatelliteHackGame {
    private int level;
    private List<Satellite> satellites;
    private Time startTime;
    private Time endTime;
    public static final int TIME_LIMIT = 20000;
    public static final float BULLS_EYE = 0.15f;

    public SatelliteHackGame() {
        satellites = new ArrayList<>();
        startTime = new Time("GMT+8");
        endTime = new Time("GMT+8");
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public List<Satellite> getSatellites() {
        return satellites;
    }

    public void setSatellites(List<Satellite> satellites) {
        this.satellites = satellites;
    }

    public void addSatellite(Satellite satellite) {
        satellites.add(satellite);
    }

    public Satellite getSatellite(int index) {
        return satellites.get(index);
    }

    public boolean removeSatellite(Satellite satellite) {
        return satellites.remove(satellite);
    }

    public int countSatellite() {
        return satellites.size();
    }

    public List<Float> getAccuracies() {
        return Satellite.getAccuracies(satellites);
    }

    public float getAvgAccuracy() {
        return average(getAccuracies());
    }

    public Time getStartTime() {
        return startTime;
    }

    public SatelliteHackGame startTimer() {
        startTime.setToNow();
        return this;
    }

    public Time getEndTime() {
        return endTime;
    }

    public SatelliteHackGame stopTimer() {
        endTime.setToNow();
        return this;
    }

    public int getTimeUsed() {
        return (endTime.minute - startTime.minute) * 60
                + endTime.second - startTime.second;
    }
}
