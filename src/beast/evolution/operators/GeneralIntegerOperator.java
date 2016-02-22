package beast.evolution.operators;

import java.util.Arrays;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Operator;
import beast.core.parameter.IntegerParameter;
import beast.core.parameter.RealParameter;
import beast.util.Randomizer;

@Description("Operator to draw integer values from a user specified distribution")
public class GeneralIntegerOperator extends Operator {
	public Input<IntegerParameter> parameterInput = new Input<IntegerParameter>("parameter", "integer paramete to operate on", Validate.REQUIRED);

    public Input<RealParameter> probsInput = new Input<RealParameter>("probs", "probabilities, one for each integer value. If not specified a uniform distribution is assumed.");
	public Input<IntegerParameter> indexInput = new Input<IntegerParameter>("index", "list of integer values that are allowed. Assumes all values if not specified");
	public Input<Integer> howManyyInput = new Input<Integer>("howMany", "number of parameter values to sample (default 1)" ,1);
	
	/** distribution from which to draw new integer values **/
	double [] probs;
	
	/** nr of parameter valuse to change **/
	int howMany;
	
	/** shadows parameter input **/
	IntegerParameter parameter;
	
	@Override
	public void initAndValidate() {
		howMany = howManyyInput.get();
		
		parameter = parameterInput.get();
		
    	if (indexInput.get() == null && probsInput.get() == null) {
    		// uniform distribution: consider using IntUniformOperator instead of this one
    		probs = new double[parameter.getUpper()];
    		Arrays.fill(probs, 1.0/probs.length);
    	} else if (indexInput.get() == null) {
    		// no index specified, so assume it is of the form 0, 1, 2, ...
    		RealParameter probsParam = probsInput.get();
    		probs = new double[probsParam.getDimension()];
    		for (int i = 0; i < probs.length; i++) {
    			probs[i] = probsParam.getValue(i);
    		}
    	} else if (probsInput.get() == null) {
    		// uniform distribution over the indices specified
    		IntegerParameter indexParam = indexInput.get();
    		Integer [] indices = indexParam.getValues();
    		int max = 0;
    		for (int i : indices) {
    			max = Math.max(max, i);
    		}
    		probs = new double[max + 1];
    		for (int i = 0; i < indexParam.getDimension(); i++) {
    			probs[indices[i]] = 1.0/indexParam.getDimension();
    		}
    	} else {
    		// index is specified, fully user defined distribution
    		RealParameter probsParam = probsInput.get();
    		IntegerParameter indexParam = indexInput.get();
    		if (probsParam.getDimension() != indexParam.getDimension()) {
    			throw new IllegalArgumentException("probs and index must be of the same length");
    		}
    		Integer [] indices = indexParam.getValues();
    		int max = 0;
    		for (int i : indices) {
    			max = Math.max(max, i);
    		}
    		probs = new double[max + 1];
    		for (int i = 0; i < probsParam.getDimension(); i++) {
    			probs[indices[i]] = probsParam.getValue(i);
    		}
    	}
    	
    	// sanity check
    	double sum = 0;
    	for (double f : probs) {
    		sum += f;
    	}
    	if (Math.abs(sum - 1.0) > 1e-6) {
    		throw new IllegalArgumentException("Probabilities must sum to one (instead of " + sum + ")");
    	}
    	
    	// convert to cumulative pdf
    	for (int i = 1; i < probs.length; i++) {
    		probs[i] += probs[i-1];
    	}
	}
	

	@Override
	public double proposal() {
		for (int i = 0; i < howMany; i++) {
            // do not worry about duplication, does not matter
			int index = Randomizer.nextInt(parameter.getDimension());
			//int oldValue = parameter.getValue(index);
			int newValue = Randomizer.randomChoice(probs);
			parameter.setValue(index, newValue);
		}
		return 0;
	}

} // class GeneralIntegerOperator
