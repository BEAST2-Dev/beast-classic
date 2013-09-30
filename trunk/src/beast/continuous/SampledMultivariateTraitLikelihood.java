package beast.continuous;

//import dr.evolution.tree.NodeRef;
//import dr.evolution.tree.Tree;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.tree.TreeModel;
//import dr.inference.model.CompoundParameter;
//import dr.inference.model.CompoundSymmetricMatrix;
//import dr.inference.model.Model;


import java.util.List;
import java.util.Random;

import beast.core.Description;
import beast.core.Input;
import beast.core.State;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;
import beast.math.matrixalgebra.Vector;



/**
 * @author Marc A. Suchard
 */
@Description("SampledMultivariateTraitLikelihood ported from BEAST1")
public class SampledMultivariateTraitLikelihood extends AbstractMultivariateTraitLikelihood {

//    public SampledMultivariateTraitLikelihood(String traitName,
//                                              TreeModel treeModel,
//                                              MultivariateDiffusionModel diffusionModel,
//                                              CompoundParameter traitParameter,
//                                              List<Integer> missingIndices,
//                                              boolean cacheBranches, boolean scaleByTime, boolean useTreeLength,
//                                              BranchRateModel rateModel, Model samplingDensity,
//                                              boolean reportAsMultivariate,
//                                              boolean reciprocalRates) {
//        super(traitName, treeModel, diffusionModel, traitParameter, missingIndices, cacheBranches, scaleByTime,
//                useTreeLength, rateModel, samplingDensity, reportAsMultivariate, reciprocalRates);
//    }

	public Input<Boolean> initFromTree = new Input<Boolean>("initFromTree","initiliase initial state from tree", false);
	
	@Override
	public void initAndValidate() throws Exception {
		super.initAndValidate();
		if (initFromTree.get()) {
			Node [] nodes = treeModel.getNodesAsArray();
			Double [] trait = new Double[nodes.length * 2];
			int k = 0;
			for (Node node : nodes) {
				Object o = node.getMetaData("lat");
				trait[k++] = (Double) o;
				o =  node.getMetaData("long");
				trait[k++] = (Double) o;
			}
			RealParameter traitParameter = new RealParameter(trait);
			traitParameter.setBounds(this.traitParameter.getLower(), this.traitParameter.getUpper());
			this.traitParameter.assignFromWithoutID(traitParameter);
		}
	}
	
	
    protected String extraInfo() {
        return "\tSampling internal trait values: true\n";
    }

    /**
     * Calculate the log likelihood of the current state.
     *
     * @return the log likelihood.
     */
    @Override
    public double calculateLogLikelihood() {

        if (!cacheBranches)
            logP= traitLogLikelihood(null, treeModel.getRoot());
        else
        	logP = traitCachedLogLikelihood(null, treeModel.getRoot());
        if (logP > maxLogLikelihood) {
            maxLogLikelihood = logP;
        }
        return logP;
    }

    protected  double calculateAscertainmentCorrection(int taxonIndex) {
        throw new RuntimeException("Ascertainment correction not yet implemented for sampled trait likelihoods");
    }

    public final double getLogDataLikelihood() {
        double logLikelihood = 0;
        for (int i = 0; i < treeModel.getLeafNodeCount(); i++) {
            Node tip = treeModel.getNode(i); // TODO Do not include integrated tips; how to check???

            if (cacheBranches && validLogLikelihoods[tip.getNr()])
                logLikelihood += cachedLogLikelihoods[tip.getNr()];
            else {
                Node parent = tip.getParent();

                double[] tipTrait = traitMap.getTrait(treeModel, tip); 
                double[] parentTrait = traitMap.getTrait(treeModel, parent);
                double time = getRescaledBranchLength(tip);

                logLikelihood += diffusionModel.getLogLikelihood(parentTrait, tipTrait, time);
            }
        }
        return logLikelihood;
    }


    private double traitCachedLogLikelihood(double[] parentTrait, Node node) {

        double logL = 0.0;
        double[] childTrait = null;
        final int nodeNumber = node.getNr();

        if (!node.isRoot()) {

            if (!validLogLikelihoods[nodeNumber]) { // recompute

                childTrait = traitMap.getTrait(treeModel, node);
                double time = getRescaledBranchLength(node);
                if (parentTrait == null)
                    parentTrait = traitMap.getTrait(treeModel, node.getParent());
                logL = diffusionModel.getLogLikelihood(parentTrait, childTrait, time);
                cachedLogLikelihoods[nodeNumber] = logL;
                validLogLikelihoods[nodeNumber] = true;
            } else
                logL = cachedLogLikelihoods[nodeNumber];
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            logL += traitCachedLogLikelihood(childTrait, node.getChild(i));
        }

        return logL;
    }

    private double traitLogLikelihood(double[] parentTrait, Node node) {

        double logL = 0.0;
        double[] childTrait = traitMap.getTrait(treeModel, node);

        if (parentTrait != null) {

            double time = getRescaledBranchLength(node);
            logL = diffusionModel.getLogLikelihood(parentTrait, childTrait, time);
//            if (logL > 0) {
//                logL = diffusionModel.getLogLikelihood(parentTrait, childTrait, time);
//            }
            if (new Double(logL).isNaN()) {
                System.err.println("AbstractMultivariateTraitLikelihood: likelihood is undefined");
                System.err.println("time = " + time);
                System.err.println("parent trait value = " + new Vector(parentTrait));
                System.err.println("child trait value = " + new Vector(childTrait));

//                double[][] precisionMatrix = diffusionModel.getPrecisionmatrix();
//                if (precisionMatrix != null) {
//                    System.err.println("precision matrix = " + new Matrix(diffusionModel.getPrecisionmatrix()));
//                    if (diffusionModel.getPrecisionParameter() instanceof CompoundSymmetricMatrix) {
//                        CompoundSymmetricMatrix csMatrix = (CompoundSymmetricMatrix) diffusionModel.getPrecisionParameter();
//                        System.err.println("diagonals = " + new Vector(csMatrix.getDiagonals()));
//                        System.err.println("off diagonal = " + csMatrix.getOffDiagonal());
//                    }
//                }
            }
        }
        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            logL += traitLogLikelihood(childTrait, node.getChild(i));
        }

        if (new Double(logL).isNaN()) {
            System.err.println("logL = " + logL);
//            System.err.println(new Matrix(diffusionModel.getPrecisionmatrix()));
            System.exit(-1);
        }

        return logL;
    }

    @Override
    public double[] getTraitForNode(TreeInterface treeModel, Node node, String traitName) {
        return traitMap.getTrait(treeModel, node); 
    }

	@Override
	public List<String> getArguments() {return null;}
	@Override
	public List<String> getConditions() {return null;}
	@Override
	public void sample(State state, Random random) {}

}