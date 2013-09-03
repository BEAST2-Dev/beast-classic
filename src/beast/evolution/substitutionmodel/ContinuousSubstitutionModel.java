package beast.evolution.substitutionmodel;

import beast.core.Description;
import beast.core.Input.Validate;
import beast.evolution.datatype.ContinuousDataType;
import beast.evolution.datatype.DataType;
import beast.evolution.substitutionmodel.EigenDecomposition;
import beast.evolution.substitutionmodel.SubstitutionModel;
import beast.evolution.tree.Node;

@Description("Substitution model that can deal with an alignment containing continuous data")
abstract public class ContinuousSubstitutionModel extends SubstitutionModel.Base {

	public ContinuousSubstitutionModel() {
		frequenciesInput.setRule(Validate.OPTIONAL);
	}
	
    /**
     * @return the log likelihood of going from start to stop in the given time
     */
    abstract public double getLogLikelihood(double[] start, double[] stop, double time);


    @Override
	public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
	}

	@Override
	public double[] getRateMatrix(Node node) {
		return null;
	}

	@Override
	public double[] getFrequencies() {
		return null;
	}

	@Override
	public int getStateCount() {
		return 0;
	}

	@Override
	public EigenDecomposition getEigenDecomposition(Node node) {
		return null;
	}

	@Override
	public boolean canReturnComplexDiagonalization() {
		return false;
	}

	@Override
	public boolean canHandleDataType(DataType dataType) throws Exception {
		return (dataType instanceof ContinuousDataType);
	}
	
}
