package beast.evolution.likelihood;



import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import beagle.Beagle;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;
import beast.evolution.datatype.DataType;
import beast.evolution.datatype.UserDataType;
import beast.evolution.likelihood.TreeLikelihood;
import beast.evolution.sitemodel.SiteModel;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;
import beast.evolution.tree.Tree;
import beast.evolution.tree.TreeInterface;
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
    
    public Input<Boolean> useJava = new Input<Boolean>("useJava", "prefer java, even if beagle is available", true);
    
    
	public Input<List<LeafTrait>> leafTriatsInput = new Input<List<LeafTrait>>("leaftrait", "list of leaf traits",
			new ArrayList<LeafTrait>());

	int[][] storedTipStates;

	/** parameters for each of the leafs **/
	IntegerParameter[] parameters;

	/** and node number associated with parameter **/
	int[] leafNr;

	int traitDimension;

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

    int[][] tipStates; // used to store tip states when using beagle
    
    @Override
    public void initAndValidate() throws Exception {
    	if (dataInput.get().getSiteCount() == 0) {
    		return;
    	}
    	
    	
    	String sJavaOnly = null;
    	if (useJava.get()) {
    		sJavaOnly = System.getProperty("java.only");
    		System.setProperty("java.only", "" + true);
    	}
    	super.initAndValidate();
    	if (useJava.get()) {
	    	if (sJavaOnly != null) {
	    		System.setProperty("java.only", sJavaOnly);
	    	} else {
	    		System.clearProperty("java.only");
	    	}
    	}
    	
        this.tag = tagInput.get();
        TreeInterface treeModel = treeInput.get();
        patternCount = dataInput.get().getPatternCount();
        dataType = dataInput.get().getDataType();
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

            public int[] getTrait(TreeInterface tree, Node node) {
                return getStatesForNode(tree,node);
            }

            public String getTraitString(TreeInterface tree, Node node) {
                return formattedState(getStatesForNode(tree,node), dataType);
            }
        });

