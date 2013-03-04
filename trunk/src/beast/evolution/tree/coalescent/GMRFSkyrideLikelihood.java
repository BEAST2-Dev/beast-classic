/*
 * GMRFSkyrideLikelihood.java
 *
 * Copyright (c) 2002-2011 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package beast.evolution.tree.coalescent;


import no.uib.cipr.matrix.DenseVector;
import no.uib.cipr.matrix.NotConvergedException;
import no.uib.cipr.matrix.SymmTridiagEVD;
import no.uib.cipr.matrix.SymmTridiagMatrix;

import java.util.ArrayList;
import java.util.List;

import beast.core.Citation;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;

/**
 * A likelihood function for a Gaussian Markov random field on a log population size trajectory.
 *
 * @author Jen Tom
 * @author Erik Bloomquist
 * @author Vladimir Minin
 * @author Marc Suchard
 * @version $Id: GMRFSkylineLikelihood.java,v 1.3 2007/03/20 22:40:04 msuchard Exp $
 */
@Description("A likelihood function for a Gaussian Markov random field on a log population size trajectory")
@Citation("Minin, Bloomquist and Suchard (2008) Molecular Biology and Evolution, 25, 1459-1471")
public class GMRFSkyrideLikelihood extends Coalescent /*OldAbstractCoalescentLikelihood*/  {
    public Input<RealParameter> groupParameterInput = new Input<RealParameter>("groupSizes","",Validate.REQUIRED);
    public Input<RealParameter> popSizeParameterInput = new Input<RealParameter>("populationSizes","",Validate.REQUIRED);
    public Input<RealParameter> precParameterInput = new Input<RealParameter>("precisionParameter","",Validate.REQUIRED);
    public Input<RealParameter> lambdaInput = new Input<RealParameter>("lambda","");
    public Input<Boolean> timeAwareSmoothingInput = new Input<Boolean>("timeAwareSmoothing","use time Aware Smoothing", false);
    public Input<Boolean> rescaleByRootHeightInput = new Input<Boolean>("rescaleByRootHeightInput","rescale By Root Height", false);

	public GMRFSkyrideLikelihood() {
		popSize.setRule(Validate.OPTIONAL);
	}

	// PUBLIC STUFF

	public static final double LOG_TWO_TIMES_PI = 1.837877;
	public static final boolean TIME_AWARE_IS_ON_BY_DEFAULT = true;

	// PRIVATE STUFF
	protected RealParameter popSizeParameter;
	protected RealParameter groupSizeParameter;
	protected RealParameter precisionParameter;
	protected RealParameter lambdaParameter;
//	protected RealParameter betaParameter;
//	protected double[] gmrfWeights;
	protected int fieldLength;
	protected double[] coalescentIntervals;
	protected double[] storedCoalescentIntervals;
	protected double[] sufficientStatistics;
	protected double[] storedSufficientStatistics;

	protected boolean likelihoodKnown = false;
	protected double logLikelihood = 0;

    //changed from private to protected
    protected double logFieldLikelihood;
    protected double storedLogFieldLikelihood;
    
	protected SymmTridiagMatrix weightMatrix;
	protected SymmTridiagMatrix storedWeightMatrix;
//	protected RealParameter dMatrix;
	protected boolean timeAwareSmoothing = TIME_AWARE_IS_ON_BY_DEFAULT;
    protected boolean rescaleByRootHeight;
    
