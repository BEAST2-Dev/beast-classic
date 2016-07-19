/*
 * GMRFMultilocusSkyrideBlockUpdateOperator.java
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

package beast.evolution.operators;

import no.uib.cipr.matrix.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math.MathException;

import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.StateNode;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.coalescent.GMRFMultilocusSkyrideLikelihood;
import beast.util.Randomizer;

/* A Metropolis-Hastings operator to update the log population sizes and precision parameter jointly under a Gaussian Markov random field prior
 *
 * @author Erik Bloomquist
 * @author Marc Suchard
 * @author Mandev Gill
 * @version $Id: GMRFMultilocusSkylineBlockUpdateOperator.java,v 1.5 2007/03/20 11:26:49 msuchard Exp $
 */
public class GMRFMultilocusSkyrideBlockUpdateOperator extends Operator {
	public Input<GMRFMultilocusSkyrideLikelihood> GMRFSkyrideLikelihoodInput = new Input<GMRFMultilocusSkyrideLikelihood>("likelihood","GMRF Multilocus Skyride Likelihood", Validate.REQUIRED);
	public Input<Double> scaleFactorInput = new Input<Double>("scaleFactor","scale factor: larger means more bold proposals", Validate.REQUIRED);
	public Input<Integer> maxIterationsInput = new Input<Integer>("maxIterations","max number of iterations for proposal...", 200);
	public Input<Double> stopValueInput = new Input<Double>("stopValue","",0.01);
    final public Input<Boolean> optimiseInput = new Input<>("optimise", "flag to indicate that the scale factor is automatically changed in order to achieve a good acceptance rate (default true)", true);

    private double scaleFactor;
    private double lambdaScaleFactor;
    private int fieldLength;

    private int maxIterations;
    private double stopValue;

    private RealParameter popSizeParameter;
    private RealParameter precisionParameter;
    private RealParameter lambdaParameter;

    GMRFMultilocusSkyrideLikelihood gmrfField;

    private double[] zeros;

    public GMRFMultilocusSkyrideBlockUpdateOperator() {
	}

	@Override
    public void initAndValidate() {
        gmrfField = GMRFSkyrideLikelihoodInput.get();
        popSizeParameter = gmrfField.getPopSizeParameter();
        precisionParameter = gmrfField.getPrecisionParameter();
        lambdaParameter = gmrfField.getLambdaParameter();

        this.scaleFactor = scaleFactorInput.get();
        lambdaScaleFactor = 0.0;
        fieldLength = popSizeParameter.getDimension();

        this.maxIterations = maxIterationsInput.get();
        this.stopValue = stopValueInput.get();

        zeros = new double[fieldLength];
    }

    private double getNewLambda(double currentValue, double lambdaScale) {
        double a = Randomizer.nextDouble() * lambdaScale - lambdaScale / 2;
        double b = currentValue + a;
        if (b > 1)
            b = 2 - b;
        if (b < 0)
            b = -b;

        return b;
    }

    private double getNewPrecision(double currentValue, double scaleFactor) {
        double length = scaleFactor - 1 / scaleFactor;
        double returnValue;


        if (scaleFactor == 1)
            return currentValue;
        if (Randomizer.nextDouble() < length / (length + 2 * Math.log(scaleFactor))) {
            returnValue = (1 / scaleFactor + length * Randomizer.nextDouble()) * currentValue;
        } else {
            returnValue = Math.pow(scaleFactor, 2.0 * Randomizer.nextDouble() - 1) * currentValue;
        }

        return returnValue;
    }

    public DenseVector getMultiNormalMean(DenseVector CanonVector, BandCholesky Cholesky) {

        DenseVector tempValue = new DenseVector(zeros);
        DenseVector Mean = new DenseVector(zeros);

        UpperTriangBandMatrix CholeskyUpper = Cholesky.getU();

        // Assume Cholesky factorization of the precision matrix Q = LL^T

        // 1. Solve L\omega = b

        CholeskyUpper.transSolve(CanonVector, tempValue);

        // 2. Solve L^T \mu = \omega

        CholeskyUpper.solve(tempValue, Mean);

        return Mean;
    }

