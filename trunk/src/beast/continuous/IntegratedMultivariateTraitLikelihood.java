/*
 * IntegratedMultivariateTraitLikelihood.java
 *
 * Copyright (c) 2002-2014 Alexei Drummond, Andrew Rambaut and Marc Suchard
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

package beast.continuous;


import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import dr.evomodel.continuous.MissingTraits;
import dr.math.distributions.WishartSufficientStatistics;
import beast.core.Input;
import beast.core.parameter.RealParameter;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;
import beast.math.matrixalgebra.SymmetricMatrix;

/**
 * A multivariate trait likelihood that analytically integrates out the unobserved trait values at all internal
 * and root nodes
 *
 * @author Marc A. Suchard
 */
public abstract class IntegratedMultivariateTraitLikelihood extends AbstractMultivariateTraitLikelihood {
    public Input<String> traitName = new Input<String>("traitName", "description here");
//    public Input<MultivariateTraitTree> treeModel = new Input<MultivariateTraitTree>("treeModel", "description here");
    public Input<MultivariateDiffusionModel> diffusionModel = new Input<MultivariateDiffusionModel>("diffusionModel", "description here");
//    public Input<CompoundParameter> traitParameter = new Input<CompoundParameter>("traitParameter", "description here");
    public Input<RealParameter> deltaParameter = new Input<RealParameter>("deltaParameter", "description here");
    public Input<List<Integer>> missingIndices = new Input<List<Integer>>("missingIndices", "description here");
    public Input<Boolean> cacheBranches = new Input<Boolean>("cacheBranches", "description here");
    public Input<Boolean> scaleByTime = new Input<Boolean>("scaleByTime", "description here");
    public Input<Boolean> useTreeLength = new Input<Boolean>("useTreeLength", "description here");
//    public Input<BranchRateModel> rateModel = new Input<BranchRateModel>("rateModel", "description here");
    public Input<List<BranchRateModel>> driftModels = new Input<List<BranchRateModel>>("optimalValues", "description here");
    public Input<List<BranchRateModel>> optimalValues = new Input<List<BranchRateModel>>("optimalValues", "description here");
    public Input<BranchRateModel> strengthOfSelection = new Input<BranchRateModel>("strengthOfSelection", "description here");
//    public Input<Model> samplingDensity = new Input<Model>("samplingDensity", "description here");
    public Input<Boolean> reportAsMultivariate = new Input<Boolean>("reportAsMultivariate", "description here");
    public Input<double []> rootPriorMean = new Input<double []>("rootPriorMean", "description here");
    public Input<Double> rootPriorSampleSize = new Input<Double>("rootPriorSampleSize", "description here");
    public Input<Boolean> reciprocalRates = new Input<Boolean>("reciprocalRates", "description here");

    public static final double LOG_SQRT_2_PI = 0.5 * Math.log(2 * Math.PI);

//    public IntegratedMultivariateTraitLikelihood(String traitName,
//                                                 MultivariateTraitTree treeModel,
//                                                 MultivariateDiffusionModel diffusionModel,
//                                                 CompoundParameter traitParameter,
//                                                 List<Integer> missingIndices,
//                                                 boolean cacheBranches, boolean scaleByTime, boolean useTreeLength,
//                                                 BranchRateModel rateModel, Model samplingDensity,
//                                                 boolean reportAsMultivariate,
//                                                 boolean reciprocalRates) {
//
//        this(traitName, treeModel, diffusionModel, traitParameter, null, missingIndices, cacheBranches, scaleByTime,
//                useTreeLength, rateModel, samplingDensity, reportAsMultivariate, reciprocalRates);
//    }
//
//    public IntegratedMultivariateTraitLikelihood(String traitName,
//                                                 MultivariateTraitTree treeModel,
//                                                 MultivariateDiffusionModel diffusionModel,
//                                                 CompoundParameter traitParameter,
//                                                 Parameter deltaParameter,
//                                                 List<Integer> missingIndices,
//                                                 boolean cacheBranches, boolean scaleByTime, boolean useTreeLength,
//                                                 BranchRateModel rateModel, Model samplingDensity,
//                                                 boolean reportAsMultivariate,
//                                                 boolean reciprocalRates) {
//        this(traitName, treeModel, diffusionModel, traitParameter, deltaParameter, missingIndices, cacheBranches,
//                scaleByTime, useTreeLength, rateModel, null, samplingDensity, reportAsMultivariate, reciprocalRates);
//    }
//
//
    private CacheHelper cacheHelper;
//
//    public IntegratedMultivariateTraitLikelihood(String traitName,
//                                                 MultivariateTraitTree treeModel,
//                                                 MultivariateDiffusionModel diffusionModel,
//                                                 CompoundParameter traitParameter,
//                                                 Parameter deltaParameter,
//                                                 List<Integer> missingIndices,
//                                                 boolean cacheBranches, boolean scaleByTime, boolean useTreeLength,
//                                                 BranchRateModel rateModel,
//                                                 List<BranchRateModel> driftModels,
//                                                 Model samplingDensity,
//                                                 boolean reportAsMultivariate,
//                                                 boolean reciprocalRates) {
//
//        super(traitName, treeModel, diffusionModel, traitParameter, deltaParameter, missingIndices, cacheBranches, scaleByTime,
//                useTreeLength, rateModel, driftModels, samplingDensity, reportAsMultivariate, reciprocalRates);

    public void initAndValidate() throws Exception {
    	super.initAndValidate();
    	
        // Delegate caches to helper
        meanCache = new double[dim * treeModel.getNodeCount()];
        if (driftModels.get() != null) {
            cacheHelper = new DriftCacheHelper(dim * treeModel.getNodeCount(), cacheBranches.get()); // new DriftCacheHelper ....
        } else {
            cacheHelper = new CacheHelper(dim * treeModel.getNodeCount(), cacheBranches.get());
        }

        drawnStates = new double[dim * treeModel.getNodeCount()];
        upperPrecisionCache = new double[treeModel.getNodeCount()];
        lowerPrecisionCache = new double[treeModel.getNodeCount()];
        logRemainderDensityCache = new double[treeModel.getNodeCount()];

        if (cacheBranches.get()) {
            storedMeanCache = new double[dim * treeModel.getNodeCount()];
            storedUpperPrecisionCache = new double[treeModel.getNodeCount()];
            storedLowerPrecisionCache = new double[treeModel.getNodeCount()];
            storedLogRemainderDensityCache = new double[treeModel.getNodeCount()];
        }

        // Set up reusable temporary storage
        Ay = new double[dimTrait];
        tmpM = new double[dimTrait][dimTrait];
        tmp2 = new double[dimTrait];

        zeroDimVector = new double[dim];

        missingTraits = new MissingTraits.CompletelyMissing(treeModel, missingIndices.get(), dim);
        setTipDataValuesForAllNodes();

    }

