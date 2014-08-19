package beast.evolution.tree;

import java.util.List;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeInterface;
import beast.util.Randomizer;

@Description("Maps nodes in a tree to entries of a parameter")
public class TreeTraitMap extends CalculationNode implements TreeTrait<double[]> {
	public Input<TreeInterface> treeInput = new Input<TreeInterface>("tree", "tree for which to map the nodes", Validate.REQUIRED);
	public Input<RealParameter> parameterInput = new Input<RealParameter>("parameter",
			"paramater for which to map entries for", Validate.REQUIRED);
	public Input<String> traitName = new Input<String>("traitName", "name of the trait", "unnamed");
	public Input<Intent> intent = new Input<Intent>("intent", "intent of the trait, one of " + Intent.values()
			+ " (Default whole tree)", Intent.WHOLE_TREE);
	public Input<String> value = new Input<String>("value","initialisation values for traits in the form of " +
			"a comma separated string of taxon-name, value pairs. For example, for a two-dimensional trait " +
			"the value could be Taxon1=10 20,Taxon2=20 30,Taxon3=10 10");

	public Input<Boolean> initAsMeanInput =new Input<Boolean>("initByMean", "initialise internal nodes by taking the mean of its children", false);
	public Input<Double> jitterInput =new Input<Double>("jitter", "amount of jitter used to ensure traits are not exactly the same when using initByMean", 0.0001);
	
	public Input<String> randomizeupper = new Input<String>("randomizeupper", "if specified, used as upper bound for randomly initialising unassigned nodes");
	public Input<String> randomizelower = new Input<String>("randomizelower", "if specified, used as lower bound for randomly initialising unassigned nodes");
	TreeInterface tree;
	RealParameter parameter;
	

	/** flag to indicate the root has no trait **/
	boolean rootHasNoTrait;
	int[] nodeToParameterIndexMap;
	int[] storedNodeToParameterIndexMap;

	double [] traitvalues;
	@Override
	public void initAndValidate() throws Exception {
		tree = treeInput.get();
		int nNodes = tree.getNodeCount();
		parameter = parameterInput.get();
		if (parameter.getMinorDimension1() > 0) {
			parameter.setDimension(nNodes * parameter.getMinorDimension1());
			traitvalues = new double[parameter.getMinorDimension1()];
		} else {
			parameter.setDimension(nNodes);
			traitvalues = new double[1];
		}
		
		
		rootHasNoTrait = intent.get().equals(Intent.BRANCH);

		nodeToParameterIndexMap = new int[nNodes];
		storedNodeToParameterIndexMap = new int[nNodes];
		for (int i = 0; i < nNodes; i++) {
			nodeToParameterIndexMap[i] = i;
		}
		if (rootHasNoTrait) {
			nodeToParameterIndexMap[tree.getRoot().getNr()] = -1;
		}
		System.arraycopy(nodeToParameterIndexMap, 0, storedNodeToParameterIndexMap, 0, nNodes);
		
		
		if ((value.get() != null && value.get().trim().length() > 0) || randomizelower.get() != null || randomizeupper.get() != null) {
	        List<String> sTaxa = tree.getTaxonset().asStringList();
	        boolean [] bDone = new boolean[sTaxa.size()];
	        
			// we need to initialise the trait parameter
	        int dim = traitvalues.length;
			Double [] values = new Double[nNodes * dim];
			if (randomizelower.get() != null || randomizeupper.get() != null) {
				// randomly assign values in the provided range
				// defaults to range with extremes zero if nothing is specified
				double [] upper = new double[dim];
				double [] lower = new double[dim];
				if (randomizelower.get() != null) {
					String [] lowers = randomizelower.get().split("\\s+");
					int i = 0;
					while (i < dim) {
						lower[i] = Double.parseDouble(lowers[i % lowers.length]);
						i++;
					}
				}
				if (randomizeupper.get() != null) {
					String [] uppers = randomizeupper.get().split("\\s+");
					int i = 0;
					while (i < dim) {
						upper[i] = Double.parseDouble(uppers[i % uppers.length]);
						i++;
					}
				}
				for (int i = 0; i < nNodes; i++) {
					for (int j = 0; j < dim; j++) {
						values[i * dim + j] = lower[j] + Randomizer.nextDouble() * (upper[j] - lower[j]);
					}
				}
			}
		
			if (value.get() != null && value.get().trim().length() > 0) {
				String [] sValues = value.get().split(",");
		        for (String sTrait : sValues) {
		            sTrait = sTrait.replaceAll("\\s+", " ");
		            String[] sStrs = sTrait.split("=");
		            if (sStrs.length != 2) {
		                throw new Exception("could not parse trait: " + sTrait);
		            }
		            String sTaxonID = normalize(sStrs[0]);
		            int iTaxon = sTaxa.indexOf(sTaxonID);
		            if (iTaxon < 0) {
		                throw new Exception("Trait (" + sTaxonID + ") is not a known taxon. Spelling error perhaps?");
		            }
		            String sTraitValue = normalize(sStrs[1]);
		            String [] sTraitValues = sTraitValue.split("\\s");
		            for (int i = 0; i < sTraitValues.length; i++) {
		            	values[iTaxon * dim + i] = Double.parseDouble(sTraitValues[i]);
		            }
		            if (bDone[iTaxon]) {
		            	throw new Exception("Trait for taxon " + sTaxa.get(iTaxon)+ " defined twice");
		            }
		            bDone[iTaxon] = true;
		        }
			}
	        // sanity check: did we cover all taxa?
	        for (int i = 0; i < sTaxa.size(); i++) {
	            if (!bDone[i]) {
	                System.out.println("WARNING: no trait specified for " + sTaxa.get(i));
	            }
	        }
	        
	        // initialise internal nodes
	        if (initAsMeanInput.get()) {
	        	initInternalNodes(tree.getRoot(), values, dim);
	        }
	        
	        RealParameter tmp = new RealParameter(values);	        
	        tmp.setBounds(parameter.getLower(), parameter.getUpper());
	        parameter.assignFromWithoutID(tmp);
		}
	}

