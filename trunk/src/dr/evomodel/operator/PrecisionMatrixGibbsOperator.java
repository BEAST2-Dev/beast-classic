/*
 * PrecisionMatrixGibbsOperator.java
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

package dr.evomodel.operator;


import beast.continuous.AbstractMultivariateTraitLikelihood;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTraitMap;
//import dr.evomodel.continuous.AbstractMultivariateTraitLikelihood;
//import dr.evomodel.continuous.SampledMultivariateTraitLikelihood;
//import dr.inference.distribution.MultivariateDistributionLikelihood;
import dr.math.distributions.WishartDistribution;
//import dr.math.distributions.WishartSufficientStatistics;
//import dr.math.interfaces.ConjugateWishartStatisticsProvider;
import dr.math.matrixAlgebra.IllegalDimension;
import dr.math.matrixAlgebra.SymmetricMatrix;

/**
 * @author Marc Suchard
 */
public class PrecisionMatrixGibbsOperator extends Operator {

	public Input<Tree> treeInput = new Input<Tree>("tree", "", Validate.REQUIRED);
	public Input<RealParameter> precisionParamInput = new Input<RealParameter>("parameter","parameter representing precision matrix", Validate.REQUIRED);
    public Input<TreeTraitMap> mapInput = new Input<TreeTraitMap>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);

	public Input<AbstractMultivariateTraitLikelihood> traitModelInput = new Input<AbstractMultivariateTraitLikelihood>("likelihood","", Validate.REQUIRED);
	public Input<WishartDistribution> priorDistributionInput = new Input<WishartDistribution>("prior","must be a Wishart Distribution", Validate.REQUIRED);

	
    public static final String VARIANCE_OPERATOR = "precisionGibbsOperator";
//    public static final String PRECISION_MATRIX = "precisionMatrix";
    public static final String TREE_MODEL = "treeModel";
//    public static final String OUTCOME = "outcome";
    public static final String MEAN = "mean";
    public static final String PRIOR = "prior";
//    public static final String TRAIT_MODEL = "traitModel";

    private AbstractMultivariateTraitLikelihood traitModel;
    WishartDistribution priorDistribution;
    private RealParameter precisionParam;
    //    private WishartDistribution priorDistribution;
    private double priorDf;
    private SymmetricMatrix priorInverseScaleMatrix;
    private Tree treeModel;
    private int dim;
    private double numberObservations;
    private String traitName;
    // only implemented for SampledTraitLikelihood
//    private final boolean isSampledTraitLikelihood;
    
    TreeTraitMap traitMap;

    @Override
    public void initAndValidate() throws Exception {
    	traitMap = mapInput.get();
    	treeModel = treeInput.get();
    	precisionParam = precisionParamInput.get();
    	
        this.traitModel = traitModel;
        priorDistribution = priorDistributionInput.get();
//        this.priorDistribution = priorDistribution;
        this.priorDf = priorDistribution.df();
        this.priorInverseScaleMatrix = null;
        if (priorDistribution.scaleMatrix() != null)
            this.priorInverseScaleMatrix =
                    (SymmetricMatrix) (new SymmetricMatrix(priorDistribution.scaleMatrix())).inverse();
        this.treeModel = traitModel.getTreeModel();
        traitName = traitModel.getTraitName();
        dim = precisionParam.getMinorDimension1();//getRowDimension(); // assumed to be square

//        isSampledTraitLikelihood = (traitModel instanceof SampledMultivariateTraitLikelihood);

//        if (!isSampledTraitLikelihood &&
//                !(traitModel instanceof ConjugateWishartStatisticsProvider)) {
//            throw new RuntimeException("Only implemented for a SampledMultivariateTraitLikelihood and " +
//                    "ConjugateWishartStatisticsProvider");
//        }
    }

    public int getStepCount() {
        return 1;
    }

//    private void incrementScaledSquareMatrix(double[][] out, double[][] in, double scalar, int dim) {
//        for (int i = 0; i < dim; i++) {
//            for (int j = 0; j < dim; j++) {
//                out[i][j] += scalar * in[i][j];
//            }
//        }
//    }

