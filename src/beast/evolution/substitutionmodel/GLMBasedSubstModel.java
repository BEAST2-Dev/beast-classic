package beast.evolution.substitutionmodel;


import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;

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
	protected void setupRelativeRates() {
		relativeRates = glm.getRateMatrix();
	}

}