    public DenseVector getMultiNormal(DenseVector StandNorm, DenseVector Mean, BandCholesky Cholesky) {

        DenseVector returnValue = new DenseVector(zeros);

        UpperTriangBandMatrix CholeskyUpper = Cholesky.getU();

        // 3. Solve L^T v = z

        CholeskyUpper.solve(StandNorm, returnValue);

        // 4. Return x = \mu + v

        returnValue.add(Mean);

        return returnValue;
    }


    public static DenseVector getMultiNormal(DenseVector Mean, UpperSPDDenseMatrix Variance) {
        int length = Mean.size();
        DenseVector tempValue = new DenseVector(length);
        DenseVector returnValue = new DenseVector(length);
        UpperSPDDenseMatrix ab = Variance.copy();

        for (int i = 0; i < returnValue.size(); i++)
            tempValue.set(i, Randomizer.nextGaussian());

        DenseCholesky chol = new DenseCholesky(length, true);
        chol.factor(ab);

        UpperTriangDenseMatrix x = chol.getU();

        x.transMult(tempValue, returnValue);
        returnValue.add(Mean);
        return returnValue;
    }


    public static double logGeneralizedDeterminant(UpperTriangBandMatrix Matrix) {
        double returnValue = 0;

        for (int i = 0; i < Matrix.numColumns(); i++) {
            if (Matrix.get(i, i) > 0.0000001) {
                returnValue += Math.log(Matrix.get(i, i));
            }
        }

        return returnValue;
    }
       
    public DenseVector newtonRaphson(double[] data1, double[] data2, DenseVector currentGamma, SymmTridiagMatrix proposedQ) throws MathException {
        return newNewtonRaphson(data1, data2, currentGamma, proposedQ, maxIterations, stopValue);
    }

    public static DenseVector newNewtonRaphson(double[] data1, double[] data2, DenseVector currentGamma, SymmTridiagMatrix proposedQ,
                                               int maxIterations, double stopValue) throws MathException {

        DenseVector iterateGamma = currentGamma.copy();
        DenseVector tempValue = currentGamma.copy();

        int numberIterations = 0;


        while (gradient(data1, data2, iterateGamma, proposedQ).norm(Vector.Norm.Two) > stopValue) {
           try {
                jacobian(data2, iterateGamma, proposedQ).solve(gradient(data1, data2, iterateGamma, proposedQ), tempValue);
           } catch (no.uib.cipr.matrix.MatrixNotSPDException e) {
                Logger.getLogger("dr.evomodel.coalescent.operators.GMRFMultilocusSkyrideBlockUpdateOperator").fine("Newton-Raphson F");
                throw new MathException("");
            } catch (no.uib.cipr.matrix.MatrixSingularException e) {
                Logger.getLogger("dr.evomodel.coalescent.operators.GMRFMultilocusSkyrideBlockUpdateOperator").fine("Newton-Raphson F");
                throw new MathException("");
            }     

            iterateGamma.add(tempValue);
            numberIterations++;

            if (numberIterations > maxIterations) {
                Logger.getLogger("dr.evomodel.coalescent.operators.GMRFMultilocusSkyrideBlockUpdateOperator").fine("Newton-Raphson F");
                throw new MathException("Newton Raphson algorithm did not converge within " + maxIterations + " step to a norm less than " + stopValue + "\n" +
                        "Try starting BEAST with a more accurate initial tree.");
            }
        }

        Logger.getLogger("dr.evomodel.coalescent.operators.GMRFMultilocusSkyrideBlockUpdateOperator").fine("Newton-Raphson S");
        return iterateGamma;

    }

