package beast.evolution.likelihood;

import java.util.logging.Logger;


import beast.core.Input;
import beast.core.Input.Validate;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.UserDataType;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeTrait;
import beast.evolution.tree.TreeTraitProvider;
import beast.util.Randomizer;

/**
 * @author Marc A. Suchard
 */
public class AncestralStateTreeLikelihood extends TreeLikelihood implements TreeTraitProvider {
    public static final String STATES_KEY = "states";

    public Input<String> tagInput = new Input<String>("tag","label used to report trait", Validate.REQUIRED);
    public Input<Boolean> useMAPInput = new Input<Boolean>("useMAP","whether to use maximum aposteriori assignments or sample", false);
    public Input<Boolean> returnMLInput = new Input<Boolean>("returnML", "report integrate likelihood of tip data", true);

    /**
     * Constructor.
     * Now also takes a DataType so that ancestral states are printed using data codes
     *
     * @param patternList     -
     * @param treeModel       -
     * @param siteModel       -
     * @param branchRateModel -
     * @param useAmbiguities  -
     * @param storePartials   -
     * @param dataType        - need to provide the data-type, so that corrent data characters can be returned
     * @param tag             - string label for reconstruction characters in tree log
     * @param forceRescaling  -
     * @param useMAP          - perform maximum aposteriori reconstruction
     * @param returnML        - report integrate likelihood of tip data
     */
    int patternCount;
    int stateCount;
    @Override
    public void initAndValidate() throws Exception {
    	super.initAndValidate();
        this.tag = tag;
        Tree treeModel = m_tree.get();
        patternCount = m_data.get().getPatternCount();
        stateCount = dataType.getStateCount();

        reconstructedStates = new int[treeModel.getNodeCount()][patternCount];
        storedReconstructedStates = new int[treeModel.getNodeCount()][patternCount];

        this.useMAP = useMAPInput.get();
        this.returnMarginalLogLikelihood = returnMLInput.get();
      
        treeTraits.addTrait(STATES_KEY, new TreeTrait.IA() {
            public String getTraitName() {
                return tag;
            }

            public Intent getIntent() {
                return Intent.NODE;
            }

            public int[] getTrait(Tree tree, Node node) {
                return getStatesForNode(tree,node);
            }

            public String getTraitString(Tree tree, Node node) {
                return formattedState(getStatesForNode(tree,node), dataType);
            }
        });

        if (m_useAmbiguities.get()) {
            Logger.getLogger("dr.evomodel.treelikelihood").info("Ancestral reconstruction using ambiguities is currently "+
            "not support without BEAGLE");
            System.exit(-1);
        }
        if (m_siteModel.getCategoryCount() > 1)
            throw new RuntimeException("Reconstruction not implemented for multiple categories yet.");

    }

    @Override
    public void store() {
        super.store();

        for (int i = 0; i < reconstructedStates.length; i++) {
            System.arraycopy(reconstructedStates[i], 0, storedReconstructedStates[i], 0, reconstructedStates[i].length);
        }

        storedAreStatesRedrawn = areStatesRedrawn;
        storedJointLogLikelihood = jointLogLikelihood;
    }

    @Override
    public void restore() {

        super.restore();

        int[][] temp = reconstructedStates;
        reconstructedStates = storedReconstructedStates;
        storedReconstructedStates = temp;

        areStatesRedrawn = storedAreStatesRedrawn;
        jointLogLikelihood = storedJointLogLikelihood;
    }
    
    @Override
    protected boolean requiresRecalculation() {
    	likelihoodKnown = false;
    	return super.requiresRecalculation();
    }
//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//        super.handleModelChangedEvent(model, object, index);
//        fireModelChanged(model);
//    }
    
    

    public DataType getDataType() {
        return dataType;
    }

    public int[] getStatesForNode(Tree tree, Node node) {
        if (tree != m_tree.get()) {
            throw new RuntimeException("Can only reconstruct states on treeModel given to constructor");
        }

        if (!likelihoodKnown) {
        	try {
        		calculateLogP();
        	} catch (Exception e) {
				throw new RuntimeException(e.getMessage());
			}
            likelihoodKnown = true;
        }

        if (!areStatesRedrawn) {
            redrawAncestralStates();
        }
        return reconstructedStates[node.getNr()];
    }


    public void redrawAncestralStates() {
        jointLogLikelihood = 0;
        Tree tree = m_tree.get();
        traverseSample(tree, tree.getRoot(), null);
        areStatesRedrawn = true;
    }

//    private boolean checkConditioning = true;

    
    @Override
    public double calculateLogP() throws Exception {
        areStatesRedrawn = false;
        double marginalLogLikelihood = super.calculateLogP();
        if (returnMarginalLogLikelihood) {
            return logP;
        }
        // redraw states and return joint density of drawn states
        redrawAncestralStates();
        logP = jointLogLikelihood;
        return logP;
    }

    protected TreeTraitProvider.Helper treeTraits = new Helper();

    public TreeTrait[] getTreeTraits() {
        return treeTraits.getTreeTraits();
    }

    public TreeTrait getTreeTrait(String key) {
        return treeTraits.getTreeTrait(key);
    }


    private static String formattedState(int[] state, DataType dataType) {
        StringBuffer sb = new StringBuffer();
        sb.append("\"");
        if (dataType instanceof UserDataType) {
            boolean first = true;
            for (int i : state) {
                if (!first) {
                    sb.append(" ");
                } else {
                    first = false;
                }

                sb.append(dataType.getCode(i));
            }

        } else {
            for (int i : state) {
                sb.append(dataType.getChar(i));
            }
        }
        sb.append("\"");
        return sb.toString();
    }

