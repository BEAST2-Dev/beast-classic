package beastclassic.evolution.substitutionmodel;

import java.util.ArrayList;
import java.util.List;

import beast.base.inference.CalculationNode;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Loggable;
import beast.base.inference.parameter.BooleanParameter;
import beast.base.inference.parameter.RealParameter;

public abstract class GlmModel extends CalculationNode implements Loggable {
	
    public Input<List<RealParameter>> covariatesInput = new Input<>("covariates", "input of covariates", new ArrayList<>(), Validate.REQUIRED);
    public Input<RealParameter> scalerInput = new Input<>("scaler", "input of covariates scaler", Validate.REQUIRED);    
    public Input<BooleanParameter> indicatorInput = new Input<>("indicator", "input of covariates scaler", Validate.REQUIRED);
    public Input<RealParameter> clockInput = new Input<>("clock", "clock rate of the parameter",Validate.REQUIRED);
    public Input<RealParameter> errorInput = new Input<>("error", "time variant error term in the GLM model for the rates");
    public Input<RealParameter> constantErrorInput = new Input<>("constantError", "time invariant error term in the GLM model for the rates");
    
//    public int nrIntervals;
//    public int verticalEntries;
    
	public abstract double[] getRates();
	
	public boolean isDirty(){
		for (int i = 0; i < scalerInput.get().getDimension(); i++)
			if(scalerInput.get().isDirty(i))
					return true;
		
		for (int i = 0; i < indicatorInput.get().getDimension(); i++)
			if(indicatorInput.get().isDirty(i))
					return true;
		
		if (errorInput.get() != null)
			for (int i = 0; i < errorInput.get().getDimension(); i++)
				if(errorInput.get().isDirty(i))
						return true;
		
		if (constantErrorInput.get() != null)
			for (int i = 0; i < constantErrorInput.get().getDimension(); i++)
				if(constantErrorInput.get().isDirty(i))
						return true;

		
		if (clockInput.get().isDirty(0))
			return true;
		
		return false;
	}

//	public void setNrIntervals(int i){
//		nrIntervals = i;
////		verticalEntries = covariatesInput.get().get(0).getDimension()/nrIntervals;
//	}
	

}