    private static DenseVector gradient(double[] data1, double[] data2, DenseVector value, SymmTridiagMatrix Q) {

        DenseVector returnValue = new DenseVector(value.size());
        Q.mult(value, returnValue);
        for (int i = 0; i < value.size(); i++) {
            returnValue.set(i, -returnValue.get(i) - data1[i] + data2[i] * Math.exp(-value.get(i)));
        }
        return returnValue;
    }


    private static SPDTridiagMatrix jacobian(double[] data2, DenseVector value, SymmTridiagMatrix Q) {
        SPDTridiagMatrix jacobian = new SPDTridiagMatrix(Q, true);
        for (int i = 0, n = value.size(); i < n; i++) {
            jacobian.set(i, i, jacobian.get(i, i) + Math.exp(-value.get(i)) * data2[i]);
        }
        return jacobian;
    }

    @Override
    public double proposal() {

        double currentPrecision = precisionParameter.getValue(0);
        double proposedPrecision = this.getNewPrecision(currentPrecision, scaleFactor);

        double currentLambda = this.lambdaParameter.getValue(0);
        double proposedLambda = this.getNewLambda(currentLambda, lambdaScaleFactor);

        precisionParameter.setValue(0, proposedPrecision);
        lambdaParameter.setValue(0, proposedLambda);

        Double [] Values = gmrfField.getPopSizeParameter().getValues();
        double [] values = new double[Values.length];
        for (int i = 0; i < values.length; i++) {
        	values[i] = Values[i];
        }
        DenseVector currentGamma = new DenseVector(values);
        DenseVector proposedGamma;

        SymmTridiagMatrix currentQ = gmrfField.getStoredScaledWeightMatrix(currentPrecision, currentLambda);
        SymmTridiagMatrix proposedQ = gmrfField.getScaledWeightMatrix(proposedPrecision, proposedLambda);

        double[] wNative = gmrfField.getSufficientStatistics();
        double[] numCoalEv = gmrfField.getNumCoalEvents();

        UpperSPDBandMatrix forwardQW = new UpperSPDBandMatrix(proposedQ, 1);
        UpperSPDBandMatrix backwardQW = new UpperSPDBandMatrix(currentQ, 1);

        BandCholesky forwardCholesky = new BandCholesky(wNative.length, 1, true);
        BandCholesky backwardCholesky = new BandCholesky(wNative.length, 1, true);

        DenseVector diagonal1 = new DenseVector(fieldLength);
        DenseVector diagonal2 = new DenseVector(fieldLength);
        DenseVector diagonal3 = new DenseVector(fieldLength);

        DenseVector modeForward;
		try {
			modeForward = newtonRaphson(numCoalEv, wNative, currentGamma, proposedQ.copy());
		} catch (MathException e) {
			return Double.NEGATIVE_INFINITY;
		}
       
        for (int i = 0; i < fieldLength; i++) {
            diagonal1.set(i, wNative[i] * Math.exp(-modeForward.get(i)));
            diagonal2.set(i, modeForward.get(i) + 1);

            forwardQW.set(i, i, diagonal1.get(i) + forwardQW.get(i, i));
            //diagonal1.set(i, diagonal1.get(i) * diagonal2.get(i) - 1);
            diagonal1.set(i, diagonal1.get(i) * diagonal2.get(i) - numCoalEv[i]);
        }

        forwardCholesky.factor(forwardQW.copy());

        DenseVector forwardMean = getMultiNormalMean(diagonal1, forwardCholesky);

        DenseVector stand_norm = new DenseVector(zeros);

        for (int i = 0; i < zeros.length; i++)
            stand_norm.set(i, Randomizer.nextGaussian());

        proposedGamma = getMultiNormal(stand_norm, forwardMean, forwardCholesky);

        /*
        double hRatio = 0;
        for (int i = 0; i < fieldLength; i++) {
            diagonal1.set(i, proposedGamma.get(i) - forwardMean.get(i));
        }
        diagonal3.zero();
        forwardQW.mult(diagonal1, diagonal3);

        hRatio -= logGeneralizedDeterminant(forwardCholesky.getU() ) - 0.5 * diagonal1.dot(diagonal3);
        */

        for (int i = 0; i < fieldLength; i++)
            popSizeParameter.setValue(i, proposedGamma.get(i));

        //((Parameter.Abstract) popSizeParameter).fireParameterChangedEvent();


        double hRatio = 0;

        diagonal1.zero();
        diagonal2.zero();
        diagonal3.zero();

        DenseVector modeBackward;
		try {
			modeBackward = newtonRaphson(numCoalEv, wNative, proposedGamma, currentQ.copy());
		} catch (MathException e) {
			return Double.NEGATIVE_INFINITY;
		}

        for (int i = 0; i < fieldLength; i++) {
            diagonal1.set(i, wNative[i] * Math.exp(-modeBackward.get(i)));
            diagonal2.set(i, modeBackward.get(i) + 1);

            backwardQW.set(i, i, diagonal1.get(i) + backwardQW.get(i, i));
            //diagonal1.set(i, diagonal1.get(i) * diagonal2.get(i) - 1);
            diagonal1.set(i, diagonal1.get(i) * diagonal2.get(i) - numCoalEv[i]);
        }

        backwardCholesky.factor(backwardQW.copy());

        DenseVector backwardMean = getMultiNormalMean(diagonal1, backwardCholesky);

        for (int i = 0; i < fieldLength; i++) {
            diagonal1.set(i, currentGamma.get(i) - backwardMean.get(i));
        }

        backwardQW.mult(diagonal1, diagonal3);

        hRatio += logGeneralizedDeterminant(backwardCholesky.getU()) - 0.5 * diagonal1.dot(diagonal3);
        hRatio -= logGeneralizedDeterminant(forwardCholesky.getU() ) - 0.5 * stand_norm.dot(stand_norm);

       return hRatio;
    }

