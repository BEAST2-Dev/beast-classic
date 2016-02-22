package beast.evolution.likelihood;

import beast.core.CalculationNode;
import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.IntegerParameter;

@Description("For associating a taxon with its parameter")
public class LeafTrait extends CalculationNode {
	public Input<String> taxonName = new Input<String>("taxon", "taxon name identifying the leaf", Validate.REQUIRED);
	public Input<IntegerParameter> parameter = new Input<IntegerParameter>("parameter", "parameter associated with the leaf", Validate.REQUIRED);
	
	public void initAndValidate() {
		// nothing to do
	};
}