    private int drawChoice(double[] measure) {
        if (useMAP) {
            double max = measure[0];
            int choice = 0;
            for (int i = 1; i < measure.length; i++) {
                if (measure[i] > max) {
                    max = measure[i];
                    choice = i;
                }
            }
            return choice;
        } else {
            return Randomizer.randomChoicePDF(measure);
        }
    }

    /**
     * Traverse (pre-order) the tree sampling the internal node states.
     *
     * @param tree        - TreeModel on which to perform sampling
     * @param node        - current node
     * @param parentState - character state of the parent node to 'node'
     */
    public void traverseSample(Tree tree, Node node, int[] parentState) {

        int nodeNum = node.getNr();

        Node parent = node.getParent();

        // This function assumes that all partial likelihoods have already been calculated
        // If the node is internal, then sample its state given the state of its parent (pre-order traversal).

        double[] conditionalProbabilities = new double[stateCount];
        int[] state = new int[patternCount];

        if (!node.isLeaf()) {

            if (parent == null) {

                double[] rootPartials = m_fRootPartials;

                // This is the root node
                for (int j = 0; j < patternCount; j++) {

                    System.arraycopy(rootPartials, j * stateCount, conditionalProbabilities, 0, stateCount);
                    double[] frequencies = m_substitutionModel.getFrequencies();
                    for (int i = 0; i < stateCount; i++) {
                        conditionalProbabilities[i] *= frequencies[i];
                    }
                    try {
                        state[j] = drawChoice(conditionalProbabilities);
                    } catch (Error e) {
                        System.err.println(e.toString());
                        System.err.println("Please report error to Marc");
                        state[j] = 0;
                    }
                    reconstructedStates[nodeNum][j] = state[j];

                    //System.out.println("Pr(j) = " + frequencies[state[j]]);
                    jointLogLikelihood += Math.log(frequencies[state[j]]);
                }

            } else {

                // This is an internal node, but not the root
                double[] partialLikelihood = new double[stateCount * patternCount];

                m_likelihoodCore.getNodePartials(nodeNum, partialLikelihood);

//				final double branchRate = branchRateModel.getBranchRate(tree, node);
//
//				            // Get the operational time of the branch
//				final double branchTime = branchRate * ( tree.getNodeHeight(parent) - tree.getNodeHeight(node) );
//
//				for (int i = 0; i < categoryCount; i++) {
//
//				                siteModel.getTransitionProbabilitiesForCategory(i, branchTime, probabilities);
//
//				}
//




            	if (m_beagle != null) {
                    /*((AbstractLikelihoodCore)*/ m_beagle.beagle.getTransitionMatrix(nodeNum, m_fProbabilities);
            	} else {
                    /*((AbstractLikelihoodCore)*/ m_likelihoodCore.getNodeMatrix(nodeNum, 0, m_fProbabilities);
            		
            	}


                for (int j = 0; j < patternCount; j++) {

                    int parentIndex = parentState[j] * stateCount;
                    int childIndex = j * stateCount;

                    for (int i = 0; i < stateCount; i++) {
                        conditionalProbabilities[i] = partialLikelihood[childIndex + i] * m_fProbabilities[parentIndex + i];
                    }

                    state[j] = drawChoice(conditionalProbabilities);
                    reconstructedStates[nodeNum][j] = state[j];

                    double contrib = m_fProbabilities[parentIndex + state[j]];
                    //System.out.println("Pr(" + parentState[j] + ", " + state[j] +  ") = " + contrib);
                    jointLogLikelihood += Math.log(contrib);
                }
            }

            // Traverse down the two child nodes
            Node child1 = node.getChild(0);
            traverseSample(tree, child1, state);

            Node child2 = node.getChild(1);
            traverseSample(tree, child2, state);
        } else {

            // This is an external leaf

        	if (m_beagle != null) {
                /*((AbstractLikelihoodCore)*/ m_beagle.beagle.getTipStates(nodeNum, reconstructedStates[nodeNum]);
        	} else {
            /*((AbstractLikelihoodCore)*/ m_likelihoodCore.getNodeStates(nodeNum, reconstructedStates[nodeNum]);
        	}

            // Check for ambiguity codes and sample them

            for (int j = 0; j < patternCount; j++) {

                final int thisState = reconstructedStates[nodeNum][j];
                final int parentIndex = parentState[j] * stateCount;
            	if (m_beagle != null) {
                    /*((AbstractLikelihoodCore) */m_beagle.beagle.getTransitionMatrix(nodeNum, m_fProbabilities);
            	} else {
                /*((AbstractLikelihoodCore) */m_likelihoodCore.getNodeMatrix(nodeNum, 0, m_fProbabilities);
            	}
                if (dataType.isAmbiguousState(thisState)) {

                    System.arraycopy(m_fProbabilities, parentIndex, conditionalProbabilities, 0, stateCount);
                    reconstructedStates[nodeNum][j] = drawChoice(conditionalProbabilities);
                }

                double contrib = m_fProbabilities[parentIndex + reconstructedStates[nodeNum][j]];
                //System.out.println("Pr(" + parentState[j] + ", " + reconstructedStates[nodeNum][j] +  ") = " + contrib);
                jointLogLikelihood += Math.log(contrib);
            }
        }
    }

    private DataType dataType;
    private int[][] reconstructedStates;
    private int[][] storedReconstructedStates;

    private String tag;
    private boolean areStatesRedrawn = false;
    private boolean storedAreStatesRedrawn = false;

    private boolean useMAP = false;
    private boolean returnMarginalLogLikelihood = true;

    private double jointLogLikelihood;
    private double storedJointLogLikelihood;

    boolean likelihoodKnown = false;
}
