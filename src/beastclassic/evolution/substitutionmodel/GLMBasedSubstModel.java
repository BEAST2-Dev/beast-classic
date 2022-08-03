package beastclassic.evolution.substitutionmodel;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.evolution.substitutionmodel.GeneralSubstitutionModel;
import beastlabs.evolution.substitutionmodel.ComplexColtEigenSystem;

@Description("Subst model where the rates are determined by a generalised linear model (GLM)")
public class GLMBasedSubstModel extends GeneralSubstitutionModel {
	
	public Input<GLM> glmInput = new Input<>("glm", "GLM describing the rates", Validate.REQUIRED);
	GLM glm;
	
	public GLMBasedSubstModel() {
		ratesInput.setRule(Validate.OPTIONAL);
	}
	
	@Override
	public void initAndValidate() {
		glm = glmInput.get();
        frequencies = frequenciesInput.get();
        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        if (glmInput.get().getDimension() != nrOfStates) {
            throw new IllegalArgumentException("Dimension of input 'rates' is " + glmInput.get().getDimension() + " but a " +
                    "a dimension of " + nrOfStates + " was expected");
        }

        eigenSystem = new ComplexColtEigenSystem(nrOfStates);

        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[nrOfStates * (nrOfStates-1)];
        storedRelativeRates = new double[relativeRates.length];	
    }
	
	
	@Override
	public void setupRelativeRates() {
		relativeRates = glm.getRateMatrix();
	}

}