    public IntegratedMultivariateTraitLikelihood(String traitName,
                                                 TreeInterface treeModel,
                                                 MultivariateDiffusionModel diffusionModel,
                                                 RealParameter traitParameter,
                                                 RealParameter deltaParameter,
                                                 List<Integer> missingIndices,
                                                 boolean cacheBranches, boolean scaleByTime, boolean useTreeLength,
                                                 BranchRateModel rateModel,
                                                 List<BranchRateModel> optimalValues,
                                                 BranchRateModel strengthOfSelection,
                                                 //Model samplingDensity,
                                                 boolean reportAsMultivariate,
                                                 boolean reciprocalRates) {

        //super(traitName, treeModel, diffusionModel, traitParameter, deltaParameter, missingIndices, cacheBranches, scaleByTime,
        //       useTreeLength, rateModel, optimalValues, strengthOfSelection, samplingDensity, reportAsMultivariate, reciprocalRates);

        // Delegate caches to helper
        meanCache = new double[dim * treeModel.getNodeCount()];

        if (optimalValues != null) {
            cacheHelper = new OUCacheHelper(dim * treeModel.getNodeCount(), cacheBranches);
        } else {
            cacheHelper = new CacheHelper(dim * treeModel.getNodeCount(), cacheBranches);
        }


        drawnStates = new double[dim * treeModel.getNodeCount()];
        upperPrecisionCache = new double[treeModel.getNodeCount()];
        lowerPrecisionCache = new double[treeModel.getNodeCount()];
        logRemainderDensityCache = new double[treeModel.getNodeCount()];

        if (cacheBranches) {
            storedMeanCache = new double[dim * treeModel.getNodeCount()];
            storedUpperPrecisionCache = new double[treeModel.getNodeCount()];
            storedLowerPrecisionCache = new double[treeModel.getNodeCount()];
            storedLogRemainderDensityCache = new double[treeModel.getNodeCount()];
        }

        // Set up reusable temporary storage
        Ay = new double[dimTrait];
        tmpM = new double[dimTrait][dimTrait];
        tmp2 = new double[dimTrait];

        zeroDimVector = new double[dim];

        missingTraits = new MissingTraits.CompletelyMissing(treeModel, missingIndices, dim);
        setTipDataValuesForAllNodes();

    }


    private void setTipDataValuesForAllNodes() {
        for (int i = 0; i < treeModel.getLeafNodeCount(); i++) {
            Node node = treeModel.getNode(i);
            setTipDataValuesForNode(node);
        }
        missingTraits.handleMissingTips();
    }

    @SuppressWarnings("unused")
    public double getTotalTreePrecision() {
        getLogLikelihood(); // Do peeling if necessary
        final int rootIndex = treeModel.getRoot().getNr();
        return lowerPrecisionCache[rootIndex];
    }

    private void setTipDataValuesForNode(Node node) {
        // Set tip data values
        int index = node.getNr();
        double[] traitValue = new double[traitParameter.getMinorDimension1()];
        for (int i = 0; i < traitValue.length; i++) {
        	traitValue[i] = traitParameter.getMatrixValue(index, i);
        }
        if (traitValue.length < dim) {
            throw new RuntimeException("The trait parameter for the tip with index, " + index + ", is too short");
        }

        cacheHelper.setTipMeans(traitValue, dim, index, node);
//        System.arraycopy(traitValue, 0, meanCache
////                cacheHelper.getMeanCache()
//                , dim * index, dim);
    }

    public double[] getTipDataValues(int index) {
        double[] traitValue = new double[dim];
        System.arraycopy(cacheHelper.getMeanCache(), dim * index, traitValue, 0, dim);
        return traitValue;
    }

    public void setTipDataValuesForNode(int index, double[] traitValue) {
        // Set tip data values
        // cacheHelper.copyToMeanCache(traitValue, dim*index, dim);
        cacheHelper.setTipMeans(traitValue, dim, index);
        //System.arraycopy(traitValue, 0, meanCache, dim * index, dim);
        makeDirty();
    }

    protected String extraInfo() {
        return "\tSample internal node traits: false\n";
    }

//    public List<Citation> getCitations() {
//        List<Citation> citations = super.getCitations();
//        citations.add(
//                new Citation(
//                        new Author[]{
//                                new Author("O", "Pybus"),
//                                new Author("P", "Lemey"),
//                                new Author("A", "Rambaut"),
//                                new Author("MA", "Suchard")
//                        },
//                        Citation.Status.IN_PREPARATION
//                )
//        );
//        return citations;
//    }

    public double getLogDataLikelihood() {
        return getLogLikelihood();
    }

    public abstract boolean getComputeWishartSufficientStatistics();

