package beast.evolution.tree;

import beast.core.CalculationNode;
import beast.core.Input;
import beast.core.Valuable;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

public class RootTrait extends CalculationNode implements Valuable {
    public Input<TreeTraitMap> mapInput = new Input<TreeTraitMap>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);

    TreeTraitMap map;
    Tree tree;
    RealParameter parameter;
    int dim;
    
    public void initAndValidate() throws Exception {
    	map = mapInput.get();
    	tree = map.tree;
    	parameter = map.parameterInput.get();
    	dim = parameter.getMinorDimension1();
    }

	@Override
	public int getDimension() {
		return dim;
	}

	@Override
	public double getArrayValue() {
		return getArrayValue(0);
	}

	@Override
	public double getArrayValue(int iDim) {
		int root = tree.getRoot().getNr();
		int i = map.nodeToParameterIndexMap[root];
		double value = parameter.getMatrixValue(i, iDim);
		return value;
	};
}
