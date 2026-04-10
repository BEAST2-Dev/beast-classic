package beastclassic.evolution.substitutionmodel;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.core.Log;
import beast.base.spec.inference.parameter.BoolVectorParam;
import beast.base.spec.evolution.substitutionmodel.ComplexSubstitutionModel;
import beast.base.spec.type.RealVector;
import beastclassic.inference.BayesianStochasticSearchVariableSelection;


/**
 * @author dkuh004
 *         Date: Sep 18, 2011
 *         Time: 6:03:49 PM
 *
 *  * ported from beast1 - author: Marc Suchard
 */
@Description("SVS General Substitution Model")
public class SVSGeneralSubstitutionModel extends ComplexSubstitutionModel implements BayesianStochasticSearchVariableSelection {

    public Input<BoolVectorParam> indicator = new Input<>("rateIndicator",
            "rates to indicate the presence or absence of transition matrix entries", Validate.REQUIRED);

    public Input<Boolean> isSymmetricInput = new Input<>("symmetric",
    		"Indicates the rate matrix is symmetric. " +
            "If true (default) n(n-1)/2 rates and indicators need to be specified. " +
            "If false, n(n-1) rates and indicators need to be specified.", Boolean.TRUE);

    private BoolVectorParam rateIndicator;
    private boolean isSymmetric = false;

    @Override
    public void initAndValidate(){

        frequencies = frequenciesInput.get();

        updateMatrix = true;
        nrOfStates = frequencies.getFreqs().length;
        int ratesDim = ratesInput.get().size();
        if (isSymmetricInput.get() && ratesDim != nrOfStates * (nrOfStates-1)/2) {
            throw new IllegalArgumentException("Dimension of input 'rates' is " + ratesDim + " but a " +
                    "rate matrix of dimension " + nrOfStates + "x" + (nrOfStates -1) + "/2" + "=" + nrOfStates * (nrOfStates -1) / 2 + " was " +
                    "expected");
        }
        if (!isSymmetricInput.get() && ratesDim != nrOfStates * (nrOfStates-1)) {
            int dim = nrOfStates * (nrOfStates -1);
            Log.warning.println("Dimension of input 'rates' is " + ratesDim + ". " +
            		"Expected " + nrOfStates + "x" + (nrOfStates -1)  + "=" + dim);
            isSymmetric = true;
        }

        eigenSystem = createEigenSystem();

        rateMatrix = new double[nrOfStates][nrOfStates];
        relativeRates = new double[ratesDim];
        storedRelativeRates = new double[ratesDim];

        rateIndicator = indicator.get();
    }


    public BoolVectorParam getIndicators() {
        return rateIndicator;
    }

    public boolean validState() {
        return !updateMatrix || Utils.connectedAndWellConditioned(probability,this);
    }

    public void makeDirty() {
        updateMatrix = true;
    }

    private double[] probability = null;

    @Override
    public void setupRelativeRates() {

        RealVector<?> rates = this.ratesInput.get();
        for (int i = 0; i < relativeRates.length; i++) {
            relativeRates[i] = rates.get(i) * (rateIndicator.get(i) ? 1. : 0.);
        }
    }

    /** sets up rate matrix **/
    @Override
    public void setupRateMatrix() {
    	if (!isSymmetricInput.get()) {
    		super.setupRateMatrix();
    		return;
    	}
        double [] fFreqs = frequencies.getFreqs();
        int count = 0;
        for (int i = 0; i < nrOfStates; i++) {
            rateMatrix[i][i] = 0;
            for (int j = i+1; j <  nrOfStates; j++) {
                rateMatrix[i][j] = relativeRates[count];
               	rateMatrix[j][i] = relativeRates[count];
                count++;
            }
        }
        // bring in frequencies
        for (int i = 0; i < nrOfStates; i++) {
            for (int j = i + 1; j < nrOfStates; j++) {
                rateMatrix[i][j] *= fFreqs[j];
                rateMatrix[j][i] *= fFreqs[i];
            }
        }
        // set up diagonal
        for (int i = 0; i < nrOfStates; i++) {
            double fSum = 0.0;
            for (int j = 0; j < nrOfStates; j++) {
                if (i != j)
                    fSum += rateMatrix[i][j];
            }
            rateMatrix[i][i] = -fSum;
        }
        // normalise rate matrix to one expected substitution per unit time
        double fSubst = 0.0;
        for (int i = 0; i < nrOfStates; i++)
            fSubst += -rateMatrix[i][i] * fFreqs[i];

        for (int i = 0; i < nrOfStates; i++) {
            for (int j = 0; j < nrOfStates; j++) {
                rateMatrix[i][j] = rateMatrix[i][j] / fSubst;
            }
        }
    } // setupRateMatrix


    @Override
    protected boolean requiresRecalculation() {
    	// if the rate is only dirty for a value that the indicators block out,
    	// no recalculation is required, so check this first.
    	if (frequencies.isDirtyCalculation()) {
		    return super.requiresRecalculation();
    	}
		BoolVectorParam indicator2 = indicator.get();
		for (int i = 0; i < ratesInput.get().size(); i++) {
			if (indicator2.get(i)) {
		    	return super.requiresRecalculation();
			}
		}
		// no calculation is affected
		return false;
    }

    @Override
    public boolean canReturnComplexDiagonalization() {
    	return !isSymmetric;
    }
}