    public double calculateLogLikelihood() {

        double logLikelihood = 0;
        double[][] traitPrecision = diffusionModel.get().getPrecisionmatrix();
        double logDetTraitPrecision = Math.log(diffusionModel.get().getDeterminantPrecisionMatrix());
        double[] conditionalRootMean = tmp2;

        final boolean computeWishartStatistics = getComputeWishartSufficientStatistics();

        if (computeWishartStatistics) {
//            if (wishartStatistics == null) {
            wishartStatistics = new WishartSufficientStatistics(dimTrait);
//            } else {
//                wishartStatistics.clear();
//            }
        }

        // Use dynamic programming to compute conditional likelihoods at each internal node
        postOrderTraverse(treeModel, treeModel.getRoot(), traitPrecision, logDetTraitPrecision, computeWishartStatistics);

//        if (DEBUG) {
//            System.err.println("mean: " + new Vector(cacheHelper.getMeanCache()));
//            System.err.println("correctedMean: " + new Vector(cacheHelper.getCorrectedMeanCache()));
//            System.err.println("upre: " + new Vector(upperPrecisionCache));
//            System.err.println("lpre: " + new Vector(lowerPrecisionCache));
//            System.err.println("cach: " + new Vector(logRemainderDensityCache));
//        }

        // Compute the contribution of each datum at the root
        final int rootIndex = treeModel.getRoot().getNr();

        // Precision scalar of datum conditional on root
        double conditionalRootPrecision = lowerPrecisionCache[rootIndex];

        for (int datum = 0; datum < numData; datum++) {

            double thisLogLikelihood = 0;

            // Get conditional mean of datum conditional on root
            // System.arraycopy(meanCache, rootIndex * dim + datum * dimTrait, conditionalRootMean, 0, dimTrait);
            System.arraycopy(cacheHelper.getMeanCache(), rootIndex * dim + datum * dimTrait, conditionalRootMean, 0, dimTrait);

//            if (DEBUG) {
//                System.err.println("Datum #" + datum);
//                System.err.println("root mean: " + new Vector(conditionalRootMean));
//                System.err.println("root prec: " + conditionalRootPrecision);
//                System.err.println("diffusion prec: " + new Matrix(traitPrecision));
//            }

            // B = root prior precision
            // z = root prior mean
            // A = likelihood precision
            // y = likelihood mean

            // y'Ay
            double yAy = computeWeightedAverageAndSumOfSquares(conditionalRootMean, Ay, traitPrecision, dimTrait,
                    conditionalRootPrecision); // Also fills in Ay

            if (conditionalRootPrecision != 0) {
                thisLogLikelihood += -LOG_SQRT_2_PI * dimTrait
                        + 0.5 * (logDetTraitPrecision + dimTrait * Math.log(conditionalRootPrecision) - yAy);
            }

//            if (DEBUG) {
//                double[][] T = new double[dimTrait][dimTrait];
//                for (int i = 0; i < dimTrait; i++) {
//                    for (int j = 0; j < dimTrait; j++) {
//                        T[i][j] = traitPrecision[i][j] * conditionalRootPrecision;
//                    }
//                }
//                System.err.println("Conditional root MVN precision = \n" + new Matrix(T));
//                System.err.println("Conditional root MVN density = " + dr.math.distributions.MultivariateNormalDistribution.logPdf(
//                        conditionalRootMean, new double[dimTrait], T,
//                        Math.log(dr.math.distributions.MultivariateNormalDistribution.calculatePrecisionMatrixDeterminate(T)), 1.0));
//            }

            if (integrateRoot) {
                // Integrate root trait out against rootPrior
                thisLogLikelihood += integrateLogLikelihoodAtRoot(conditionalRootMean, Ay, tmpM, traitPrecision,
                        conditionalRootPrecision); // Ay is destroyed
            }

            if (DEBUG) {
                System.err.println("yAy = " + yAy);
                System.err.println("logLikelihood (before remainders) = " + thisLogLikelihood +
                        " (should match conditional root MVN density when root not integrated out)");
            }

            logLikelihood += thisLogLikelihood;
        }

        logLikelihood += sumLogRemainders();
        if (DEBUG) {
            System.out.println("logLikelihood is " + logLikelihood);
        }

        if (DEBUG) { // Root trait is univariate!!!
            System.err.println("logLikelihood (final) = " + logLikelihood);
//            checkViaLargeMatrixInversion();
        }

        if (DEBUG_PNAS) {
            checkLogLikelihood(logLikelihood, sumLogRemainders(), conditionalRootMean,
                    conditionalRootPrecision, traitPrecision);
        }

        areStatesRedrawn = false;  // Should redraw internal node states when needed
        return logLikelihood;
    }

    protected void checkLogLikelihood(double loglikelihood, double logRemainders,
                                      double[] conditionalRootMean, double conditionalRootPrecision,
                                      double[][] traitPrecision) {
        // Do nothing; for checking PNAS paper
    }

//    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
//        if (variable == traitParameter) { // A tip value got updated
//            if (index > dimTrait * treeModel.getLeafNodeCount()) {
//                throw new RuntimeException("Attempting to update an invalid index");
//            }
//
//            if (index != -1) {
//                cacheHelper.setMeanCache(index, traitParameter.getValue(index));
//            } else {
//                for (int idx = 0; idx < traitParameter.getDimension(); ++idx) {
//                    cacheHelper.setMeanCache(idx, traitParameter.getValue(idx));
//                }
//            }
//            // meanCache[index] = traitParameter.getValue(index);
//            likelihoodKnown = false;
////            if (!cacheBranches) {
////                throw new RuntimeException("Must cache means in IMTL if they are random");
////            }
//            // TODO Need better solution.  If tips are random, cacheBranches should be true (to get reset).
//            // TODO However, jitter calls setParameterValue() on the tips at initialization
//        }
//        super.handleVariableChangedEvent(variable, index, type);
//    }
    
    @Override
    protected boolean requiresRecalculation() {
    	return super.requiresRecalculation();
    }

    protected static double computeWeightedAverageAndSumOfSquares(double[] y, double[] Ay, double[][] A,
                                                                  int dim, double scale) {
        // returns Ay and yAy
        double yAy = 0;
        for (int i = 0; i < dim; i++) {
            Ay[i] = 0;
            for (int j = 0; j < dim; j++)
                Ay[i] += A[i][j] * y[j] * scale;
            yAy += y[i] * Ay[i];
        }
        return yAy;
    }

    private double sumLogRemainders() {
        double sumLogRemainders = 0;
        for (double r : logRemainderDensityCache)
            sumLogRemainders += r;
        // Could skip leafs
        return sumLogRemainders;
    }

    protected abstract double integrateLogLikelihoodAtRoot(double[] conditionalRootMean,
                                                           double[] marginalRootMean,
                                                           double[][] temporaryStorage,
                                                           double[][] treePrecisionMatrix,
                                                           double conditionalRootPrecision);

    public void makeDirty() {
        super.makeDirty();
        areStatesRedrawn = false;
    }

