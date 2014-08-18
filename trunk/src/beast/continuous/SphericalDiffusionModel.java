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

@Description("Diffusion model that assumes a normal diffusion process on a sphere")
public class SphericalDiffusionModel extends ContinuousSubstitutionModel {
	public Input<RealParameter> precisionInput = new Input<RealParameter>("precision", "precision of diffusion process", Validate.REQUIRED);

    RealParameter precision;

    @Override
    public void initAndValidate() throws Exception {
    	precision = precisionInput.get();
    	super.initAndValidate();
    }

	@Override
    public double getLogLikelihood(double[] start, double[] stop, double time) {
		
		// assumes start = {latitude, longitude}
		// assumes stop = {latitude, longitude}
		// and -90 < latitude < 90, -180 < longitude < 180
		
		double latitude1 = start[0];
		double longitude1 = start[1];
		double theta1 = (latitude1)*Math.PI/180.0;
		if (longitude1 < 0) longitude1 += 360;
		double phi1 = longitude1 * Math.PI/180;

		double latitude2 = stop[0];
		double longitude2 = stop[1];
		double theta2 = (latitude2)*Math.PI/180.0;
		if (longitude2 < 0) longitude2 += 360;
		double phi2 = longitude2 * Math.PI/180;
		
		double Deltalambda = phi2 - phi1;
		
		double angle = Math.acos(Math.sin(theta1)*Math.sin(theta2)+Math.cos(theta1) * Math.cos(theta2) * Math.cos(Deltalambda)); 

        double inverseVariance = precision.getValue(0) / time;
        double logP = -angle*angle * inverseVariance /2.0 + 0.5 * Math.log(angle * Math.sin(angle) * inverseVariance);
//		System.err.println(start[0] + " " + start[1] + " -> " + stop[0] + " " + stop[1] + " => " + logP);
        return logP;
        
    }

	public static void main(String[] args) {
		
		double [] start = new double[]{90, 0};
		double [] stop= new double[]{-90, 0};
		
		double latitude1 = start[0];
		double longitude1 = start[1];
		double theta1 = (latitude1)*Math.PI/180.0;
		if (longitude1 < 0) longitude1 += 360;
		double phi1 = longitude1 * Math.PI/180;

		double latitude2 = stop[0];
		double longitude2 = stop[1];
		double theta2 = (latitude2)*Math.PI/180.0;
		if (longitude2 < 0) longitude2 += 360;
		double phi2 = longitude2 * Math.PI/180;
		
		double Deltalambda = phi2 - phi1;
		
		double f1 = Math.sin(theta1)*Math.sin(theta2);
		double f2 = Math.cos(theta1) * Math.cos(theta2) * Math.cos(Deltalambda);
		
		double angle = Math.acos(f1+ f2);
		System.err.println("angle = " + angle * 180 / Math.PI);
		
	}

}