    //MCMCOperator INTERFACE

//    public final String getOperatorName() {
//        return GMRFSkyrideBlockUpdateOperatorParser.BLOCK_UPDATE_OPERATOR;
//    }

    public double getCoercableParameter() {
//        return Math.log(scaleFactor);
        return Math.sqrt(scaleFactor - 1);
    }

    public void setCoercableParameter(double value) {
//        scaleFactor = Math.exp(value);
        scaleFactor = 1 + value * value;
    }

    public double getRawParameter() {
        return scaleFactor;
    }

    public double getScaleFactor() {
        return scaleFactor;
    }

    public double getTargetAcceptanceProbability() {
        return 0.234;
    }

    public double getMinimumAcceptanceLevel() {
        return 0.1;
    }

    public double getMaximumAcceptanceLevel() {
        return 0.4;
    }

    public double getMinimumGoodAcceptanceLevel() {
        return 0.20;
    }

    public double getMaximumGoodAcceptanceLevel() {
        return 0.30;
    }

    public final String getPerformanceSuggestion() {

        double prob = m_nNrAccepted / (m_nNrAccepted + m_nNrRejected + 0.0);
        double targetProb = getTargetAcceptanceProbability();

        double ratio = prob / targetProb;
        if (ratio > 2.0) ratio = 2.0;
        if (ratio < 0.5) ratio = 0.5;
        double sf = scaleFactor * ratio;        
        
        final DecimalFormat formatter = new DecimalFormat("#.###");
        if (prob < getMinimumGoodAcceptanceLevel()) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else if (prob > getMaximumGoodAcceptanceLevel()) {
            return "Try setting scaleFactor to about " + formatter.format(sf);
        } else return "";
    }


    @Override
    public List<StateNode> listStateNodes() {
    	List<StateNode> stateNodes = new ArrayList<>();
    	stateNodes.add(popSizeParameter);
    	stateNodes.add(precisionParameter);
    	stateNodes.add(lambdaParameter);
    	return stateNodes;
    }
}

