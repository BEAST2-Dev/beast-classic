/*
 * GreatCircleDiffusionModel.java
 *
 * Copyright (C) 2002-2009 Alexei Drummond and Andrew Rambaut
 *
 * This file is part of BEAST.
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership and licensing.
 *
 * BEAST is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * BEAST is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beast.continuous;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.ContinuousSubstitutionModel;
import dr.math.util.ArcCos;

// while FastMath is supposed to be faster, this is not evident in a small timing test.
//import static org.apache.commons.math3.util.FastMath;
import static java.lang.Math.*;

@Description("Diffusion model that assumes a normal diffusion process on a sphere")
public class SphericalDiffusionModel extends ContinuousSubstitutionModel {
    public Input<RealParameter> precisionInput = new Input<RealParameter>("precision", "precision of diffusion process", Validate.REQUIRED);
    public Input<Boolean> m_fast = new Input<>("fast", "Use an approximation for arccos for angles close to 0 "
    + "(|cos(x) > 0.9). In this range the approximation has an error of at most 1e-10, " +
            "and is faster than the Java version.", false);

    RealParameter precision;
    boolean fast = false;

    @Override
    public void initAndValidate() throws Exception {
        precision = precisionInput.get();
        fast = m_fast.get();

        super.initAndValidate();
    }

    @Override
    public double getLogLikelihood(double[] start, double[] stop, double time) {

        if( time <= 1e-20 ) {
            return -1e100;
        }
        if( start[0] == stop[0] && start[1] == stop[1] ) {
            return -1e100;
        }

        // assumes start = {latitude, longitude}
        // assumes stop = {latitude, longitude}
        // and -90 < latitude < 90, -180 < longitude < 180

        double latitude1 = start[0];
        double longitude1 = start[1];
        final double DEG2RAD = Math.PI / 180.0;
        final double theta1 = (latitude1) * DEG2RAD;
        if( longitude1 < 0 ) longitude1 += 360;
        //final double phi1 = longitude1 * DEG2RAD;

        double latitude2 = stop[0];
        double longitude2 = stop[1];
        final double theta2 = (latitude2) * DEG2RAD;
        if( longitude2 < 0 ) longitude2 += 360;
        //final double phi2 = longitude2 * DEG2RAD;

        final double deltaLambda = (longitude2 - longitude1) * DEG2RAD; //phi2 - phi1, in radians;

        // Use trigonometric equalities to reduce cost of computing both sin(x)*sin(y) and cos(x)*cos(y)
        // to two cos() calls and 4 +/- and one '/2'.
        final double cosplus = cos(theta1 + theta2);
        final double cosminus = cos(theta1 - theta2);
        final double twicecoscos = (cosminus + cosplus);
        final double twicesinsin = (cosminus - cosplus);
        final double x = (twicesinsin + twicecoscos * cos(deltaLambda)) / 2;
//        final double x = FastMath.sin(theta1) * FastMath.sin(theta2) +
//                FastMath.cos(theta1) * FastMath.cos(theta2) * FastMath.cos(deltaLambda);
        final double angle;
        if( fast ) {
          angle = (abs(x) > .9 ? ArcCos.acos_parts_fast7(x) : acos(x));
        } else {
           angle = acos(x);
        }

        final double inverseVariance = precision.getValue(0) / time;
        final double logP = -angle * angle * inverseVariance / 2.0 + 0.5 * Math.log(angle * sin(angle) * inverseVariance);
//		System.err.println(start[0] + " " + start[1] + " -> " + stop[0] + " " + stop[1] + " => " + logP);
        return logP;

    }

    public static void main(String[] args) {

        double[] start = new double[]{90, 0};
        double[] stop = new double[]{-90, 0};

        double latitude1 = start[0];
        double longitude1 = start[1];
        double theta1 = (latitude1) * Math.PI / 180.0;
        if( longitude1 < 0 ) longitude1 += 360;
        double phi1 = longitude1 * Math.PI / 180;

        double latitude2 = stop[0];
        double longitude2 = stop[1];
        double theta2 = (latitude2) * Math.PI / 180.0;
        if( longitude2 < 0 ) longitude2 += 360;
        double phi2 = longitude2 * Math.PI / 180;

        double Deltalambda = phi2 - phi1;

        double f1 = Math.sin(theta1) * Math.sin(theta2);
        double f2 = cos(theta1) * cos(theta2) * cos(Deltalambda);

        double angle = Math.acos(f1 + f2);
        System.err.println("angle = " + angle * 180 / Math.PI);

    }
}
