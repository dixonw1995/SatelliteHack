package dot.satellitehack;

import java.util.List;

import static java.lang.Math.sqrt;

/**
 * Created by dixon on 13/2/2017.
 */

class Tools {
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