    void postOrderTraverse(/*MultivariateTraitTree*/ TreeInterface treeModel, Node node, double[][] precisionMatrix,
                           double logDetPrecisionMatrix, boolean cacheOuterProducts) {

        final int thisNumber = node.getNr();

        if (node.isLeaf()) {

            // Fill in precision scalar, traitValues already filled in

            if (missingTraits.isCompletelyMissing(thisNumber)) {
                upperPrecisionCache[thisNumber] = 0;
                lowerPrecisionCache[thisNumber] = 0; // Needed in the pre-order traversal
            } else { // not missing tip trait
                // changeou
                //    upperPrecisionCache[thisNumber] = (1.0 / getRescaledBranchLengthForPrecision(node)) * Math.pow(cacheHelper.getOUFactor(node), 2);
                upperPrecisionCache[thisNumber] = cacheHelper.getUpperPrecFactor(node) * Math.pow(cacheHelper.getOUFactor(node), 2);
                lowerPrecisionCache[thisNumber] = Double.POSITIVE_INFINITY;
            }
            return;
        }

        final Node childNode0 = node.getChild(0);
        final Node childNode1 = node.getChild(1);

        postOrderTraverse(treeModel, childNode0, precisionMatrix, logDetPrecisionMatrix, cacheOuterProducts);
        postOrderTraverse(treeModel, childNode1, precisionMatrix, logDetPrecisionMatrix, cacheOuterProducts);

        final int childNumber0 = childNode0.getNr();
        final int childNumber1 = childNode1.getNr();
        final int meanOffset0 = dim * childNumber0;
        final int meanOffset1 = dim * childNumber1;
        final int meanThisOffset = dim * thisNumber;

        final double precision0 = upperPrecisionCache[childNumber0];
        final double precision1 = upperPrecisionCache[childNumber1];
        final double totalPrecision = precision0 + precision1;

        lowerPrecisionCache[thisNumber] = totalPrecision;

        // Multiply child0 and child1 densities

        // changeou
        cacheHelper.computeMeanCaches(meanThisOffset, meanOffset0, meanOffset1,
                totalPrecision, precision0, precision1, missingTraits, node, childNode0, childNode1);
//        if (totalPrecision == 0) {
//            System.arraycopy(zeroDimVector, 0, meanCache, meanThisOffset, dim);
//        } else {
//            // Delegate in case either child is partially missing
//            // computeCorrectedWeightedAverage
//            missingTraits.computeWeightedAverage(meanCache,
//                    meanOffset0, precision0,
//                    meanOffset1, precision1,
//                    meanThisOffset, dim);
//        }
        // In this delegation, you can call
        //getShiftForBranchLength(node);

        if (!node.isRoot()) {
            // Integrate out trait value at this node
            //changeou
            //  double thisPrecision = 1.0 / getRescaledBranchLengthForPrecision(node);
            double thisPrecision = cacheHelper.getUpperPrecFactor(node);
            if (Double.isInfinite(thisPrecision)) {
                // must handle this case for ouprocess
                upperPrecisionCache[thisNumber] = totalPrecision;
            } else {
                upperPrecisionCache[thisNumber] = (totalPrecision * thisPrecision / (totalPrecision + thisPrecision)) * Math.pow(cacheHelper.getOUFactor(node), 2);
            }
        }

        // Compute logRemainderDensity

        logRemainderDensityCache[thisNumber] = 0;

        if (precision0 != 0 && precision1 != 0) {
            // changeou
            incrementRemainderDensities(
                    precisionMatrix,
                    logDetPrecisionMatrix, thisNumber, meanThisOffset,
                    meanOffset0,
                    meanOffset1,
                    precision0,
                    precision1,
                    cacheHelper.getOUFactor(childNode0),
                    cacheHelper.getOUFactor(childNode1),
                    cacheOuterProducts);
        }
    }

    private void incrementRemainderDensities(double[][] precisionMatrix,
                                             double logDetPrecisionMatrix,
                                             int thisIndex,
                                             int thisOffset,
                                             int childOffset0,
                                             int childOffset1,
                                             double precision0,
                                             double precision1,
                                             double OUFactor0,
                                             double OUFactor1,
                                             boolean cacheOuterProducts) {

        final double remainderPrecision = precision0 * precision1 / (precision0 + precision1);

        if (cacheOuterProducts) {
            incrementOuterProducts(thisOffset, childOffset0, childOffset1, precision0, precision1);
        }

        for (int k = 0; k < numData; k++) {

            double childSS0 = 0;
            double childSS1 = 0;
            double crossSS = 0;

            for (int i = 0; i < dimTrait; i++) {

                // In case of no drift, getCorrectedMeanCache() simply returns mean cache
                // final double wChild0i = meanCache[childOffset0 + k * dimTrait + i] * precision0;
                final double wChild0i = cacheHelper.getCorrectedMeanCache()[childOffset0 + k * dimTrait + i] * precision0;
                // final double wChild1i = meanCache[childOffset1 + k * dimTrait + i] * precision1;
                final double wChild1i = cacheHelper.getCorrectedMeanCache()[childOffset1 + k * dimTrait + i] * precision1;

                for (int j = 0; j < dimTrait; j++) {

                    // subtract "correction"
                    //final double child0j = meanCache[childOffset0 + k * dimTrait + j];
                    final double child0j = cacheHelper.getCorrectedMeanCache()[childOffset0 + k * dimTrait + j];
                    // subtract "correction"
                    //final double child1j = meanCache[childOffset1 + k * dimTrait + j];
                    final double child1j = cacheHelper.getCorrectedMeanCache()[childOffset1 + k * dimTrait + j];

                    childSS0 += wChild0i * precisionMatrix[i][j] * child0j;
                    childSS1 += wChild1i * precisionMatrix[i][j] * child1j;

                    // make sure meanCache in following is not "corrected"
                    // crossSS += (wChild0i + wChild1i) * precisionMatrix[i][j] * meanCache[thisOffset + k * dimTrait + j];
                    crossSS += (wChild0i + wChild1i) * precisionMatrix[i][j] * cacheHelper.getMeanCache()[thisOffset + k * dimTrait + j];
                }
            }

            logRemainderDensityCache[thisIndex] +=
                    -dimTrait * LOG_SQRT_2_PI
                            + 0.5 * (dimTrait * Math.log(remainderPrecision) + logDetPrecisionMatrix)
                            - 0.5 * (childSS0 + childSS1 - crossSS)
                            // changeou
                            - dimTrait * (Math.log(OUFactor0) + Math.log(OUFactor1));
            //               double tempnum = childSS0 + childSS1 - crossSS;
            //   System.err.println("childSS0 + childSS1 - crossSS:  " + tempnum);
        }
        //     System.err.println("logRemainderDensity: " + logRemainderDensityCache[thisIndex]);
        //    System.err.println("thisIndex: " + thisIndex);
        //   System.err.println("remainder precision: " + remainderPrecision);
        // System.err.println("precision0: " + precision0);
        // System.err.println("precision1: " + precision1);
        // System.err.println("precision0*precision1: " + precision0*precision1);

        //    System.err.println("logDetPrecisionMatrix: " + logDetPrecisionMatrix);

    }

