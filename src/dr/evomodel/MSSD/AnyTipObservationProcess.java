package dr.evomodel.MSSD;


import beast.core.Description;
import beast.core.parameter.RealParameter;
import beast.evolution.alignment.Alignment;
import beast.evolution.branchratemodel.BranchRateModel;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;


//import dr.evolution.alignment.PatternList;
//import dr.evolution.tree.NodeRef;
//import dr.evolution.tree.Tree;
//import dr.evomodel.branchratemodel.BranchRateModel;
//import dr.evomodel.sitemodel.SiteModel;
//import dr.evomodel.tree.TreeModel;
//import dr.inference.model.Parameter;

/**
 * Package: AnyTipObservationProcess
 * Description:
 * <p/>
 * <p/>
 * Created by
 * Alexander V. Alekseyenko (alexander.alekseyenko@gmail.com)
 * Date: Mar 18, 2008
 * Time: 6:45:00 PM
 */
@Description("Class ported from BEAST1")
public class AnyTipObservationProcess extends AbstractObservationProcess {
    protected double[] u0;
    protected double[] p;

    public AnyTipObservationProcess(String modelName, Tree treeModel, Alignment patterns, SiteModel siteModel,
                                    BranchRateModel branchRateModel, RealParameter mu, RealParameter lam, boolean integrateGainRate) {
        super(modelName, treeModel, patterns, siteModel, branchRateModel, mu, lam, integrateGainRate);
    }

    public double calculateLogTreeWeight() {
        int L = treeModel.getNodeCount();
        if (u0 == null || p == null) {
            u0 = new double[L];    // probability that the trait at node i survives to no leaf
            p = new double[L];     // probability of survival on the branch ancestral to i
        }
        int i, j, childNumber;
        Node node;
        double logWeight = 0.0;

        double averageRate = getAverageRate();

        for (i = 0; i < L; ++i) {
            p[i] = 1.0 - getNodeSurvivalProbability(i, averageRate);
        }

        /*Tree.Utils.*/postOrderTraversalList(treeModel, postOrderNodeList);

        for (int postOrderIndex = 0; postOrderIndex < nodeCount; postOrderIndex++) {

            i = postOrderNodeList[postOrderIndex];

            if (i < treeModel.getLeafNodeCount()) { // Is tip
                u0[i] = 0.0;
                logWeight += 1.0 - p[i];
            } else { // Is internal node or root
                u0[i] = 1.0;
                node = treeModel.getNode(i);
                for (j = 0; j < node.getChildCount(); ++j) {                   
                    childNumber = node.getChild(j).getNr();
                    u0[i] *= 1.0 - p[childNumber] * (1.0 - u0[childNumber]);
                }
                logWeight += (1.0 - u0[i]) * (1.0 - p[i]);
            }
        }

        return -logWeight * lam.getValue(0) / (getAverageRate() * mu.getValue(0));
    }


    private void postOrderTraversalList(Tree tree, int[] postOrderList) {
        int idx = nodeCount - 1;
        int cidx = nodeCount - 1;

        postOrderList[idx] = tree.getRoot().getNr();

        while (cidx > 0) {
            Node cNode = tree.getNode(postOrderList[idx]);
            for(int i = 0; i < cNode.getChildCount(); ++i) {
                cidx -= 1;
                postOrderList[cidx] = cNode.getChild(i).getNr();
            }
            idx -= 1;
        }
	}

	public void setTipNodePatternInclusion() { // These values never change
        for (int i = 0; i < treeModel.getLeafNodeCount(); i++) {
            Node node = treeModel.getNode(i);

            for (int patternIndex = 0; patternIndex < patternCount; patternIndex++) {
                extantInTipsBelow[i * patternCount + patternIndex] = 1;
                int taxonIndex = patterns.getTaxonIndex(node.getID());
                int patternItem = patterns.getPattern(taxonIndex, patternIndex);
                int[] states = dataType.getStatesForCode(patternItem);
                for (int state : states) {
                    if (state == deathState) {
                        extantInTipsBelow[i * patternCount + patternIndex] = 0;
                    }
                }
                extantInTips[patternIndex] += extantInTipsBelow[i * patternCount + patternIndex];

            }
        }

        for (int i = 0; i < treeModel.getNodeCount(); i++) {
            for (int patternIndex = 0; patternIndex < patternCount; patternIndex++) {
                nodePatternInclusion[i * patternCount + patternIndex] =
                        (extantInTipsBelow[i * patternCount +patternIndex] >= extantInTips[patternIndex]);
            }
        }
    }

    public void setNodePatternInclusion() {

        if (postOrderNodeList == null) {
            postOrderNodeList = new int[nodeCount];         
        }
        
        if (nodePatternInclusion == null) {
            nodePatternInclusion = new boolean[nodeCount * patternCount];
            storedNodePatternInclusion = new boolean[nodeCount * patternCount];
        }

        if (extantInTips == null) {
            extantInTips = new int[patternCount];
            extantInTipsBelow = new int[nodeCount * patternCount];
            setTipNodePatternInclusion();
        }

        // Determine post-order traversal
        /*Tree.Utils.*/postOrderTraversalList(treeModel, postOrderNodeList);

        // Do post-order traversal
        // Do post-order traversal
        for (int postOrderIndex = 0; postOrderIndex < nodeCount; postOrderIndex++) {
            Node node = treeModel.getNode(postOrderNodeList[postOrderIndex]);
            final int nChildren = node.getChildCount();
            if (nChildren > 0) {
                final int nodeNumber = node.getNr();
                for (int patternIndex = 0; patternIndex < patternCount; patternIndex++) {
                    extantInTipsBelow[nodeNumber * patternCount + patternIndex] = 0;
                    for (int j = 0; j < nChildren; j++) {
                        final int childIndex = node.getChild(j).getNr();
                        extantInTipsBelow[nodeNumber * patternCount + patternIndex] +=
                                extantInTipsBelow[childIndex * patternCount + patternIndex];
                    }
                }
            }
        }

        for (int i = treeModel.getLeafNodeCount(); i < treeModel.getNodeCount(); ++i) {
            for (int patternIndex = 0; patternIndex < patternCount; patternIndex++) {
                nodePatternInclusion[i * patternCount + patternIndex] =
                        (extantInTipsBelow[i * patternCount + patternIndex] >= extantInTips[patternIndex]);
            }
        }
        
        nodePatternInclusionKnown = true;
    }

    private int[] extantInTips;
    private int[] extantInTipsBelow; // Easier to store/restore (later) if 1D array

    private int[] postOrderNodeList;

}
