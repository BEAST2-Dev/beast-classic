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


import javax.swing.JFrame;
import javax.swing.JPanel;

import org.apache.commons.math.ConvergenceException;
import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.analysis.UnivariateRealFunction;
import org.apache.commons.math.analysis.integration.TrapezoidIntegrator;
import org.apache.commons.math.analysis.integration.UnivariateRealIntegrator;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.ContinuousSubstitutionModel;
import beast.util.Randomizer;
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
    	if (fast) {
    		return getLogLikelihood2(start, stop, time);
    	}
           
            if (time <= 1e-20) {
                    return -1e100;
            }
            if (start[0] == stop[0] && start[1] == stop[1]) {
                    return -1e100;
            }
           
            // assumes start = {latitude, longitude}
            // assumes stop = {latitude, longitude}
            // and -90 < latitude < 90, -180 < longitude < 180
           
            double latitude1 = start[0];
            double longitude1 = start[1];
            double theta1 = (latitude1) * Math.PI/180.0;
            if (longitude1 < 0) longitude1 += 360;
            double phi1 = longitude1 * Math.PI/180;

            double latitude2 = stop[0];
            double longitude2 = stop[1];
            double theta2 = (latitude2) * Math.PI/180.0;
            if (longitude2 < 0) longitude2 += 360;
            double phi2 = longitude2 * Math.PI/180;
           
            double Deltalambda = phi2 - phi1;
            
            // See http://en.wikipedia.org/wiki/Great-circle_distance#Formulas
            double angle = Math.acos(Math.sin(theta1) * Math.sin(theta2) + Math.cos(theta1) * Math.cos(theta2) * Math.cos(Deltalambda));

            double inverseVariance = precision.getValue(0) / time;
            // See Equation (8) from http://arxiv.org/pdf/1303.1278v1.pdf
            double logP = 0.5 * Math.log(angle * Math.sin(angle)) + 0.5 * Math.log(inverseVariance) -0.5 * angle*angle * inverseVariance;
            //double logP = - 0.5 * angle*angle * inverseVariance;
