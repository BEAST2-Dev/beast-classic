package beast.evolution.tree;

import java.io.PrintStream;

import beast.core.CalculationNode;
import beast.core.Function;
import beast.core.Input;
import beast.core.Loggable;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.tree.Tree;



public class RootTrait extends CalculationNode implements Function, Loggable {
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
	}

	@Override
	public void init(PrintStream out) throws Exception {
		String id = getID();
		if (id == null) {
			id = "RootTrait";
		}
		for (int i = 0; i < dim; i++) {
			out.append(id + i + "\t");
		}
	}

	@Override
	public void log(int nSample, PrintStream out) {
		for (int i = 0; i < dim; i++) {
			out.append(getArrayValue(i) + "\t");
		}
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	};
	
	
}
