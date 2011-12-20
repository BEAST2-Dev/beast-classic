package beast.evolution.tree;

import dr.evolution.tree.TreeTrait;
import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

@Description("Maps nodes in a tree to entries of a parameter")
public class TreeTraitMap extends CalculationNode implements TreeTrait<double[]> {
	public Input<Tree> treeInput = new Input<Tree>("tree", "tree for which to map the nodes", Validate.REQUIRED);
	public Input<RealParameter> parameterInput = new Input<RealParameter>("parameter",
			"paramater for which to map entries for", Validate.REQUIRED);
	public Input<String> traitName = new Input<String>("traitName", "name of the trait", "unnamed");
	public Input<Intent> intent = new Input<Intent>("intent", "intent of the trait, one of " + Intent.values()
			+ " (Default whole tree)", Intent.WHOLE_TREE);

	Tree tree;
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
		if (parameter.getStride1() > 0) {
			parameter.setDimension(nNodes * parameter.getStride1());
			traitvalues = new double[parameter.getStride1()];
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
	}

	public String getTraitName() {
		return traitName.get();
	}

	public Intent getIntent() {
		return intent.get();
	}

	public double [] getTrait(Tree tree, Node node) {
		parameter.getMatrixValues1(nodeToParameterIndexMap[node.getNr()], traitvalues);
		return traitvalues;
	}

	public void setTrait(Node node, double [] values) {
		assert values.length == traitvalues.length;

		int i = nodeToParameterIndexMap[node.getNr()];
		for (int j = 0; j < values.length; j++) {
			parameter.setMatrixValue(i, j, values[j]);
		}
	}

	@Override
	public Class getTraitClass() {
		return double[].class;
	}

	@Override
	public String getTraitString(Tree tree, Node node) {
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