//          System.err.println(start[0] + " " + start[1] + " -> " + stop[0] + " " + stop[1] + " => " + logP);
            return logP;
    }

    
    //@Override
    public double getLogLikelihood2(double[] start, double[] stop, double time) {

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

	final static int NR_OF_STEPS = 1000;
	
	public double[] sample(double[] start, double time, double precision) throws ConvergenceException, FunctionEvaluationException, IllegalArgumentException {
		
		// first, sample an angle from the spherical diffusion density
		final double inverseVariance = precision / time;

		UnivariateRealFunction function = new UnivariateRealFunction() {
			@Override
			public double value(double x) throws FunctionEvaluationException {
				double logR = -x*x * inverseVariance /2.0 + 0.5 * Math.log(x * Math.sin(x) * inverseVariance);

				return Math.exp(logR);
			}
		};
		
        UnivariateRealIntegrator integrator = new TrapezoidIntegrator();
        integrator.setAbsoluteAccuracy(1.0e-10);
        integrator.setRelativeAccuracy(1.0e-14);
        integrator.setMinimalIterationCount(2);
        integrator.setMaximalIterationCount(25);
		
		double [] cumulative = new double[NR_OF_STEPS];
		for (int i = 1; i < NR_OF_STEPS; i++) {
			cumulative[i] = cumulative[i - 1] + integrator.integrate(function, (i - 1) * Math.PI / NR_OF_STEPS, i * Math.PI / NR_OF_STEPS);
		}
		
		// normalise 
		double sum = cumulative[NR_OF_STEPS - 1];
		for (int i = 0; i < NR_OF_STEPS; i++) {
			cumulative[i] /= sum;
		}

		int i = Randomizer.randomChoice(cumulative);
		double angle = i* Math.PI / NR_OF_STEPS;
		
		
		// now we have an angle, use this to rotate the point [0,0] over
		// this angle in a random direction angle2
		double angle2 = Randomizer.nextDouble() * Math.PI * 2.0;

	    double [] xC = new double[] {Math.cos(angle), Math.sin(angle)*Math.cos(angle2), Math.sin(angle)*Math.sin(angle2)};

	    // convert back to latitude, longitude relative to (lat=0, long=0)
		double [] xL = cartesian2Sperical(xC);
	    //double [] sC = spherical2Cartesian(start[0], start[1]);
		double [] position = reverseMap(xL[0], xL[1], start[0], start[1]);
		return position;
	}

	/** Convert spherical coordinates (latitude,longitude) in degrees on unit sphere 
	 * to Cartesian (x,y,z) coordinates **/
	public static double [] spherical2Cartesian(double fLat, double fLong) {
		double fPhi = (fLong * Math.PI / 180.0);
		double fTheta = (90 - fLat) * Math.PI / 180.0;
	    //{x}=\rho \, \sin\theta \, \cos\phi  
	    //{y}=\rho \, \sin\theta \, \sin\phi  
	    //{z}=\rho \, \cos\theta 
		double [] fNorm = new double[3];
		fNorm[0] = Math.sin(fTheta) * Math.cos(fPhi);
		fNorm[1] = Math.sin(fTheta) * Math.sin(fPhi);
		fNorm[2] = Math.cos(fTheta);
		return fNorm;
	} // spherical2Cartesian
	
	/** inverse of spherical2Cartesian **/
	public static double [] cartesian2Sperical(double[] f3dRotated2) {
		return 	new double[]{
				Math.acos(-f3dRotated2[2]) * 180/Math.PI - 90,
				Math.atan2(f3dRotated2[1], f3dRotated2[0]) * 180.0/Math.PI
		};
	}

	public static double [] reverseMap(double fLat, double fLong, double fLatT, double fLongT) {
		// from spherical to Cartesian coordinates
		double [] f3DPoint = spherical2Cartesian(fLong, fLat);
		// rotate, first latitude, then longitude
		double [] f3DRotated = new double[3];
		double fC = Math.cos(fLongT * Math.PI / 180);
		double fS = Math.sin(fLongT * Math.PI / 180);
		double [] f3DRotated2 = new double[3];
		double fC2 = Math.cos(-fLatT * Math.PI / 180);
		double fS2 = Math.sin(-fLatT * Math.PI / 180);

		// rotate over latitude
		f3DRotated[0] = f3DPoint[0] * fC2 + f3DPoint[2] * fS2;
		f3DRotated[1] = f3DPoint[1];
		f3DRotated[2] = -f3DPoint[0] * fS2 + f3DPoint[2] * fC2;

		// rotate over longitude
		f3DRotated2[0] = f3DRotated[0] * fC - f3DRotated[1] * fS; 
		f3DRotated2[1] = f3DRotated[0] * fS + f3DRotated[1] * fC; 
		f3DRotated2[2] = f3DRotated[2]; 

		double [] point = cartesian2Sperical(f3DRotated2); 
		return point;
	} // map
	
    public static void main(String[] args) throws Exception {
    	double [] start = new double[]{0,0};
    	double time = 1;
    	double precision = 10;
    	
    	final int NR_OF_POINTS = 1024;
    	final double[][] points = new double[NR_OF_POINTS][];
    	SphericalDiffusionModel s = new SphericalDiffusionModel();
    	
      for (int i = 0; i < NR_OF_POINTS; i++) {
//    	double angle = 15.0;
//    	
//    	// now we have an angle, use this to rotate the point [0,0] over
//		// this angle in a random direction angle2
//		double angle2 = Randomizer.nextDouble() * 360;
//		double _angle = angle * Math.PI / 180;
//		double _angle2 = angle2 * Math.PI / 180;
//	    double [] xC = new double[] {Math.cos(_angle), Math.sin(_angle)*Math.cos(_angle2), Math.sin(_angle)*Math.sin(_angle2)};
//
//	    // convert back to latitude, longitude
//		double [] xL = cartesian2Sperical(xC);
//	    //double [] sC = spherical2Cartesian(start[0], start[1]);
//		double [] target = reverseMap(xL[0], xL[1], start[0], start[1]);
//		
//		//System.err.println(target[0] + "," + target[1]+ ",0");
    	
    	
		points[i] = s.sample(start, time, precision);
      }

      
	  	JFrame frame = new JFrame();
	  	JPanel panel= new JPanel() {
	  		protected void paintComponent(java.awt.Graphics g) {
	  			double scaleX = getWidth() / 360.0;
	  			double scaleY = getHeight() / 180.0;
	  			for (int i = 0; i < NR_OF_POINTS; i++) {
	  				g.drawRect((int) ((points[i][1]+180) * scaleX), (int) ((points[i][0]+90) * scaleY), 3, 3);
	  			}
	  		};
	  	};
	  	frame.add(panel);
	  	frame.setSize(1024, 768);
	  	frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }
    
}