//    private void zeroSquareMatrix(double[][] out, int dim) {
//        for (int i = 0; i < dim; i++) {
//            for (int j = 0; j < dim; j++) {
//                out[i][j] = 0.0;
//            }
//        }
//    }

//    private void incrementOuterProduct(double[][] S,
//                                       ConjugateWishartStatisticsProvider integratedLikelihood) {
//
//
//        final WishartSufficientStatistics sufficientStatistics = integratedLikelihood.getWishartStatistics();
//        final double[][] outerProducts = sufficientStatistics.getScaleMatrix();
//
//        final double df = sufficientStatistics.getDf();
////        final double df = 2;
//
////        final double df = integratedLikelihood.getTotalTreePrecision();
//
////        System.err.println("OuterProducts = \n" + new Matrix(outerProducts));
////        System.err.println("Total tree DF  = " + df);
////        System.exit(-1);
//
//        for (int i = 0; i < outerProducts.length; i++) {
//            System.arraycopy(outerProducts[i], 0, S[i], 0, S[i].length);
//        }
//        numberObservations = df;
//    }

    private void incrementOuterProduct(double[][] S, Node node) {

        if (!node.isRoot()) {

            Node parent = node.getParent();
            double[] parentTrait = traitMap.getTrait(treeModel, parent); 
            double[] childTrait = traitMap.getTrait(treeModel, node);
            double time = traitModel.getRescaledBranchLength(node);

            if (time > 0) {

                double sqrtTime = Math.sqrt(time);

                double[] delta = new double[dim];

                for (int i = 0; i < dim; i++)
                    delta[i] = (childTrait[i] - parentTrait[i]) / sqrtTime;

                for (int i = 0; i < dim; i++) {            // symmetric matrix,
                    for (int j = i; j < dim; j++)
                        S[j][i] = S[i][j] += delta[i] * delta[j];
                }
                numberObservations += 1; // This assumes a *single* observation per tip
            }
        }
        // recurse down tree
        if (!node.isLeaf()) {
	        incrementOuterProduct(S, node.m_left);
	        incrementOuterProduct(S, node.m_right);
        }
    }

    public double[][] getOperationScaleMatrixAndSetObservationCount() {

        // calculate sum-of-the-weighted-squares matrix over tree
        double[][] S = new double[dim][dim];
        SymmetricMatrix S2;
        SymmetricMatrix inverseS2 = null;
        numberObservations = 0; // Need to reset, as incrementOuterProduct can be recursive

//        if (isSampledTraitLikelihood) {
            incrementOuterProduct(S, treeModel.getRoot());
//        } else { // IntegratedTraitLikelihood
//            incrementOuterProduct(S, (ConjugateWishartStatisticsProvider) traitModel);
//        }

        try {
            S2 = new SymmetricMatrix(S);
            if (priorInverseScaleMatrix != null)
                S2 = priorInverseScaleMatrix.add(S2);
            inverseS2 = (SymmetricMatrix) S2.inverse();

        } catch (IllegalDimension illegalDimension) {
            illegalDimension.printStackTrace();
        }

        assert inverseS2 != null;

        return inverseS2.toComponents();
    }

    @Override
    public double proposal() {

    	try{ 
	        final double[][] scaleMatrix = getOperationScaleMatrixAndSetObservationCount();
	        final double treeDf = numberObservations;
	        final double df = priorDf + treeDf;
	
	        double[][] draw = WishartDistribution.nextWishart(df, scaleMatrix);
	
	        for (int i = 0; i < dim; i++) {
	            for (int j = 0; j < dim; j++)
	                precisionParam.setValue(i*dim + j, draw[j][i]);
	        }
    	} catch (Exception e) {
			return Double.NEGATIVE_INFINITY;
		}

        return Double.POSITIVE_INFINITY;
    }

    public String getPerformanceSuggestion() {
        return null;
    }

    public String getOperatorName() {
        return VARIANCE_OPERATOR;
    }



}
