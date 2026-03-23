package beastclassic.evolution.operators;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.spec.inference.operator.RealRandomWalkOperator;
import beast.base.evolution.tree.Node;
import beast.base.util.Randomizer;
import beastclassic.evolution.tree.TreeTraitMap;

@Description("Random walk opeartor root of a tree trait")
public class RootTraitRandowWalkOperator extends RealRandomWalkOperator {
    public Input<TreeTraitMap> mapInput = new Input<>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);

	double windowSize = 1;
	boolean m_bUseGaussian;

	TreeTraitMap map;

	public void initAndValidate() {
		super.initAndValidate();
		map = mapInput.get();
	}

	@Override
	public double proposal() {

		RealVectorParam<? extends Real> param = parameterInput.get();
		Node root = map.treeInput.get().getRoot();
		int rootOffset = map.getTraitNr(root);
		int traitDim = map.traitDim;
		int i = rootOffset * traitDim + Randomizer.nextInt(traitDim);
		double value = param.get(i);
		double newValue = value;
		if (m_bUseGaussian) {
			newValue += Randomizer.nextGaussian() * windowSize;
		} else {
			newValue += Randomizer.nextDouble() * 2 * windowSize - windowSize;
		}

		if (newValue < param.getLower() || newValue > param.getUpper()) {
			return Double.NEGATIVE_INFINITY;
		}
		if (newValue == value) {
			return Double.NEGATIVE_INFINITY;
		}

		param.set(i, newValue);

		return 0.0;
	}

}
