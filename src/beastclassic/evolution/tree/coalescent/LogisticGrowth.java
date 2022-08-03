/*
 * LogisticGrowth.java
 *
 * Copyright (c) 2002-2015 Alexei Drummond, Andrew Rambaut and Marc Suchard
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
 *  BEAST is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with BEAST; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin St, Fifth Floor,
 * Boston, MA  02110-1301  USA
 */

package beastclassic.evolution.tree.coalescent;


import java.util.ArrayList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.RealParameter;
import beast.base.evolution.tree.coalescent.PopulationFunction;

@Description("Demographic function according to logistic growth")
public class LogisticGrowth extends PopulationFunction.Abstract  {
    final public Input<RealParameter> N0ParameterInput = new Input<>("popSize",
            "present-day population size (defaults to 1.0). ");
    final public Input<RealParameter> growthRateParameterInput = new Input<>("growthRate",
            "growth rate of the logistic growth", Validate.REQUIRED);
    final public Input<RealParameter> shapeParameterInput = new Input<>("shape",
            "shape parameter of the logistic growth", Validate.REQUIRED);
	

    public void initAndValidate() {
        this.N0Parameter = N0ParameterInput.get();
        if (N0Parameter == null) {
        	N0Parameter = new RealParameter("1.0");
        }
        N0Parameter.setUpper(Double.POSITIVE_INFINITY);
        N0Parameter.setLower(0.0);

        this.growthRateParameter = growthRateParameterInput.get();;
        growthRateParameter.setUpper(Double.POSITIVE_INFINITY);
        growthRateParameter.setLower(0.0);

        this.shapeParameter = shapeParameterInput.get();
        shapeParameter.setUpper(Double.POSITIVE_INFINITY);
        shapeParameter.setLower(0.0);

//        this.alpha = 0.5;
//        this.usingGrowthRate = true;

    }

    RealParameter N0Parameter = null;
    RealParameter growthRateParameter = null;
    RealParameter shapeParameter = null;
//    double alpha = 0.5;
//    boolean usingGrowthRate = true;

    double lowLimit = 0; // 1e-6;


	@Override
	public List<String> getParameterIds() {
		List<String> ids = new ArrayList<>();
		ids.add(N0ParameterInput.get().getID());
		ids.add(growthRateParameterInput.get().getID());
		ids.add(shapeParameterInput.get().getID());
        return ids;
	}


	@Override
	public double getPopSize(double t) {
        double nZero = N0Parameter.getValue();
        double r = growthRateParameter.getValue();
        double c = shapeParameter.getValue();

//		return nZero * (1 + c) / (1 + (c * Math.exp(r*t)));
//		AER rearranging this to use exp(-rt) may help
// 		with some overflow situations...

        double expOfMRT = Math.exp(-r * t);
        return lowLimit + (nZero * (1 + c) * expOfMRT) / (c + expOfMRT);
	}

	
    /**
     * Returns value of demographic intensity function at time t
     * (= integral 1/N(x) dx from 0 to t).
     */
    @Override
    public double getIntensity(double t) {
        double nZero = N0Parameter.getValue();
        double r = growthRateParameter.getValue();
        double c = shapeParameter.getValue();

        double ert = Math.exp(r * t);
        if( lowLimit == 0 ) {
       // double emrt = Math.exp(-r * t);
          return (c * (ert - 1)/r + t)  / ((1+c) * nZero);
        }
        double z = lowLimit;
        return (r*t*z + (1 + c)*nZero*Math.log(nZero + c*nZero + z + c*ert*z))/(r*z*(nZero + c*nZero + z));
    }

    /**
     * Returns value of demographic intensity function at time t
     * (= integral 1/N(x) dx from 0 to t).
     */
    @Override
    public double getInverseIntensity(double x) {

        throw new RuntimeException("Not implemented!");
    }

}
