package dot.satellitehack;

import android.location.Criteria;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;
import static java.lang.Float.NaN;

/**
 * Created by dixon on 13/2/2017.
 */

class MathTools {
    static DecimalFormat decimalFormat = new DecimalFormat("0.0");

    static Random random = new Random();

    static float getCameraAzimuth(float azimuth, float pitch, float roll) {
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

    static float getInclination(float pitch, float roll) {
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

    static double[] quadraticFormula(double a, double b, double c) {
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

    static double discriminant(double a, double b, double c) {
        return b * b - 4 * a * c;
    }

    static boolean isZero(double value) {
        final double threshold = 0.000001;
        return value >= -threshold && value <= threshold;
    }

    static double positiveMod(double dividend, double divisor) {
        double remainder = dividend % divisor;
        if (remainder < 0) remainder += divisor;
        return remainder;
    }

    static float average(List<Float> numbers) {
        if (numbers.size() == 0)
            return 0;
        return average(numbers, 0, 0);
    }

    static float average(List<Float> numbers, float sum, int length) {
        if (numbers.size() == 0)
            return sum/length;
        sum += numbers.remove(0);
        return average(numbers, sum, length + 1);
    }
}