//        if (m_useAmbiguities.get()) {
//            Logger.getLogger("dr.evomodel.treelikelihood").info("Ancestral reconstruction using ambiguities is currently "+
//            "not support without BEAGLE");
//            System.exit(-1);
//        }
        if (beagle != null) {
            if (!(siteModelInput.get() instanceof SiteModel.Base)) {
            	throw new Exception ("siteModel input should be of type SiteModel.Base");
            }
            m_siteModel = (SiteModel.Base) siteModelInput.get();
        	substitutionModel = (SubstitutionModel.Base) m_siteModel.substModelInput.get();
            int nStateCount = dataInput.get().getMaxStateCount();
            probabilities = new double[(nStateCount + 1) * (nStateCount + 1)];
            
            int tipCount = treeModel.getLeafNodeCount();
            tipStates = new int[tipCount][];

            for (int k = 0; k < tipCount; k++) {
            	int[] states = new int[patternCount];
                for (int i = 0; i < patternCount; i++) {
                    states[i] = dataInput.get().getPattern(k, i);
                }
                tipStates[k] = states;
            }
        }
        
        if (m_siteModel.getCategoryCount() > 1)
            throw new RuntimeException("Reconstruction not implemented for multiple categories yet.");

        
        
        
        // stuff for dealing with ambiguities in tips
        if (!m_useAmbiguities.get()) {
        	return;
        }
		if (tipStates == null) {
            int tipCount = treeInput.get().getLeafNodeCount();
            tipStates = new int[tipCount][];

            for (int k = 0; k < tipCount; k++) {
            	int[] states = new int[patternCount];
                for (int i = 0; i < patternCount; i++) {
                    states[i] = dataInput.get().getPattern(k, i);
                }
                tipStates[k] = states;
            }
		}
		traitDimension = tipStates[0].length;

		leafNr = new int[leafTriatsInput.get().size()];
		parameters = new IntegerParameter[leafTriatsInput.get().size()];

		List<String> taxaNames = dataInput.get().getTaxaNames();
		for (int i = 0; i < leafNr.length; i++) {
			LeafTrait leafTrait = leafTriatsInput.get().get(i);
			parameters[i] = leafTrait.parameter.get();
			// sanity check
			if (parameters[i].getDimension() != traitDimension) {
				throw new Exception("Expected parameter dimension to be " + traitDimension + ", not "
						+ parameters[i].getDimension());
			}
			// identify node
			String taxon = leafTrait.taxonName.get();
			int k = 0;
			while (k < taxaNames.size() && !taxaNames.get(k).equals(taxon)) {
				k++;
			}
			leafNr[i] = k;
			// sanity check
			if (k == taxaNames.size()) {
				throw new Exception("Could not find taxon '" + taxon + "' in tree");
			}
			// initialise parameter value from states
			Integer[] values = new Integer[tipStates[k].length];
			for (int j = 0; j < tipStates[k].length; j++) {
				values[j] = tipStates[k][j];
			}
			IntegerParameter p = new IntegerParameter(values);
			p.setLower(0);
			p.setUpper(dataType.getStateCount()-1);
			parameters[i].assignFromWithoutID(p);
		}

		storedTipStates = new int[tipStates.length][traitDimension];
		for (int i = 0; i < tipStates.length; i++) {
			System.arraycopy(tipStates[i], 0, storedTipStates[i], 0, traitDimension);
		}

    }

    @Override
    public void store() {
        super.store();

        for (int i = 0; i < reconstructedStates.length; i++) {
            System.arraycopy(reconstructedStates[i], 0, storedReconstructedStates[i], 0, reconstructedStates[i].length);
        }

        storedAreStatesRedrawn = areStatesRedrawn;
        storedJointLogLikelihood = jointLogLikelihood;
        
        
        // deal with ambiguous tips
		for (int i = 0; i < leafNr.length; i++) {
			int k = leafNr[i];
			System.arraycopy(tipStates[k], 0, storedTipStates[k], 0, traitDimension);
		}
    }

    @Override
    public void restore() {

        super.restore();

        int[][] temp = reconstructedStates;
        reconstructedStates = storedReconstructedStates;
        storedReconstructedStates = temp;

        areStatesRedrawn = storedAreStatesRedrawn;
        jointLogLikelihood = storedJointLogLikelihood;
        
        // deal with ambiguous tips
		for (int i = 0; i < leafNr.length; i++) {
			int k = leafNr[i];
			int[] tmp = tipStates[k];
			tipStates[k] = storedTipStates[k];
			storedTipStates[k] = tmp;
			// Does not handle ambiguities or missing taxa
			likelihoodCore.setNodeStates(k, tipStates[k]);
		}

    }
    
    @Override
    protected boolean requiresRecalculation() {
    	likelihoodKnown = false;
    	boolean isDirty = super.requiresRecalculation();
    	if (!m_useAmbiguities.get()) {
    		return isDirty;
    	}
    	
    	
    	int hasDirt = Tree.IS_CLEAN;
		
		// check whether any of the leaf trait parameters changed
		for (int i = 0; i < leafNr.length; i++) {
			if (parameters[i].somethingIsDirty()) {
				int k = leafNr[i];
				for (int j = 0; j < traitDimension; j++) {
					tipStates[k][j] = parameters[i].getValue(j);
				}
				likelihoodCore.setNodeStates(k, tipStates[k]);
				isDirty = true;
				// mark leaf's parent node as dirty
				Node leaf = treeInput.get().getNode(k);
				// leaf.makeDirty(Tree.IS_DIRTY);
				leaf.getParent().makeDirty(Tree.IS_DIRTY);
	            hasDirt = Tree.IS_DIRTY;
			}
		}
		isDirty |= super.requiresRecalculation();
		this.hasDirt |= hasDirt;

		return isDirty;
    	
    	
    }
