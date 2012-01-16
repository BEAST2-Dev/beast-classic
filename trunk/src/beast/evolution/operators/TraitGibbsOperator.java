/*
 * PrecisionMatrixGibbsOperator.java
 *
 * Copyright (C) 2002-2007 Alexei Drummond and Andrew Rambaut
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

//import dr.evomodel.continuous.AbstractMultivariateTraitLikelihood;
//import dr.evomodel.continuous.SampledMultivariateTraitLikelihood;
//import dr.evomodel.tree.TreeModel;
//import dr.geo.GeoSpatialDistribution;
//import dr.geo.GeoSpatialCollectionModel;
//import dr.inference.distribution.MultivariateDistributionLikelihood;
import dr.geo.GeoSpatialCollectionModel;
import dr.geo.GeoSpatialDistribution;
import dr.math.MathUtils;
import dr.math.matrixAlgebra.SymmetricMatrix;
import dr.math.distributions.MultivariateDistribution;
import dr.math.distributions.MultivariateNormalDistribution;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import beast.continuous.AbstractMultivariateTraitLikelihood;
import beast.continuous.SampledMultivariateTraitLikelihood;
import beast.core.Input;
import beast.core.Operator;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Taxon;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTraitMap;

/**
 * @author Marc Suchard
 */
public class TraitGibbsOperator extends Operator {
	public Input<Tree> treeInput = new Input<Tree>("tree", "", Validate.REQUIRED);
	public Input<RealParameter> precisionParamInput = new Input<RealParameter>("parameter",
			"parameter representing precision matrix", Validate.REQUIRED);
    public Input<TreeTraitMap> mapInput = new Input<TreeTraitMap>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);
	public Input<SampledMultivariateTraitLikelihood> traitModelInput = new Input<SampledMultivariateTraitLikelihood>("likelihood","", Validate.REQUIRED);
	public Input<MultivariateNormalDistribution> rootPrior = new Input<MultivariateNormalDistribution>("rootprior", "", Validate.REQUIRED);
	public Input<Boolean> onlyInternalNodesInput = new Input<Boolean>("onlyInternalNodesInput", "" ,true);
	public Input<Boolean> onlyTipsWithPriorsInput = new Input<Boolean>("onlyTipsWithPriorsInput", "" ,true);
	
	public static final String GIBBS_OPERATOR = "traitGibbsOperator";
	public static final String INTERNAL_ONLY = "onlyInternalNodes";
	public static final String TIP_WITH_PRIORS_ONLY = "onlyTipsWithPriors";
	public static final String NODE_PRIOR = "nodePrior";
	public static final String NODE_LABEL = "taxon";
	public static final String ROOT_PRIOR = "rootPrior";

	private Tree treeModel;
	private RealParameter precisionMatrixParameter;
	private SampledMultivariateTraitLikelihood traitModel;
	private int dim;
	private String traitName;

	private Map<String, GeoSpatialDistribution> nodeGeoSpatialPrior;
	private Map<String, MultivariateNormalDistribution> nodeMVNPrior;
	private GeoSpatialCollectionModel parameterPrior = null;

	private boolean onlyInternalNodes = true;
	private boolean onlyTipsWithPriors = true;
	private boolean sampleRoot = false;
	private double[] rootPriorMean;
	private double[][] rootPriorPrecision;
	private final int maxTries = 10000;

	TreeTraitMap traitMap;
	
	@Override
	public void initAndValidate() throws Exception {
    	traitMap = mapInput.get();

		this.traitModel = traitModelInput.get();
		this.treeModel = treeInput.get();
		this.precisionMatrixParameter = precisionParamInput.get();
		this.traitName = traitModel.getTraitName();
		setRootPrior(rootPrior.get());
		
		this.onlyInternalNodes = onlyInternalNodesInput.get();
		this.onlyTipsWithPriors = onlyTipsWithPriorsInput.get();
		this.dim = precisionMatrixParameter.getMinorDimension1();
	}

	public void setRootPrior(MultivariateNormalDistribution rootPrior) {
		rootPriorMean = rootPrior.getMean();
		rootPriorPrecision = rootPrior.getScaleMatrix();
		sampleRoot = true;
	}

	public void setTaxonPrior(String taxonID, MultivariateDistribution distribution) {

		if (distribution instanceof GeoSpatialDistribution) {
			if (nodeGeoSpatialPrior == null) {
				nodeGeoSpatialPrior = new HashMap<String, GeoSpatialDistribution>();
			}
			nodeGeoSpatialPrior.put(taxonID, (GeoSpatialDistribution) distribution);

		} else if (distribution instanceof MultivariateNormalDistribution) {
			if (nodeMVNPrior == null) {
				nodeMVNPrior = new HashMap<String, MultivariateNormalDistribution>();
			}
			nodeMVNPrior.put(taxonID, (MultivariateNormalDistribution) distribution);
		} else {
			throw new RuntimeException("Only flat/truncated geospatial and multivariate normal distributions allowed");
		}
	}

	public void setParameterPrior(GeoSpatialCollectionModel distribution) {
		parameterPrior = distribution;
	}

	public int getStepCount() {
		return 1;
	}

	private boolean nodeGeoSpatialPriorExists(Node node) {
		return nodeGeoSpatialPrior != null && nodeGeoSpatialPrior.containsKey(node.getID());
	}

	private boolean nodeMVNPriorExists(Node node) {
		return nodeMVNPrior != null && nodeMVNPrior.containsKey(node.getID());
	}

	@Override
	public double proposal() {
		try {
			Node node = null;
			final Node root = treeModel.getRoot();

			while (node == null) {
				if (onlyInternalNodes)
					node = treeModel.getNode(treeModel.getLeafNodeCount() + MathUtils.nextInt(treeModel.getInternalNodeCount()));
				else {
					node = treeModel.getNode(MathUtils.nextInt(treeModel.getNodeCount()));
					if (onlyTipsWithPriors && (node.getChildCount() == 0) && // Is
																						// a
																						// tip
							!nodeGeoSpatialPriorExists(node)) { // Does not have
																// a prior
						node = null;
					}
				}
				if (!sampleRoot && node == root)
					node = null;
			} // select any internal (or internal/external) node

			final double[] initialValue = traitMap.getTrait(treeModel, node);

			MeanPrecision mp;

			if (node != root)
				mp = operateNotRoot(node);
			else
				mp = operateRoot(node);

			final String taxon = node.getID();

			// final boolean nodePriorExists = nodeGeoSpatialPrior != null &&
			// nodeGeoSpatialPrior.containsKey(taxon);
			final boolean nodePriorExists = nodeGeoSpatialPriorExists(node);

			// if (!onlyInternalNodes) {
			// final boolean isTip = (treeModel.getChildCount(node) == 0);
			// if (!nodePriorExists && isTip)
			// System.err.println("Warning: sampling taxon '"+treeModel.getNodeTaxon(node).getId()
			// +"' tip trait without a prior!!!");
			// }

			int count = 0;

			final boolean parameterPriorExists = parameterPrior != null;

			double[] draw;

			do {
				do {
					if (count > maxTries) {
						traitMap.setTrait(node, initialValue);
						throw new Exception("Truncated Gibbs is stuck!");
					}

					draw = MultivariateNormalDistribution.nextMultivariateNormalPrecision(mp.mean, mp.precision);
					count++;

				} while (nodePriorExists && // There is a prior for this node
						(nodeGeoSpatialPrior.get(taxon)).logPdf(draw) == Double.NEGATIVE_INFINITY); // And
																									// draw
																									// is
																									// invalid
																									// under
																									// prior
				// TODO Currently only works for flat/truncated priors, make
				// work for MVN

				traitMap.setTrait(node, draw);

			} while (parameterPriorExists && (parameterPrior.getLogLikelihood() == Double.NEGATIVE_INFINITY));
		} catch (Exception e) {
			return Double.NEGATIVE_INFINITY;
		}
		return Double.POSITIVE_INFINITY;
	}

	private MeanPrecision operateNotRoot(Node node) {

		double[][] precision = getParameterAsMatrix();

		Node parent = node.getParent();

		double[] mean = new double[dim];

		double weight = 1.0 / traitModel.getRescaledBranchLength(node);

		double[] trait = traitMap.getTrait(treeModel, parent);

		for (int i = 0; i < dim; i++)
			mean[i] = trait[i] * weight;

		double weightTotal = weight;
		for (int j = 0; j < node.getChildCount(); j++) {
			Node child = node.getChild(j);
			trait = traitMap.getTrait(treeModel, child);
			weight = 1.0 / traitModel.getRescaledBranchLength(child);

			for (int i = 0; i < dim; i++)
				mean[i] += trait[i] * weight;

			weightTotal += weight;
		}

		for (int i = 0; i < dim; i++) {
			mean[i] /= weightTotal;
			for (int j = i; j < dim; j++)
				precision[j][i] = precision[i][j] *= weightTotal;
		}

		if (nodeMVNPriorExists(node)) {
			throw new RuntimeException("Still trying to implement multivariate normal taxon priors");
		}

		return new MeanPrecision(mean, precision);
	}

	class MeanPrecision {
		final double[] mean;
		final double[][] precision;

		MeanPrecision(double[] mean, double[][] precision) {
			this.mean = mean;
			this.precision = precision;
		}
	}

	private MeanPrecision operateRoot(Node node) {

		double[] trait;
		double weightTotal = 0.0;

		double[] weightedAverage = new double[dim];

		double[][] precision = getParameterAsMatrix();

		for (int k = 0; k < node.getChildCount(); k++) {
			Node child = node.getChild(k);
			trait = traitMap.getTrait(treeModel, child);
			final double weight = 1.0 / traitModel.getRescaledBranchLength(child);

			for (int i = 0; i < dim; i++) {
				for (int j = 0; j < dim; j++)
					weightedAverage[i] += precision[i][j] * weight * trait[j];
			}

			weightTotal += weight;
		}

		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++) {
				weightedAverage[i] += rootPriorPrecision[i][j] * rootPriorMean[j];
				precision[i][j] = precision[i][j] * weightTotal + rootPriorPrecision[i][j];
			}
		}

		double[][] variance = new SymmetricMatrix(precision).inverse().toComponents();

		trait = new double[dim];
		for (int i = 0; i < dim; i++) {
			for (int j = 0; j < dim; j++)
				trait[i] += variance[i][j] * weightedAverage[j];
		}

		return new MeanPrecision(trait, precision);
	}

	public String getPerformanceSuggestion() {
		return null;
	}

	public String getOperatorName() {
		return GIBBS_OPERATOR;
	}

    public double[][] getParameterAsMatrix() {
        final int I = dim;
        final int J = dim;
        double[][] parameterAsMatrix = new double[I][J];
        for (int i = 0; i < I; i++) {
            for (int j = 0; j < J; j++)
                parameterAsMatrix[i][j] = precisionMatrixParameter.getValue(i * J + j);
        }
        return parameterAsMatrix;
    }
}