    private void incrementOuterProducts(int thisOffset,
                                        int childOffset0,
                                        int childOffset1,
                                        double precision0,
                                        double precision1) {

        final double[][] outerProduct = wishartStatistics.getScaleMatrix();

        for (int k = 0; k < numData; k++) {

            for (int i = 0; i < dimTrait; i++) {

                // final double wChild0i = meanCache[childOffset0 + k * dimTrait + i] * precision0;
                // final double wChild1i = meanCache[childOffset1 + k * dimTrait + i] * precision1;
                final double wChild0i = cacheHelper.getCorrectedMeanCache()[childOffset0 + k * dimTrait + i] * precision0;
                final double wChild1i = cacheHelper.getCorrectedMeanCache()[childOffset1 + k * dimTrait + i] * precision1;

                for (int j = 0; j < dimTrait; j++) {

                    //final double child0j = meanCache[childOffset0 + k * dimTrait + j];
                    //final double child1j = meanCache[childOffset1 + k * dimTrait + j];
                    final double child0j = cacheHelper.getCorrectedMeanCache()[childOffset0 + k * dimTrait + j];
                    final double child1j = cacheHelper.getCorrectedMeanCache()[childOffset1 + k * dimTrait + j];

                    outerProduct[i][j] += wChild0i * child0j;
                    outerProduct[i][j] += wChild1i * child1j;

                    //outerProduct[i][j] -= (wChild0i + wChild1i) * meanCache[thisOffset + k * dimTrait + j];
                    outerProduct[i][j] -= (wChild0i + wChild1i) * cacheHelper.getMeanCache()[thisOffset + k * dimTrait + j];
                }
            }
        }
        wishartStatistics.incrementDf(1); // Peeled one node
    }

//    private void computeWeightedMeanCache(int thisOffset,
//                                          int childOffset0,
//                                          int childOffset1,
//                                          double precision0,
//                                          double precision1) {
//
//        final double totalVariance = 1.0 / (precision0 + precision1);
//        for (int i = 0; i < dim; i++) {
//            meanCache[thisOffset + i] = (meanCache[childOffset0 + i] * precision0 +
//                    meanCache[childOffset1 + i] * precision1)
//                    * totalVariance;
//        }
//    }

    protected double[] getRootNodeTrait() {
        return getTraitForNode(treeModel, treeModel.getRoot(), traitName.get());
    }

    public double[] getTraitForNode(TreeInterface tree, Node node, String traitName) {

//        if (tree != treeModel) {
//            throw new RuntimeException("Can only reconstruct states on treeModel given to constructor");
//        }

        getLogLikelihood();

        if (!areStatesRedrawn)
            redrawAncestralStates();

        int index = node.getNr();

        double[] trait = new double[dim];
        System.arraycopy(drawnStates, index * dim, trait, 0, dim);
        return trait;
    }

    public void redrawAncestralStates() {

        double[][] treePrecision = diffusionModel.get().getPrecisionmatrix();
        double[][] treeVariance = new SymmetricMatrix(treePrecision).inverse().toComponents();

        preOrderTraverseSample(treeModel, treeModel.getRoot(), 0, treePrecision, treeVariance);

//        if (DEBUG) {
//            System.err.println("all draws = " + new Vector(drawnStates));
//        }

        areStatesRedrawn = true;
    }

    @Override
    public void store() {
        super.store();

        if (cacheBranches.get()) {
            //     System.arraycopy(meanCache, 0, storedMeanCache, 0, meanCache.length);
            cacheHelper.store();
            System.arraycopy(upperPrecisionCache, 0, storedUpperPrecisionCache, 0, upperPrecisionCache.length);
            System.arraycopy(lowerPrecisionCache, 0, storedLowerPrecisionCache, 0, lowerPrecisionCache.length);
            System.arraycopy(logRemainderDensityCache, 0, storedLogRemainderDensityCache, 0, logRemainderDensityCache.length);
        }
    }

    @Override
    public void restore() {
        super.restore();

        if (cacheBranches.get()) {
            double[] tmp;

            cacheHelper.restore();
            //  tmp = storedMeanCache;
            //  storedMeanCache = meanCache;
            //  meanCache = tmp;

            tmp = storedUpperPrecisionCache;
            storedUpperPrecisionCache = upperPrecisionCache;
            upperPrecisionCache = tmp;

            tmp = storedLowerPrecisionCache;
            storedLowerPrecisionCache = lowerPrecisionCache;
            lowerPrecisionCache = tmp;

            tmp = storedLogRemainderDensityCache;
            storedLogRemainderDensityCache = logRemainderDensityCache;
            logRemainderDensityCache = tmp;
        }
    }


    // Computes x^t A y, used many times in these computations

