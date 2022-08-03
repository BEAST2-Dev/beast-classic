/*
 * CTMCScalePrior.java
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

package beastclassic.math.distributions;


import java.util.List;
import java.util.Random;

import beast.base.inference.Distribution;
import beast.base.core.Input;
import beast.base.inference.State;
import beast.base.inference.parameter.RealParameter;
import beastclassic.dr.GammaFunction;
import beast.base.evolution.substitutionmodel.SubstitutionModel;
import beast.base.evolution.tree.Tree;
import beast.base.evolution.tree.TreeUtils;

/**
 * @author Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 * @author Marc A. Suchard
 *         <p/>
 *         Date: Aug 22, 2008
 *         Time: 3:26:57 PM
 */
public class CTMCScalePrior extends Distribution {
    public Input<RealParameter> ctmcScaleInput1 = new Input<>("ctmcScale", "description here");
    public Input<Tree> treeInput2 = new Input<>("tree", "description here");
    public Input<Boolean> reciprocalInput3 = new Input<>("reciprocal", "description here", false);
    public Input<SubstitutionModel.Base> substModelInput4 = new Input<>("substModel", "description here");
    public Input<Boolean> trialInput5 = new Input<Boolean>("trial", "description here", false);

    private RealParameter ctmcScale;
    private Tree treeModel;
    private double treeLength;
    private boolean treeLengthKnown;

    private boolean reciprocal;
    private SubstitutionModel.Base substitutionModel;
    private boolean trial;

    private static final double logGammaOneHalf = GammaFunction.lnGamma(0.5);

    @Override
    public void initAndValidate() {
    	super.initAndValidate();
        this.ctmcScale = ctmcScaleInput1.get();
        this.treeModel = treeInput2.get();
        treeLengthKnown = false;
        this.reciprocal = reciprocalInput3.get();
        this.substitutionModel = substModelInput4.get();
        this.trial = trialInput5.get();
    }

    private void updateTreeLength() {
        treeLength = TreeUtils.getTreeLength(treeModel, treeModel.getRoot());
    }

    @Override
    public void store() {
    }

    @Override
    public void restore() {
        treeLengthKnown = false;
    }

    @Override
    protected boolean requiresRecalculation() {
        treeLengthKnown = false;
    	return super.requiresRecalculation();
    }

    private double calculateTrialLikelihood() {
        double totalTreeTime = TreeUtils.getTreeLength(treeModel, treeModel.getRoot());

        double[] eigenValues = substitutionModel.getEigenDecomposition(null).getEigenValues();
        // Find second largest
        double lambda2 = Double.NEGATIVE_INFINITY;
        for (double l : eigenValues) {
            if (l > lambda2 && l < 0.0) {
                lambda2 = l;
            }
        }
        lambda2 = -lambda2;

        double logNormalization = 0.5 * Math.log(lambda2) - logGammaOneHalf;

        double logLike = 0;
        for (int i = 0; i < ctmcScale.getDimension(); ++i) {
            double ab = ctmcScale.getValue(i) * totalTreeTime;
            logLike += logNormalization - 0.5 * Math.log(ab) - ab * lambda2;
        }
        return logLike;
    }

    @Override
    public double calculateLogP() {
    	logP = getLogLikelihood();
    	return logP;
    }

    public double getLogLikelihood() {

//        if (!treeLengthKnown) {
//            updateTreeLength();
//            treeLengthKnown = true;
//        }
//        double totalTreeTime = treeLength;

        if (trial) return calculateTrialLikelihood();

        double totalTreeTime = TreeUtils.getTreeLength(treeModel, treeModel.getRoot());
        if (reciprocal) {
            totalTreeTime = 1.0 / totalTreeTime;
        }
        if (substitutionModel != null) {
            double[] eigenValues = substitutionModel.getEigenDecomposition(null).getEigenValues();
            // Find second largest
            double lambda2 = Double.NEGATIVE_INFINITY;
            for (double l : eigenValues) {
                if (l > lambda2 && l < 0.0) {
                    lambda2 = l;
                }
            }
            totalTreeTime *= -lambda2; // TODO Should this be /=?
        }
        double logNormalization = 0.5 * Math.log(totalTreeTime) - logGammaOneHalf;
        double logLike = 0;
        for (int i = 0; i < ctmcScale.getDimension(); ++i) {
            double ab = ctmcScale.getValue(i);
            logLike += logNormalization - 0.5 * Math.log(ab) - ab * totalTreeTime; // TODO Change to treeLength and confirm results
        }
        return logLike;
    }

    public void makeDirty() {
        treeLengthKnown = false;
    }

	@Override
	public List<String> getArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sample(State state, Random random) {
		// TODO Auto-generated method stub
		
	}
}
