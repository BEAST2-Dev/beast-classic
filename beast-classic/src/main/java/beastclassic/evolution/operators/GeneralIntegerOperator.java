package beastclassic.evolution.operators;

import java.util.Arrays;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.Operator;
import beast.base.spec.domain.Int;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.IntVectorParam;
import beast.base.spec.inference.parameter.RealVectorParam;
import beast.base.util.Randomizer;

@Description("Operator to draw integer values from a user specified distribution")
public class GeneralIntegerOperator extends Operator {
	public Input<IntVectorParam<? extends Int>> parameterInput = new Input<>("parameter", "integer parameter to operate on", Validate.REQUIRED);

    public Input<RealVectorParam<? extends Real>> probsInput = new Input<>("probs", "probabilities, one for each integer value. If not specified a uniform distribution is assumed.");
	public Input<IntVectorParam<? extends Int>> indexInput = new Input<>("index", "list of integer values that are allowed. Assumes all values if not specified");
	public Input<Integer> howManyyInput = new Input<Integer>("howMany", "number of parameter values to sample (default 1)" ,1);
	
	/** distribution from which to draw new integer values **/
	double [] probs;
	
	/** nr of parameter valuse to change **/
	int howMany;
	
	/** shadows parameter input **/
	IntVectorParam<? extends Int> parameter;
	
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
    		RealVectorParam<?> probsParam = probsInput.get();
    		probs = new double[probsParam.size()];
    		for (int i = 0; i < probs.length; i++) {
    			probs[i] = probsParam.get(i);
    		}
    	} else if (probsInput.get() == null) {
    		// uniform distribution over the indices specified
    		IntVectorParam<?> indexParam = indexInput.get();
    		int[] indices = indexParam.getValues();
    		int max = 0;
    		for (int i : indices) {
    			max = Math.max(max, i);
    		}
    		probs = new double[max + 1];
    		for (int i = 0; i < indexParam.size(); i++) {
    			probs[indices[i]] = 1.0/indexParam.size();
    		}
    	} else {
    		// index is specified, fully user defined distribution
    		RealVectorParam<?> probsParam = probsInput.get();
    		IntVectorParam<?> indexParam = indexInput.get();
    		if (probsParam.size() != indexParam.size()) {
    			throw new IllegalArgumentException("probs and index must be of the same length");
    		}
    		int[] indices = indexParam.getValues();
    		int max = 0;
    		for (int i : indices) {
    			max = Math.max(max, i);
    		}
    		probs = new double[max + 1];
    		for (int i = 0; i < probsParam.size(); i++) {
    			probs[indices[i]] = probsParam.get(i);
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
			int index = Randomizer.nextInt(parameter.size());
			//int oldValue = parameter.get(index);
			int newValue = Randomizer.randomChoice(probs);
			parameter.set(index, newValue);
		}
		return 0;
	}

} // class GeneralIntegerOperator