//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//        super.handleModelChangedEvent(model, object, index);
//        fireModelChanged(model);
//    }
    
    

    public DataType getDataType() {
        return dataType;
    }

    public int[] getStatesForNode(TreeInterface tree, Node node) {
        if (tree != treeInput.get()) {
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
        TreeInterface tree = treeInput.get();
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

    public void getStates(int tipNum, int[] states)  {
        // Saved locally to reduce BEAGLE library access
        System.arraycopy(tipStates[tipNum], 0, states, 0, states.length);
    }

	public void getPartials(int number, double[] partials) {
        int cumulativeBufferIndex = Beagle.NONE;
        /* No need to rescale partials */
        beagle.beagle.getPartials(beagle.partialBufferHelper.getOffsetIndex(number), cumulativeBufferIndex, partials);
	}

	public void getTransitionMatrix(int matrixNum, double[] probabilities) {
		beagle.beagle.getTransitionMatrix(beagle.matrixBufferHelper.getOffsetIndex(matrixNum), probabilities);
	}
    
    /**
     * Traverse (pre-order) the tree sampling the internal node states.
     *
     * @param tree        - TreeModel on which to perform sampling
     * @param node        - current node
     * @param parentState - character state of the parent node to 'node'
     */
    public void traverseSample(TreeInterface tree, Node node, int[] parentState) {

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
                	if (beagle != null) {
                		getPartials(node.getNr(), conditionalProbabilities);
                	} else {
                		System.arraycopy(rootPartials, j * stateCount, conditionalProbabilities, 0, stateCount);
                	}
                    double[] frequencies = substitutionModel.getFrequencies();
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

            	if (beagle != null) {
            		getPartials(node.getNr(), partialLikelihood);
            		getTransitionMatrix(nodeNum, probabilities);
            	} else {
                    likelihoodCore.getNodePartials(nodeNum, partialLikelihood);
                    /*((AbstractLikelihoodCore)*/ likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
            	}


                for (int j = 0; j < patternCount; j++) {

                    int parentIndex = parentState[j] * stateCount;
                    int childIndex = j * stateCount;

                    for (int i = 0; i < stateCount; i++) {
                        conditionalProbabilities[i] = partialLikelihood[childIndex + i] * probabilities[parentIndex + i];
                    }

                    state[j] = drawChoice(conditionalProbabilities);
                    reconstructedStates[nodeNum][j] = state[j];

                    double contrib = probabilities[parentIndex + state[j]];
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

        	if (beagle != null) {
                /*((AbstractLikelihoodCore)*/ getStates(nodeNum, reconstructedStates[nodeNum]);
        	} else {
            /*((AbstractLikelihoodCore)*/ likelihoodCore.getNodeStates(nodeNum, reconstructedStates[nodeNum]);
        	}

            // Check for ambiguity codes and sample them

            for (int j = 0; j < patternCount; j++) {

                final int thisState = reconstructedStates[nodeNum][j];
                final int parentIndex = parentState[j] * stateCount;
            	if (beagle != null) {
                    /*((AbstractLikelihoodCore) */ getTransitionMatrix(nodeNum, probabilities);
            	} else {
                /*((AbstractLikelihoodCore) */likelihoodCore.getNodeMatrix(nodeNum, 0, probabilities);
            	}
                if (dataType.isAmbiguousState(thisState)) {

                    System.arraycopy(probabilities, parentIndex, conditionalProbabilities, 0, stateCount);
                    reconstructedStates[nodeNum][j] = drawChoice(conditionalProbabilities);
                }

                double contrib = probabilities[parentIndex + reconstructedStates[nodeNum][j]];
                //System.out.println("Pr(" + parentState[j] + ", " + reconstructedStates[nodeNum][j] +  ") = " + contrib);
                jointLogLikelihood += Math.log(contrib);
            }
        }
    }

    protected DataType dataType;
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
