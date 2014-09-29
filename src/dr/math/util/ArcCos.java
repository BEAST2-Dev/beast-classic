package dr.math.util;

import static java.lang.Math.sqrt;

/** Approximations for arccos **/
public class ArcCos {
    // coefficients for series expansion
    static final double c_3 = 1.0 / 6.0;
    static final double c_5 = 3.0 / 40.0;
    static final double c_7 = 5.0 / 112.0;
    static final double c_9 = 35.0 / 1152.0;
    static final double c_11 = 63.0 / 2816.0;
    static final double c_13 = 231.0 / 13312;

    static final double halfpi = Math.PI / 2;

    //  first 5 terms from the classic expansion (eq (18) in http://mathworld.wolfram.com/InverseCosine.html)
    static double acos_fast5(double x) {
        assert -1 <= x && x <= 1;
        final double u2 = x * x;
        final double u3 = u2 * x;
        final double u5 = u3 * u2;
        final double u7 = u5 * u2;
        final double u9 = u7 * u2;

        return halfpi - x - c_3 * u3 - c_5 * u5 - c_7 * u7 - c_9 * u9;
    }

    //  first 7 terms from the classic expansion
    static double acos_fast7(double x) {
        assert -1 <= x && x <= 1;
        final double u2 = x * x;
        final double u3 = u2 * x;
        final double u5 = u3 * u2;
        final double u7 = u5 * u2;
        final double u9 = u7 * u2;
        final double u11 = u9 * u2;
        final double u13 = u11 * u2;

        return halfpi - x - c_3 * u3 - c_5 * u5 - c_7 * u7 - c_9 * u9 - c_11 * u11 - c_13 * u13;
    }
    // arctan using first 3 terms of series
    private static double atan_fast3(double x) {
        if( x < 0 ) {
            x = -x;
            x = x-1;
            return  -(Math.PI/4 + x/2 - x*x/4);
        }
        x = x-1;
        return  Math.PI/4 + x/2 - x*x/4;
    }

    // arccos by doing an arctan on a transformed parameter

    private static double acosViaAtan(double x) {
        if (x == 1) return 0;
        if (x == -1) return Math.PI;
        // equation (12)
        final double v = x/sqrt(1-x*x);

        return halfpi - atan_fast3(v);
    }

    private static double th = sqrt(2.0)/2;
    // acos_fast5 is less accurate when |x| > 0.5. for |x| > sqrt(2)/2 we can use equation (8) to compute the acos
    // from a value in the range |x| < sqrt(2)/2.

    static double acos_parts_fast5(double x) {
        if ( x > th ) {
            return (halfpi - acos_fast5(sqrt(1 - x * x)));
        }
        else if ( x < -th ) {
            return halfpi +  acos_fast5(sqrt(1 - x * x));
        }
        else {
            return acos_fast5(x);
        }
    }

    static public double acos_parts5_atan(double x) {
        // in this range, which is more problematic for acos_parts_fast5, going through atan is more accurate
        if ( (.65 <= x && x <= .75) || (-0.75 <= x && x <= -0.65) ) {
            return acosViaAtan(x);
        }
        return acos_parts_fast5(x);
    }

    static public double acos_parts_fast7(double x) {
        if ( x > th ) {
            return (halfpi - acos_fast7(sqrt(1 - x * x)));
        }
        else if ( x < -th ) {
            return halfpi +  acos_fast7(sqrt(1 - x * x));
        }
        else {
            return acos_fast7(x);
        }
    }

    static public double acos_parts7_atan(double x) {
        // in this range, which is more problematic for acos_parts_fast5, going through atan is more accurate
        if ( (.65 <= x && x <= .75) || (-0.75 <= x && x <= -0.65) ) {
            return acosViaAtan(x);
        }
        return acos_parts_fast7(x);
    }
}
