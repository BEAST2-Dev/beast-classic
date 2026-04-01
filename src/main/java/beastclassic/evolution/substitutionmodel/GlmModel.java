package beastclassic.evolution.substitutionmodel;

import java.util.ArrayList;
import java.util.List;

import beast.base.inference.CalculationNode;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Loggable;
import beast.base.spec.domain.NonNegativeReal;
import beast.base.spec.domain.PositiveReal;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.inference.parameter.RealScalarParam;
import beast.base.spec.inference.parameter.RealVectorParam;

public abstract class GlmModel extends CalculationNode implements Loggable {

    public Input<List<RealVector<? extends Real>>> covariatesInput = new Input<>("covariates", "input of covariates", new ArrayList<>(), Validate.REQUIRED);
    public Input<RealVectorParam<? extends PositiveReal>> scalerInput = new Input<>("scaler", "input of covariates scaler", Validate.REQUIRED);
    public Input<BoolVectorParam> indicatorInput = new Input<>("indicator", "input of covariates scaler", Validate.REQUIRED);
    public Input<RealScalarParam<? extends PositiveReal>> clockInput = new Input<>("clock", "clock rate of the parameter", Validate.REQUIRED);
    public Input<RealVectorParam<? extends NonNegativeReal>> errorInput = new Input<>("error", "time variant error term in the GLM model for the rates");
    public Input<RealVectorParam<? extends NonNegativeReal>> constantErrorInput = new Input<>("constantError", "time invariant error term in the GLM model for the rates");

	public abstract double[] getRates();

	public boolean isDirty(){
		for (int i = 0; i < scalerInput.get().size(); i++)
			if(scalerInput.get().isDirty(i))
					return true;

		for (int i = 0; i < indicatorInput.get().size(); i++)
			if(indicatorInput.get().isDirty(i))
					return true;

		if (errorInput.get() != null)
			for (int i = 0; i < errorInput.get().size(); i++)
				if(errorInput.get().isDirty(i))
						return true;

		if (constantErrorInput.get() != null)
			for (int i = 0; i < constantErrorInput.get().size(); i++)
				if(constantErrorInput.get().isDirty(i))
						return true;


		if (clockInput.get().somethingIsDirty())
			return true;

		return false;
	}

}