    protected static double computeQuadraticProduct(Double[] x, double[][] A, Double[] y, int dim) {
        double sum = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                sum += x[i] * A[i][j] * y[j];
            }
        }
        return sum;
    }
    protected static double computeQuadraticProduct(double[] x, double[][] A, double[] y, int dim) {
        double sum = 0;
        for (int i = 0; i < dim; i++) {
            for (int j = 0; j < dim; j++) {
                sum += x[i] * A[i][j] * y[j];
            }
        }
        return sum;
    }

    // Computes the weighted average of two vectors, used many times in these computations

    public static void computeWeightedAverage(double[] in0, int offset0, double weight0,
                                              Double[] in1, int offset1, double weight1,
                                              double[] out2, int offset2,
                                              int length) {

        final double totalInverseWeight = 1.0 / (weight0 + weight1);
        for (int i = 0; i < length; i++) {
            out2[offset2 + i] = (in0[offset0 + i] * weight0 + in1[offset1 + i] * weight1) * totalInverseWeight;
        }
    }

    public double[] getShiftForBranchLength(Node node) {
        if (driftModels.get() != null) {
            final int dim = driftModels.get().size();
            double[] drift = new double[dim];
            double realTimeBranchLength = node.getLength();
            for (int i = 0; i < dim; ++i) {
                drift[i] = driftModels.get().get(i).getRateForBranch(node) * realTimeBranchLength;
            }
            return drift;
        } else {
            throw new RuntimeException("getShiftForBranchLength should not be called.");
        }
        // But really should get values from driftModel.getBranchRate(treeModel, node);
    }

    protected void computeCorrectedWeightedAverage(int offset0, double weight0, Node childNode0,
                                                   int offset1, double weight1, Node childNode1,
                                                   int offset2,
                                                   int length, Node thisNode) {

        final double totalInverseWeight = 1.0 / (weight0 + weight1);
        // TODO fix
//        final double length0 = getRescaledBranchLength(childNode0);
//        final double length1 = getRescaledBranchLength(childNode1);
//        final double thisLength = getRescaledBranchLength(thisNode);
        double[] shift;
        if (!thisNode.isRoot()) {
            shift = getShiftForBranchLength(thisNode);
        } else {
            shift = null;
        }
        double[] shiftChild0 = getShiftForBranchLength(childNode0);
        double[] shiftChild1 = getShiftForBranchLength(childNode1);

        if (childNode0.isLeaf()) {
            for (int i = 0; i < length; i++) {
                correctedMeanCache[offset0 + i] = meanCache[offset0 + i] - shiftChild0[i];
            }
        }

        if (childNode1.isLeaf()) {
            for (int i = 0; i < length; i++) {
                correctedMeanCache[offset1 + i] = meanCache[offset1 + i] - shiftChild1[i];
            }
        }

        for (int i = 0; i < length; i++) {

            // meanCache[offset2 + i] = ((meanCache[offset0 + i] -  shiftChild0[i]) * weight0 + (meanCache[offset1 + i] - shiftChild1[i]) * weight1) * totalInverseWeight;
            meanCache[offset2 + i] = (correctedMeanCache[offset0 + i] * weight0 + correctedMeanCache[offset1 + i] * weight1) * totalInverseWeight;
            if (!thisNode.isRoot()) {
                correctedMeanCache[offset2 + i] = meanCache[offset2 + i] - shift[i];
            } else {
                correctedMeanCache[offset2 + i] = meanCache[offset2 + i];
            }
        }
    }


    protected void computeCorrectedOUWeightedAverage(int offset0, double weight0, Node childNode0,
                                                     int offset1, double weight1, Node childNode1,
                                                     int offset2,
                                                     int length, Node thisNode) {

        final double totalInverseWeight = 1.0 / (weight0 + weight1);

        double[] optimal;
        double selection;

        if (!thisNode.isRoot()) {
            optimal = getOptimalValue(thisNode);
            selection = getTimeScaledSelection(thisNode);
        } else {
            optimal = null;
            selection = 1;
        }
        double[] optimalChild0 = getOptimalValue(childNode0);
        double[] optimalChild1 = getOptimalValue(childNode1);
        double selectionChild0 = getTimeScaledSelection(childNode0);
        double selectionChild1 = getTimeScaledSelection(childNode1);

        /*

        if (treeModel.isExternal(childNode0)) {
            for (int i = 0; i < length; i++) {
                correctedMeanCache[offset0 + i] = (meanCache[offset0 + i] - selectionChild0 * optimalChild0[i]) / (1 - selectionChild0);
            }
        }

        if (treeModel.isExternal(childNode1)) {
            for (int i = 0; i < length; i++) {
                correctedMeanCache[offset1 + i] = (meanCache[offset1 + i] - selectionChild1 * optimalChild1[i]) / (1 - selectionChild1);
            }
        }

        for (int i = 0; i < length; i++) {

            // meanCache[offset2 + i] = ((meanCache[offset0 + i] -  shiftChild0[i]) * weight0 + (meanCache[offset1 + i] - shiftChild1[i]) * weight1) * totalInverseWeight;
            meanCache[offset2 + i] = (correctedMeanCache[offset0 + i] * weight0 + correctedMeanCache[offset1 + i] * weight1) * totalInverseWeight;
            if (!treeModel.isRoot(thisNode)) {
                correctedMeanCache[offset2 + i] = (meanCache[offset2 + i] - selection * optimal[i]) / (1 - selection);
            } else {
                correctedMeanCache[offset2 + i] = meanCache[offset2 + i];
            }
        }

        */

        if (childNode0.isLeaf()) {
            for (int i = 0; i < length; i++) {
                correctedMeanCache[offset0 + i] = Math.exp(selectionChild0) * meanCache[offset0 + i] - (Math.exp(selectionChild0) - 1) * optimalChild0[i];
            }
        }

        if (childNode1.isLeaf()) {
            for (int i = 0; i < length; i++) {
                correctedMeanCache[offset1 + i] = Math.exp(selectionChild1) * meanCache[offset1 + i] - (Math.exp(selectionChild1) - 1) * optimalChild1[i];
            }
        }

        for (int i = 0; i < length; i++) {

            // meanCache[offset2 + i] = ((meanCache[offset0 + i] -  shiftChild0[i]) * weight0 + (meanCache[offset1 + i] - shiftChild1[i]) * weight1) * totalInverseWeight;
            meanCache[offset2 + i] = (correctedMeanCache[offset0 + i] * weight0 + correctedMeanCache[offset1 + i] * weight1) * totalInverseWeight;
            if (!thisNode.isRoot()) {
                correctedMeanCache[offset2 + i] = Math.exp(selection) * meanCache[offset2 + i] - (Math.exp(selection) - 1) * optimal[i];
            } else {
                correctedMeanCache[offset2 + i] = meanCache[offset2 + i];
            }
        }


    }


    protected abstract double[][] computeMarginalRootMeanAndVariance(double[] conditionalRootMean,
                                                                     double[][] treePrecisionMatrix,
                                                                     double[][] treeVarianceMatrix,
                                                                     double conditionalRootPrecision);


    private void preOrderTraverseSample(/*MultivariateTraitTree*/ TreeInterface treeModel, Node node, int parentIndex, double[][] treePrecision,
                                        double[][] treeVariance) {
        //   System.err.println("preOrderTraverseSample got called!!");
        //  System.exit(-1);
        final int thisIndex = node.getNr();

        if (node.isRoot()) {
            // draw root

            double[] rootMean = new double[dimTrait];
            final int rootIndex = treeModel.getRoot().getNr();
            double rootPrecision = lowerPrecisionCache[rootIndex];

            for (int datum = 0; datum < numData; datum++) {
                // System.arraycopy(meanCache, thisIndex * dim + datum * dimTrait, rootMean, 0, dimTrait);
                System.arraycopy(cacheHelper.getMeanCache(), thisIndex * dim + datum * dimTrait, rootMean, 0, dimTrait);

                double[][] variance = computeMarginalRootMeanAndVariance(rootMean, treePrecision, treeVariance,
                        rootPrecision);

                double[] draw = dr.math.distributions.MultivariateNormalDistribution.nextMultivariateNormalVariance(rootMean, variance);

                if (DEBUG_PREORDER) {
                    Arrays.fill(draw, 1.0);
                }

                System.arraycopy(draw, 0, drawnStates, rootIndex * dim + datum * dimTrait, dimTrait);
                //                  DEBUG=true;
//                if (DEBUG) {
//                    System.err.println("Root mean: " + new Vector(rootMean));
//                    System.err.println("Root var : " + new Matrix(variance));
//                    System.err.println("Root draw: " + new Vector(draw));
//                }
            }
        } else { // draw conditional on parentState

            if (!missingTraits.isCompletelyMissing(thisIndex)
                    && !missingTraits.isPartiallyMissing(thisIndex)) {

                //System.arraycopy(meanCache, thisIndex * dim, drawnStates, thisIndex * dim, dim);
                System.arraycopy(cacheHelper.getMeanCache(), thisIndex * dim, drawnStates, thisIndex * dim, dim);
                //  System.err.println("I got here");
                //  System.exit(-1);
            } else {

                //        System.err.println("I got here");
                //     System.exit(-1);

                if (missingTraits.isPartiallyMissing(thisIndex)) {
                    throw new RuntimeException("Partially missing values are not yet implemented");
                }
                // This code should work for sampling a missing tip trait as well, but needs testing

                // parent trait at drawnStates[parentOffset]
                double precisionToParent = 1.0 / getRescaledBranchLengthForPrecision(node);
                double precisionOfNode = lowerPrecisionCache[thisIndex];
                double totalPrecision = precisionOfNode + precisionToParent;

                double[] mean = Ay; // temporary storage
                double[][] var = tmpM; // temporary storage

                for (int datum = 0; datum < numData; datum++) {

                    int parentOffset = parentIndex * dim + datum * dimTrait;
                    int thisOffset = thisIndex * dim + datum * dimTrait;
                    //                   DEBUG=true;
//                    if (DEBUG) {
//                        double[] parentValue = new double[dimTrait];
//                        System.arraycopy(drawnStates, parentOffset, parentValue, 0, dimTrait);
//                        System.err.println("Parent draw: " + new Vector(parentValue));
//                        if (parentValue[0] != drawnStates[parentOffset]) {
//                            throw new RuntimeException("Error in setting indices");
//                        }
//                    }

                    for (int i = 0; i < dimTrait; i++) {
                        mean[i] = (drawnStates[parentOffset + i] * precisionToParent
                                //  + meanCache[thisOffset + i] * precisionOfNode) / totalPrecision;
                                + cacheHelper.getMeanCache()[thisOffset + i] * precisionOfNode) / totalPrecision;
                        for (int j = 0; j < dimTrait; j++) {
                            var[i][j] = treeVariance[i][j] / totalPrecision;
                        }
                    }
                    double[] draw = dr.math.distributions.MultivariateNormalDistribution.nextMultivariateNormalVariance(mean, var);
                    System.arraycopy(draw, 0, drawnStates, thisOffset, dimTrait);

//                    if (DEBUG) {
//                        System.err.println("Int prec: " + totalPrecision);
//                        System.err.println("Int mean: " + new Vector(mean));
//                        System.err.println("Int var : " + new Matrix(var));
//                        System.err.println("Int draw: " + new Vector(draw));
//                        System.err.println("");
//                    }
                }
            }
        }

        if (peel() && !node.isLeaf()) {
            preOrderTraverseSample(treeModel, node.getChild(0), thisIndex, treePrecision, treeVariance);
            preOrderTraverseSample(treeModel, node.getChild(1), thisIndex, treePrecision, treeVariance);
        }
    }

