package beast.evolution.operators;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.operators.RealRandomWalkOperator;
import beast.evolution.tree.Node;
import beast.evolution.tree.TreeTraitMap;
import beast.util.Randomizer;

@Description("Random walk opeartor root of a tree trait")
public class RootTraitRandowWalkOperator extends RealRandomWalkOperator {
    public Input<TreeTraitMap> mapInput = new Input<TreeTraitMap>("traitmap","maps node in tree to trait parameters", Validate.REQUIRED);

	double windowSize = 1;
	boolean m_bUseGaussian;
	
	TreeTraitMap map;

	public void initAndValidate() {
		super.initAndValidate();
		map = mapInput.get();
	}

	/**
	 * override this for proposals, returns log of hastingRatio, or
	 * Double.NEGATIVE_INFINITY if proposal should not be accepted *
	 */
	@Override
	public double proposal() {

		RealParameter param = parameterInput.get(this);
		Node root = map.treeInput.get().getRoot();
		int rootOffset = map.getTraitNr(root);
		int i = rootOffset * param.getMinorDimension1() + Randomizer.nextInt(param.getMinorDimension1());
		double value = param.getValue(i);
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
			// this saves calculating the posterior
			return Double.NEGATIVE_INFINITY;
		}

		param.setValue(i, newValue);

		return 0.0;
	}

}