	/** set trait value as mean of its children **/
	private void initInternalNodes(Node node, Double[] values, int dim) {
		double jitter = jitterInput.get();
		if (!node.isLeaf()) {
			for (Node child : node.getChildren()) {
				initInternalNodes(child, values, dim);
			}
			for (int i = 0; i < dim; i++) {
				double value = 0;
				for (Node child : node.getChildren()) {
					value += values[child.getNr() * dim + i];
				}
				values[node.getNr() * dim + i] = value / dim + Randomizer.nextDouble() * jitter - jitter / 2.0;
			}
		}
	}

//	private int indexOf(String[] sTaxa, String sTaxonID) {
//		int i = sTaxa.length - 1;
//		while (i >= 0 && !sTaxa[i].equals(sTaxonID)) {
//			i--;
//		}
//		return i;
//	}

	/**
     * remove start and end spaces
     */
    String normalize(String sStr) {
        if (sStr.charAt(0) == ' ') {
            sStr = sStr.substring(1);
        }
        if (sStr.endsWith(" ")) {
            sStr = sStr.substring(0, sStr.length() - 1);
        }
        return sStr;
    }

	public String getTraitName() {
		return traitName.get();
	}

	public Intent getIntent() {
		return intent.get();
	}

	public double [] getTrait(TreeInterface tree, Node node) {
		int id = nodeToParameterIndexMap[node.getNr()];
		parameter.getMatrixValues1(id, traitvalues);
		return traitvalues.clone();
	}
	
	public int getTraitNr(Node node) {
		int id = nodeToParameterIndexMap[node.getNr()];
		return id;
	}

	public void setTrait(Node node, double [] values) {
		assert values.length == traitvalues.length;

		int i = nodeToParameterIndexMap[node.getNr()];
		for (int j = 0; j < values.length; j++) {
			parameter.setMatrixValue(i, j, values[j]);
		}
	}

	@Override
	public Class<?> getTraitClass() {
		return double[].class;
	}

	@Override
	public String getTraitString(TreeInterface tree, Node node) {
		double [] values = getTrait(tree, node);
        if (values == null || values.length == 0) return null;
        if (values.length > 1) {
            StringBuilder sb = new StringBuilder("{");
            sb.append(values[0]);
            for (int i = 1; i < values.length; i++) {
                sb.append(",");
                sb.append(values[i]);
            }
            sb.append("}");

            return sb.toString();
        } else {
            return Double.toString(values[0]);
        }
	}

	public boolean getLoggable() {
		return true;
	}

	@Override
	protected void store() {
		System.arraycopy(nodeToParameterIndexMap, 0, storedNodeToParameterIndexMap, 0, nodeToParameterIndexMap.length);
		super.store();
	}

	@Override
	protected void restore() {
		int[] tmp = nodeToParameterIndexMap;
		nodeToParameterIndexMap = storedNodeToParameterIndexMap;
		storedNodeToParameterIndexMap = tmp;
		super.restore();
	}

	@Override
	protected boolean requiresRecalculation() {
		if (tree.somethingIsDirty()) {
			if (rootHasNoTrait) {
				int rootNr = tree.getRoot().getNr();
				if (nodeToParameterIndexMap[rootNr] >= 0) {
					// the root has changed, so what was the root before now
					// inherits the current root's trait
					int nr = nodeToParameterIndexMap[rootNr];
					int i = 0;
					while (nodeToParameterIndexMap[i] >= 0) {
						i++;
					}
					nodeToParameterIndexMap[i] = nr;
					nodeToParameterIndexMap[rootNr] = -1;
				}
			}
		}
		// the parameter must have changed, so mark as dirty
		return true;
	}

}
