package beastclassic.evolution.likelihood;

import beast.base.inference.CalculationNode;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.inference.parameter.IntegerParameter;

@Description("For associating a taxon with its parameter")
public class LeafTrait extends CalculationNode {
	public Input<String> taxonName = new Input<String>("taxon", "taxon name identifying the leaf", Validate.REQUIRED);
	public Input<IntegerParameter> parameter = new Input<IntegerParameter>("parameter", "parameter associated with the leaf", Validate.REQUIRED);
	
	public void initAndValidate() {
		// nothing to do
	};
}
