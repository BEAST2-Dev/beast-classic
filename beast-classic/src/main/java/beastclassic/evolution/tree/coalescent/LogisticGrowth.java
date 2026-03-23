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

import beast.base.core.BEASTInterface;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.type.RealScalar;
import beast.base.evolution.tree.coalescent.PopulationFunction;

@Description("Demographic function according to logistic growth")
public class LogisticGrowth extends PopulationFunction.Abstract  {
    final public Input<RealScalar<? extends PositiveReal>> N0ParameterInput = new Input<>("popSize",
            "present-day population size (defaults to 1.0). ");
    final public Input<RealScalar<? extends PositiveReal>> growthRateParameterInput = new Input<>("growthRate",
            "growth rate of the logistic growth", Validate.REQUIRED);
    final public Input<RealScalar<? extends PositiveReal>> shapeParameterInput = new Input<>("shape",
            "shape parameter of the logistic growth", Validate.REQUIRED);

    public void initAndValidate() {
        // domain constraints handled by spec type PositiveReal
    }

    private double getN0() {
        return N0ParameterInput.get() != null ? N0ParameterInput.get().get() : 1.0;
    }
//    double alpha = 0.5;
//    boolean usingGrowthRate = true;

    double lowLimit = 0; // 1e-6;


	@Override
	public List<String> getParameterIds() {
		List<String> ids = new ArrayList<>();
		if (N0ParameterInput.get() instanceof BEASTInterface bi) ids.add(bi.getID());
		if (growthRateParameterInput.get() instanceof BEASTInterface bi) ids.add(bi.getID());
		if (shapeParameterInput.get() instanceof BEASTInterface bi) ids.add(bi.getID());
        return ids;
	}


	@Override
	public double getPopSize(double t) {
        double nZero = getN0();
        double r = growthRateParameterInput.get().get();
        double c = shapeParameterInput.get().get();

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
        double nZero = getN0();
        double r = growthRateParameterInput.get().get();
        double c = shapeParameterInput.get().get();

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