    Tree tree;
    //List<Tree> treesSet;
    boolean intervalsKnown = false;
    

//    private static List<Tree> wrapTree(Tree tree) {
//        List<Tree> treeList = new ArrayList<Tree>();
//        treeList.add(tree);
//        return treeList;
//    }
    
    
    @Override
    public void initAndValidate() throws Exception {
//    public GMRFSkyrideLikelihood(List<Tree> treeList, RealParameter popParameter, RealParameter groupParameter, RealParameter precParameter,
//    		RealParameter lambda, RealParameter beta, RealParameter dMatrix,
//	                             boolean timeAwareSmoothing, boolean rescaleByRootHeight) throws Exception {

//		super(GMRFSkyrideLikelihoodParser.SKYLINE_LIKELIHOOD);
    	tree = treeIntervals.get().m_tree.get();
    	
		this.popSizeParameter = popSizeParameterInput.get();
		this.groupSizeParameter = groupParameterInput.get();
		this.precisionParameter = precParameterInput.get();
		this.lambdaParameter = lambdaInput.get();
		if (lambdaParameter == null) {
			lambdaParameter = new RealParameter("1.0");
		}
//		this.betaParameter = betaInput.get();
//		this.dMatrix = dMatrixInput.get();
//		if (dMatrix == null) {
//			dMatrix = new RealParameter("1.0");
//		}
		this.timeAwareSmoothing = timeAwareSmoothingInput.get();
        this.rescaleByRootHeight = rescaleByRootHeightInput.get();

        List<Tree> treeList = new ArrayList<Tree>();
        treeList.add(tree);
        setTree(treeList);

        int correctFieldLength = getCorrectFieldLength();

        if (popSizeParameter.getDimension() <= 1) {
            // popSize dimension hasn't been set yet, set it here:
            popSizeParameter.setDimension(correctFieldLength);
        }

		fieldLength = popSizeParameter.getDimension();
		if (correctFieldLength != fieldLength) {
			throw new IllegalArgumentException("Population size parameter should have length " + correctFieldLength);
		}

        // Field length must be set by this point
        intervals = treeIntervals.get();
		wrapSetupIntervals();
		coalescentIntervals = new double[fieldLength];
		storedCoalescentIntervals = new double[fieldLength];
		sufficientStatistics = new double[fieldLength];
		storedSufficientStatistics = new double[fieldLength];

		setupGMRFWeights();

		//addStatistic(new DeltaStatistic());

		initializationReport();

		/* Force all entries in groupSizeParameter = 1 for compatibility with Tracer */
		if (groupSizeParameter != null) {
			groupSizeParameter.initByName("value","1.0");
//			for (int i = 0; i < groupSizeParameter.getDimension(); i++)
//				groupSizeParameter.setValue(i, 1.0);
		}
		
		super.initAndValidate();
	}

    @Override
    public double calculateLogP() throws Exception {
    	logP = getLogLikelihood();
    	return logP;
    }    
    
    protected int getCorrectFieldLength() {
        return tree.getLeafNodeCount() - 1;
    }

    protected void wrapSetupIntervals() {
    	intervals.calculateIntervals();
    }

    protected void setTree(List<Tree> treeList) {
        if (treeList.size() != 1) {
             throw new RuntimeException("GMRFSkyrideLikelihood only implemented for one tree");
        }
        this.tree = treeList.get(0);
        //this.treesSet = null;
//        if (tree instanceof TreeModel) {
//            addModel((TreeModel) tree);
//        }
    }

	public double[] getCopyOfCoalescentIntervals() {
		return coalescentIntervals.clone();
	}

	public double[] getCoalescentIntervals() {
		return coalescentIntervals;
	}

	public void initializationReport() {
		System.out.println("Creating a GMRF smoothed skyride model:");
		System.out.println("\tPopulation sizes: " + popSizeParameter.getDimension());
	}

//	public static void checkTree(Tree treeModel) {
//
//		// todo Should only be run if there exists a zero-length interval
//		for (int i = 0; i < treeModel.getInternalNodeCount(); i++) {
//			Node node = treeModel.getInternalNode(i);
//			if (node != treeModel.getRoot()) {
//				double parentHeight = node.getParent().getHeight();//treeModel.getNodeHeight(treeModel.getParent(node));
//				double childHeight0 = node.getLeft().getHeight();//treeModel.getNodeHeight(treeModel.getChild(node, 0));
//				double childHeight1 = node.getRight().getHeight();//treeModel.getNodeHeight(treeModel.getChild(node, 1));
//				double maxChild = childHeight0;
//				if (childHeight1 > maxChild)
//					maxChild = childHeight1;
//				double newHeight = maxChild + Randomizer.nextDouble() * (parentHeight - maxChild);
//				node.setHeight(newHeight);
//			}
//		}
//		//treeModel.pushTreeChangedEvent();
//
//	}

