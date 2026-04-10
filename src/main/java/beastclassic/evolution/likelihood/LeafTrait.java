package beastclassic.evolution.likelihood;

import beast.base.inference.CalculationNode;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Int;
import beast.base.spec.inference.parameter.IntVectorParam;

@Description("For associating a taxon with its parameter")
public class LeafTrait extends CalculationNode {
	public Input<String> taxonName = new Input<>("taxon", "taxon name identifying the leaf", Validate.REQUIRED);
	public Input<IntVectorParam<? extends Int>> parameter = new Input<>("parameter", "parameter associated with the leaf", Validate.REQUIRED);
	
	public void initAndValidate() {
		// nothing to do
	};
}
