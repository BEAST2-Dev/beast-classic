package beast.evolution.tree;


import java.io.PrintStream;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Loggable;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.CalculationNode;
import beast.base.inference.parameter.RealParameter;


@Description("A logger for the type of the tree root")
public class RootTrait extends CalculationNode implements Function, Loggable {
    public Input<TreeTraitMap> mapInput = new Input<TreeTraitMap>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);

    TreeTraitMap map;
    TreeInterface tree;
    RealParameter parameter;
    int dim;
    
    public void initAndValidate() {
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
	public void init(PrintStream out) {
		String id = getID();
		if (id == null) {
			id = "RootTrait";
		}
		for (int i = 0; i < dim; i++) {
			out.append(id + i + "\t");
		}
	}

	@Override
	public void log(long nSample, PrintStream out) {
		for (int i = 0; i < dim; i++) {
			out.append(getArrayValue(i) + "\t");
		}
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	};
	
	
}