//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//
//        if (driftModels != null && driftModels.contains(model)) {
//            if (cacheBranches) {
//                updateAllNodes();
//            } else {
//                likelihoodKnown = false;
//            }
//        } else {
//            super.handleModelChangedEvent(model, object, index);
//        }
//    }

    protected boolean peel() {
        return true;
    }

//    public LogColumn[] getColumns() {
//        return new LogColumn[]{
//                new LikelihoodColumn(getId())};
//    }

    protected boolean areStatesRedrawn = false;

    protected double[] meanCache;
    protected double[] correctedMeanCache;

    public double getRescaledBranchLengthForPrecision(Node node) {

        double length = node.getLength();

        if (branchRateModelInput.get() != null) {
            if (reciprocalRates.get()) {
                length /= branchRateModelInput.get().getRateForBranch(node); // branch rate scales as precision (inv-time)
            } else {
                length *= branchRateModelInput.get().getRateForBranch(node); // branch rate scales as variance (time)
            }
        }

        if (scaleByTime.get()) {
            length /= treeLength;
        }

        if (deltaParameter != null && node.isLeaf()) {
            length += deltaParameter.get().getArrayValue(0);
        }
        //System.err.println("Node Number: " + node.getNumber());

        //System.err.println("Trait value" + traitParameter.getParameterValue(0));
        //System.err.println("Trait value" + traitParameter.getParameterValue(1));
        // System.err.println("Trait value" + traitParameter.getParameterValue(2));
        // System.err.println("Trait value" + traitParameter.getParameterValue(3));

        // System.err.println("branch length: " + treeModel.getBranchLength(node));
        // System.err.println("rate: " + branchRateModel.getBranchRate(treeModel,node));
        return length;
    }

    class CacheHelper {

        public CacheHelper(int cacheLength, boolean cacheBranches) {
            // modify code later so we can uncomment the following

            meanCache = new double[cacheLength];
            this.cacheBranches = cacheBranches;
            if (cacheBranches) {
                storedMeanCache = new double[cacheLength];
            }

        }

        public double[] getMeanCache() {
            return meanCache;
        }

        public double[] getCorrectedMeanCache() {
            return meanCache;
        }

        public void store() {
            // if (cacheBranches) {
            System.arraycopy(meanCache, 0, storedMeanCache, 0, meanCache.length);
            // }
        }

        public void restore() {
            //  if (cacheBranches) {
            double[] tmp = storedMeanCache;
            storedMeanCache = meanCache;
            meanCache = tmp;
            // }
        }

        public double getOUFactor(Node node) {
            return 1;
        }

        public double getUpperPrecFactor(Node node) {
            return 1.0 / getRescaledBranchLengthForPrecision(node);
        }

        protected boolean cacheBranches;
        //  private double[] meanCache;
        //  private double[] storedMeanCache;

        public void computeMeanCaches(int meanThisOffset, int meanOffset0, int meanOffset1,
                                      double precision0, double precision1, MissingTraits missingTraits) {
            //To change body of created methods use File | Settings | File Templates.
        }

        public void computeMeanCaches(int meanThisOffset, int meanOffset0, int meanOffset1,
                                      double totalPrecision, double precision0, double precision1, MissingTraits missingTraits,
                                      Node thisNode, Node node0, Node node1) {
            if (totalPrecision == 0) {
                System.arraycopy(zeroDimVector, 0, meanCache, meanThisOffset, dim);
            } else {
                // Delegate in case either child is partially missing
                // computeCorrectedWeightedAverage
                missingTraits.computeWeightedAverage(meanCache,
                        meanOffset0, precision0,
                        meanOffset1, precision1,
                        meanThisOffset, dim);
            }
        }

        // public void setTipMeans(double[] meanCache, double[] traitValue, int dim, int index, Node node) {
        public void setTipMeans(double[] traitValue, int dim, int index, Node node) {
            System.arraycopy(traitValue, 0, meanCache, dim * index, dim);
        }

        public void setTipMeans(double[] traitValue, int dim, int index) {
            System.arraycopy(traitValue, 0, meanCache, dim * index, dim);
        }

        public void copyToMeanCache(double[] src, int destPos, int length) {
            System.arraycopy(src, 0, meanCache, destPos, length);
        }

        public void setMeanCache(int index, double value) {
            meanCache[index] = value;
        }


    }

    ;


    class DriftCacheHelper extends CacheHelper {

        public DriftCacheHelper(int cacheLength, boolean cacheBranches) {
            super(cacheLength, cacheBranches);
            correctedMeanCache = new double[cacheLength];
        }

        public double[] getCorrectedMeanCache() {
            return correctedMeanCache;
        }


        public double getOUFactor(Node node) {
            return 1;
        }

        public double getUpperPrecFactor(Node node) {
            return 1.0 / getRescaledBranchLengthForPrecision(node);
        }

        public void setTipMeans(double[] traitValue, int dim, int index, Node node) {
            System.arraycopy(traitValue, 0, meanCache, dim * index, dim);
            /*
            double[] shift = getShiftForBranchLength(node);
            for (int i = 0; i < dim; i++) {
                correctedMeanCache[dim * index + i] = traitValue[i] - shift[i];
            }
            */
        }


        public void computeMeanCaches(int meanThisOffset, int meanOffset0, int meanOffset1,
                                      double totalPrecision, double precision0, double precision1, MissingTraits missingTraits,
                                      Node thisNode, Node node0, Node node1) {
            if (totalPrecision == 0) {
                System.arraycopy(zeroDimVector, 0, meanCache, meanThisOffset, dim);
            } else {
                // Delegate in case either child is partially missing
                // computeCorrectedWeightedAverage
                computeCorrectedWeightedAverage(
                        meanOffset0, precision0, node0,
                        meanOffset1, precision1, node1,
                        meanThisOffset, dim, thisNode);
            }
        }


        // private double[] correctedMeanCache;
    }

    public double getTimeScaledSelection(Node node) {
        if (strengthOfSelection != null) {
            double selection;
            double realTimeBranchLength = node.getLength();
            selection = strengthOfSelection.get().getRateForBranch(node) * realTimeBranchLength;
            return selection;
        } else {
            throw new RuntimeException("getTimeScaledSelection should not be called.");
        }
    }

    class OUCacheHelper extends CacheHelper {

        public OUCacheHelper(int cacheLength, boolean cacheBranches) {
            super(cacheLength, cacheBranches);
            correctedMeanCache = new double[cacheLength];
        }

        public double[] getCorrectedMeanCache() {
            return correctedMeanCache;
        }

        public double getOUFactor(Node node) {
            // return 1 - getTimeScaledSelection(node);
            return Math.exp(-getTimeScaledSelection(node));
        }

        public double getUpperPrecFactor(Node node) {
            return (2 * strengthOfSelection.get().getRateForBranch(node) / (1 - Math.exp(-2 * getTimeScaledSelection(node))));
        }


        public void setTipMeans(double[] traitValue, int dim, int index, Node node) {
            System.arraycopy(traitValue, 0, meanCache, dim * index, dim);
        }


        public void computeMeanCaches(int meanThisOffset, int meanOffset0, int meanOffset1,
                                      double totalPrecision, double precision0, double precision1, MissingTraits missingTraits,
                                      Node thisNode, Node node0, Node node1) {
            if (totalPrecision == 0) {
                System.arraycopy(zeroDimVector, 0, meanCache, meanThisOffset, dim);
            } else {
                // Delegate in case either child is partially missing
                // computeCorrectedWeightedAverage
                computeCorrectedOUWeightedAverage(
                        meanOffset0, precision0, node0,
                        meanOffset1, precision1, node1,
                        meanThisOffset, dim, thisNode);
            }
        }

    }


    // protected boolean hasDrift;

    protected double[] upperPrecisionCache;
    protected double[] lowerPrecisionCache;
    private double[] logRemainderDensityCache;

    private double[] storedMeanCache;
    private double[] storedUpperPrecisionCache;
    private double[] storedLowerPrecisionCache;
    private double[] storedLogRemainderDensityCache;

    private double[] drawnStates;

    protected final boolean integrateRoot = true; // Set to false if conditioning on root value (not fully implemented)
    //  protected final boolean integrateRoot = false;
    protected static boolean DEBUG = false;
    protected static boolean DEBUG_PREORDER = false;
    protected static boolean DEBUG_PNAS = false;

    private double[] zeroDimVector;

    protected WishartSufficientStatistics wishartStatistics;

    // Reusable temporary storage
    protected double[] Ay;
    protected double[][] tmpM;
    protected double[] tmp2;

    protected MissingTraits missingTraits;

}
