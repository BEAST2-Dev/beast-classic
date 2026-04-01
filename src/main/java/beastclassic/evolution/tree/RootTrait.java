package beastclassic.evolution.tree;


import java.io.PrintStream;
import java.util.AbstractList;
import java.util.List;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Loggable;
import beast.base.evolution.tree.TreeInterface;
import beast.base.inference.CalculationNode;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import beastclassic.spec.parameter.MatrixVectorParam;


@Description("A logger for the type of the tree root")
public class RootTrait extends CalculationNode implements RealVector<Real>, Loggable {
    public Input<TreeTraitMap> mapInput = new Input<>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);

    TreeTraitMap map;
    TreeInterface tree;
    MatrixVectorParam<? extends Real> parameter;
    int dim;

    public void initAndValidate() {
    	map = mapInput.get();
    	tree = map.tree;
    	parameter = map.parameterInput.get();
    	dim = parameter.getMinorDimension1();
    }

	@Override
	public double get(int i) {
		int root = tree.getRoot().getNr();
		int row = map.nodeToParameterIndexMap[root];
		return parameter.getMatrixValue(row, i);
	}

	@Override
	public Real getDomain() {
		return Real.INSTANCE;
	}

	@Override
	public List<Double> getElements() {
		return new AbstractList<>() {
			@Override public Double get(int index) { return RootTrait.this.get(index); }
			@Override public int size() { return dim; }
		};
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
			out.append(get(i) + "\t");
		}
	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	};


}