	// **************************************************************
	// Likelihood IMPLEMENTATION
	// **************************************************************
	public double getLogLikelihood() {
		if (!likelihoodKnown) {
			logLikelihood = calculateLogCoalescentLikelihood();
            logFieldLikelihood = calculateLogFieldLikelihood();
			likelihoodKnown = true;
		}
		return logLikelihood + logFieldLikelihood;
	}

    protected double peakLogCoalescentLikelihood() {
        return logLikelihood;
    }

    protected double peakLogFieldLikelihood() {
        return logFieldLikelihood;
    }

    public double[] getSufficientStatistics() {
	return sufficientStatistics;
    }

    public String toString() {
        return getID() + "(" + Double.toString(getLogLikelihood()) + ")";
    }

    protected void setupSufficientStatistics() {
	    int index = 0;

		double length = 0;
		double weight = 0;
		for (int i = 0; i < intervals.getIntervalCount(); i++) {
			length += intervals.getInterval(i);
			weight += intervals.getInterval(i) * intervals.getLineageCount(i) * (intervals.getLineageCount(i) - 1);
			if (intervals.getIntervalType(i) == IntervalType.COALESCENT) {
				coalescentIntervals[index] = length;
				sufficientStatistics[index] = weight / 2.0;
				index++;
				length = 0;
				weight = 0;
			}
		}
    }

    protected double getFieldScalar() {
        final double rootHeight;
        if (rescaleByRootHeight) {
            rootHeight = tree.getRoot().getHeight();
        } else {
            rootHeight = 1.0;
        }
        return rootHeight;
    }

	protected void setupGMRFWeights() {

        setupSufficientStatistics();

		//Set up the weight Matrix
		double[] offdiag = new double[fieldLength - 1];
		double[] diag = new double[fieldLength];

		//First set up the offdiagonal entries;

		if (!timeAwareSmoothing) {
			for (int i = 0; i < fieldLength - 1; i++) {
				offdiag[i] = -1.0;
			}


		} else {
			for (int i = 0; i < fieldLength - 1; i++) {
				offdiag[i] = -2.0 / (coalescentIntervals[i] + coalescentIntervals[i + 1]) * getFieldScalar();
			}
		}

		//Then set up the diagonal entries;
		for (int i = 1; i < fieldLength - 1; i++)
			diag[i] = -(offdiag[i] + offdiag[i - 1]);

		//Take care of the endpoints
		diag[0] = -offdiag[0];
		diag[fieldLength - 1] = -offdiag[fieldLength - 2];

		weightMatrix = new SymmTridiagMatrix(diag, offdiag);
	}


