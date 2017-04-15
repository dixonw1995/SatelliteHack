package dot.satellitehack;

import android.location.GpsSatellite;
import android.text.format.Time;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static dot.satellitehack.MathTools.*;

enum State {
    READY, ACTIVE, OVER, SUCCESS, FAILURE;

    //exception when operation doesn't match state
    static class StateException extends RuntimeException {
        StateException(String message){
            super(message);
        }
    }
}

@SuppressWarnings("deprecation")
class SatelliteHackGame {
    private int level;
    private List<Satellite> satellites;
    private Time startTime;
    private Time endTime;
    private State state = null;
    static final int TIME_LIMIT = 20000;
    static final float BULLS_EYE = 0.15f;

    SatelliteHackGame() {
        satellites = new ArrayList<>();
        startTime = new Time("GMT+8");
        endTime = new Time("GMT+8");
    }

    void setLevel(int level) {
        this.level = level;
    }

    List<Satellite> getSatellites() {
        return satellites;
    }

    private void addSatellite(Satellite satellite) {
        satellites.add(satellite);
    }

    void addSatellites(List<GpsSatellite> satList) {
        if (countSatellite() >= level || satList.size() <= 0) return;
        addSatellite(
                new Satellite(
                        satList.remove(
                                random.nextInt(satList.size())
                        )
                )
        );
        addSatellites(satList);
    }

    boolean removeSatellite(Satellite satellite) {
        return satellites.remove(satellite);
    }

    int countSatellite() {
        return satellites.size();
    }

    private List<Float> getAccuracies() {
        return Satellite.getAccuracies(satellites);
    }

    float getAvgAccuracy() {
        return average(getAccuracies());
    }

    void setAccuracies(float azimuth, float inclination) {
        for (Satellite sat : satellites) {
            sat.setAccuracy(azimuth, inclination);
        }
    }

    SatelliteHackGame startTimer() {
        startTime.setToNow();
        return this;
    }

    SatelliteHackGame stopTimer() {
        endTime.setToNow();
        return this;
    }

    int getTimeUsed() {
        return (endTime.minute - startTime.minute) * 60
                + endTime.second - startTime.second;
    }

    State getState() {
        return state;
    }

    void setState(State state) {
        this.state = state;
    }

    @Override
    public String toString() {
        return String.format(Locale.getDefault(), "Satellite Hack Game Lv.%d. State: %s", level, state);
    }
}