	public SymmTridiagMatrix getScaledWeightMatrix(double precision) {
		SymmTridiagMatrix a = weightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, a.get(i, i) * precision);
			a.set(i + 1, i, a.get(i + 1, i) * precision);
		}
		a.set(fieldLength - 1, fieldLength - 1, a.get(fieldLength - 1, fieldLength - 1) * precision);
		return a;
	}

	public SymmTridiagMatrix getStoredScaledWeightMatrix(double precision) {
		SymmTridiagMatrix a = storedWeightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, a.get(i, i) * precision);
			a.set(i + 1, i, a.get(i + 1, i) * precision);
		}
		a.set(fieldLength - 1, fieldLength - 1, a.get(fieldLength - 1, fieldLength - 1) * precision);
		return a;
	}

	public SymmTridiagMatrix getScaledWeightMatrix(double precision, double lambda) {
		if (lambda == 1)
			return getScaledWeightMatrix(precision);

		SymmTridiagMatrix a = weightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, precision * (1 - lambda + lambda * a.get(i, i)));
			a.set(i + 1, i, a.get(i + 1, i) * precision * lambda);
		}

		a.set(fieldLength - 1, fieldLength - 1, precision * (1 - lambda + lambda * a.get(fieldLength - 1, fieldLength - 1)));
		return a;
	}

	public double[] getCoalescentIntervalHeights() {
		double[] a = new double[coalescentIntervals.length];

		a[0] = coalescentIntervals[0];

		for (int i = 1; i < a.length; i++) {
			a[i] = a[i - 1] + coalescentIntervals[i];
		}
		return a;
	}

	public SymmTridiagMatrix getCopyWeightMatrix() {
		return weightMatrix.copy();
	}

	public SymmTridiagMatrix getStoredScaledWeightMatrix(double precision, double lambda) {
		if (lambda == 1)
			return getStoredScaledWeightMatrix(precision);

		SymmTridiagMatrix a = storedWeightMatrix.copy();
		for (int i = 0; i < a.numRows() - 1; i++) {
			a.set(i, i, precision * (1 - lambda + lambda * a.get(i, i)));
			a.set(i + 1, i, a.get(i + 1, i) * precision * lambda);
		}

		a.set(fieldLength - 1, fieldLength - 1, precision * (1 - lambda + lambda * a.get(fieldLength - 1, fieldLength - 1)));
		return a;
	}


	@Override
	public void store() {
		super.store();
		System.arraycopy(coalescentIntervals, 0, storedCoalescentIntervals, 0, coalescentIntervals.length);
		System.arraycopy(sufficientStatistics, 0, storedSufficientStatistics, 0, sufficientStatistics.length);
		storedWeightMatrix = weightMatrix.copy();
        storedLogFieldLikelihood = logFieldLikelihood;
	}


	@Override
	public void restore() {
		super.restore();
		System.arraycopy(storedCoalescentIntervals, 0, coalescentIntervals, 0, storedCoalescentIntervals.length);
		System.arraycopy(storedSufficientStatistics, 0, sufficientStatistics, 0, storedSufficientStatistics.length);
		weightMatrix = storedWeightMatrix;
        logFieldLikelihood = storedLogFieldLikelihood;
    }

	@Override
	protected boolean requiresRecalculation() {
		boolean isDirty = false;
        final TreeIntervals ti = treeIntervals.get();
        if (ti != null) {
            //boolean d = ti.isDirtyCalculation();
            //assert d;
            assert ti.isDirtyCalculation();
            isDirty = true;
        } else {
        	isDirty = m_tree.get().somethingIsDirty();
        }

		isDirty =  isDirty || 
			popSizeParameter.somethingIsDirty() ||
			groupSizeParameter.somethingIsDirty() ||
			precisionParameter.somethingIsDirty() ||
			lambdaParameter.somethingIsDirty()
//			dMatrix.somethingIsDirty()
//			betaParameter.somethingIsDirty();
			;

		if (isDirty) {
			likelihoodKnown = false;
			intervalsKnown = false;
		}
		return isDirty;
	}
	
//	protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type){
//		likelihoodKnown = false;
//        // Parameters (precision and popsizes do not change intervals or GMRF Q matrix
//	}

	/**
	 * Calculates the log likelihood of this set of coalescent intervals,
	 * given a demographic model.
     * @return coalescent part of density
	 */
	protected double calculateLogCoalescentLikelihood() {

		if (!intervalsKnown) {
			// intervalsKnown -> false when handleModelChanged event occurs in super.
			wrapSetupIntervals();
			setupGMRFWeights();
            intervalsKnown = true;
		}

		// Matrix operations taken from block update sampler to calculate data likelihood and field prior

		double currentLike = 0;
        Double[] currentGamma = popSizeParameter.getValues();

		for (int i = 0; i < fieldLength; i++) {
			currentLike += -currentGamma[i] - sufficientStatistics[i] * Math.exp(-currentGamma[i]);
		}

		return currentLike;// + LogNormalDistribution.logPdf(Math.exp(popSizeParameter.getParameterValue(coalescentIntervals.length - 1)), mu, sigma);
	}

    protected double calculateLogFieldLikelihood() {

        if (!intervalsKnown) {
            // intervalsKnown -> false when handleModelChanged event occurs in super.
            wrapSetupIntervals();
            setupGMRFWeights();
            intervalsKnown = true;
        }

        double currentLike = 0;
        DenseVector diagonal1 = new DenseVector(fieldLength);
        DenseVector currentGamma = newDenseVector(popSizeParameter);

        SymmTridiagMatrix currentQ = getScaledWeightMatrix(precisionParameter.getValue(0), lambdaParameter.getValue(0));
        currentQ.mult(currentGamma, diagonal1);

//        currentLike += 0.5 * logGeneralizedDeterminant(currentQ) - 0.5 * currentGamma.dot(diagonal1);

        currentLike += 0.5 * (fieldLength - 1) * Math.log(precisionParameter.getValue(0)) - 0.5 * currentGamma.dot(diagonal1);
        if (lambdaParameter.getValue(0) == 1) {
            currentLike -= (fieldLength - 1) / 2.0 * LOG_TWO_TIMES_PI;
        } else {
            currentLike -= fieldLength / 2.0 * LOG_TWO_TIMES_PI;
        }

        return currentLike;
    }


	public static double logGeneralizedDeterminant(SymmTridiagMatrix X) {
		//Set up the eigenvalue solver
		SymmTridiagEVD eigen = new SymmTridiagEVD(X.numRows(), false);
		//Solve for the eigenvalues
		try {
			eigen.factor(X);
		} catch (NotConvergedException e) {
			throw new RuntimeException("Not converged error in generalized determinate calculation.\n" + e.getMessage());
		}

		//Get the eigenvalues
		double[] x = eigen.getEigenvalues();

		double a = 0;
		for (double d : x) {
			if (d > 0.00001)
				a += Math.log(d);
		}

		return a;
	}


	public RealParameter getPrecisionParameter() {
		return precisionParameter;
	}

	public RealParameter getPopSizeParameter() {
		return popSizeParameter;
	}

	public RealParameter getLambdaParameter() {
		return lambdaParameter;
	}

	public SymmTridiagMatrix getWeightMatrix() {
		return weightMatrix.copy();
	}

//	public RealParameter getBetaParameter() {
//		return betaParameter;
//	}

//	public RealParameter getDesignMatrix() {
//		return dMatrix;
//	}

	public double calculateWeightedSSE() {
		double weightedSSE = 0;
		double currentPopSize = popSizeParameter.getValue(0);
		double currentInterval = coalescentIntervals[0];
		for (int j = 1; j < fieldLength; j++) {
			double nextPopSize = popSizeParameter.getValue(j);
			double nextInterval = coalescentIntervals[j];
			double delta = nextPopSize - currentPopSize;
			double weight = (currentInterval + nextInterval) / 2.0;
			weightedSSE += delta * delta / weight;
			currentPopSize = nextPopSize;
			currentInterval = nextInterval;
		}
		return weightedSSE;

	}

	// ****************************************************************
	// Private and protected stuff
	// ****************************************************************
    public static DenseVector newDenseVector(RealParameter x) {
        Double [] Gammas = x.getValues();
        double [] gammas = new double [Gammas.length];
        for (int i = 0; i < gammas.length; i++) {
        	gammas[i] = Gammas[i];
        }
        return new DenseVector(gammas);
    }
}

/*

WinBUGS code to fixed tree:  (A:4.0,(B:2.0,(C:0.5,D:1.0):1.0):2.0)

model {

    stat1 ~ dexp(rate[1])
    stat2 ~ dexp(rate[2])
    stat3 ~ dexp(rate[3])

    rate[1] <- 1 / exp(theta[1])
    rate[2] <- 1 / exp(theta[2])
    rate[3] <- 1 / exp(theta[3])

    theta[1] ~ dnorm(0, 0.001)
    theta[2] ~ dnorm(theta[1], weight[1])
    theta[3] ~ dnorm(theta[2], weight[2])

    weight[1] <- tau / 1.0
    weight[2] <- tau / 1.5

    tau ~ dgamma(1,0.3333)

    stat1 <- 9 / 2
    stat2 <- 6 / 2
    stat3 <- 4 / 2

}
*/