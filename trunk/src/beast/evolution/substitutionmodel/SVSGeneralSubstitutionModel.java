package beast.evolution.substitutionmodel;

import beast.core.parameter.Parameter;
import beast.core.parameter.BooleanParameter;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.Valuable;
import beast.evolution.tree.Node;
import beast.inference.BayesianStochasticSearchVariableSelection;

import java.lang.reflect.Array;
import java.util.Arrays;


/**
 * @author dkuh004
 *         Date: Sep 18, 2011
 *         Time: 6:03:49 PM
 *
 *  * ported from beast1 - author: Marc Suchard
 */
public class SVSGeneralSubstitutionModel extends GeneralSubstitutionModel implements BayesianStochasticSearchVariableSelection {

    public Input<BooleanParameter> indicator = new Input<BooleanParameter>("rateIndicator",
            "rates to indicate the presence or absence of transition matrix entries", Validate.REQUIRED);

    private BooleanParameter rateIndicator;

    @Override
    public void initAndValidate() throws Exception{

        m_frequencies = frequenciesInput.get();

        updateMatrix = true;
        m_nStates = m_frequencies.getFreqs().length;
        if (m_rates.get().getDimension() != m_nStates * (m_nStates-1)/2) {
            throw new Exception("Dimension of input 'rates' is " + m_rates.get().getDimension() + " but a " +
                    "rate matrix of dimension " + m_nStates + "x" + (m_nStates -1) + "/2" + "=" + m_nStates * (m_nStates -1) / 2 + " was " +
                    "expected");
        }

        eigenSystem = createEigenSystem();
        
//        if (robust.get()){
//            eigenSystem = new RobustEigenSystem(m_nStates);
//        }   else {
//            eigenSystem = new DefaultEigenSystem(m_nStates);
//        }
        
        m_rateMatrix = new double[m_nStates][m_nStates];
        relativeRates = new double[m_rates.get().getDimension()];
        storedRelativeRates = new double[m_rates.get().getDimension()];

        rateIndicator = indicator.get();
    }


    public Parameter<?> getIndicators() {
        return rateIndicator;
    }

    public boolean validState() {
        return !updateMatrix || Utils.connectedAndWellConditioned(probability,this);
    }


    /**
     * Forces a complete recalculation of the likelihood next time getLikelihood is called
     */
    public void makeDirty() {
        updateMatrix = true;
    }

    // **************************************************************
    // Loggable IMPLEMENTATION
    // **************************************************************

    @Override
    public void getTransitionProbabilities(Node node, double fStartTime, double fEndTime, double fRate, double[] matrix) {
        double distance = (fStartTime - fEndTime) * fRate;

        int i, j, k;
        double temp;

        // this must be synchronized to avoid being called simultaneously by
        // two different likelihood threads - AJD
        synchronized (this) {
            if (updateMatrix) {
                setupRelativeRates();
                setupRateMatrix();
                try{
                    eigenDecomposition = eigenSystem.decomposeMatrix(m_rateMatrix);
                }catch(Exception e){
                    updateMatrix = false;
                    Arrays.fill(matrix, 0);
                    return;

                }
                updateMatrix = false;
            }
        }

        // is the following really necessary?
        // implemented a pool of iexp matrices to support multiple threads
        // without creating a new matrix each call. - AJD
        // a quick timing experiment shows no difference - RRB
        double[] iexp = new double[m_nStates * m_nStates];
        // Eigen vectors
        double[] Evec = eigenDecomposition.getEigenVectors();
        // inverse Eigen vectors
        double[] Ievc = eigenDecomposition.getInverseEigenVectors();
        // Eigen values
        double[] Eval = eigenDecomposition.getEigenValues();
        for (i = 0; i < m_nStates; i++) {
            temp = Math.exp(distance * Eval[i]);
            for (j = 0; j < m_nStates; j++) {
                iexp[i * m_nStates + j] = Ievc[i * m_nStates + j] * temp;
            }
        }

        int u = 0;
        for (i = 0; i < m_nStates; i++) {
            for (j = 0; j < m_nStates; j++) {
                temp = 0.0;
                for (k = 0; k < m_nStates; k++) {
                    temp += Evec[i * m_nStates + k] * iexp[k * m_nStates + j];
                }

                matrix[u] = Math.abs(temp);
                u++;
            }
        }
    } // getTransitionProbabilities


    private double[] probability = null;

    @Override
    protected void setupRelativeRates() {

        Valuable rates = m_rates.get();
        for (int i = 0; i < relativeRates.length; i++) {
            relativeRates[i] = rates.getArrayValue(i) * (rateIndicator.getValue(i)?1.:0.);
        }
    }

    /** sets up rate matrix **/
    @Override
    protected void setupRateMatrix() {
        double [] fFreqs = m_frequencies.getFreqs();
        int count = 0;
        for (int i = 0; i < m_nStates; i++) {
            m_rateMatrix[i][i] = 0;
            for (int j = i+1; j <  m_nStates; j++) {
                m_rateMatrix[i][j] = relativeRates[count];
                m_rateMatrix[j][i] = relativeRates[count];
                count++;
            }
//             for (int j = i+1; j < m_nStates; j++) {
//                 m_rateMatrix[i][j] = relativeRates[i * (m_nStates -1) + j-1];
//             }
        }
        // bring in frequencies
        for (int i = 0; i < m_nStates; i++) {
            for (int j = i + 1; j < m_nStates; j++) {
                m_rateMatrix[i][j] *= fFreqs[j];
                m_rateMatrix[j][i] *= fFreqs[i];
            }
        }
        // set up diagonal
        for (int i = 0; i < m_nStates; i++) {
            double fSum = 0.0;
            for (int j = 0; j < m_nStates; j++) {
                if (i != j)
                    fSum += m_rateMatrix[i][j];
            }
            m_rateMatrix[i][i] = -fSum;
        }
        // normalise rate matrix to one expected substitution per unit time
        double fSubst = 0.0;
        for (int i = 0; i < m_nStates; i++)
            fSubst += -m_rateMatrix[i][i] * fFreqs[i];

        for (int i = 0; i < m_nStates; i++) {
            for (int j = 0; j < m_nStates; j++) {
                m_rateMatrix[i][j] = m_rateMatrix[i][j] / fSubst;
            }
        }
    } // setupRateMatrix


    @Override
    protected boolean requiresRecalculation() {
    	// if the rate is only dirty for a value that the indicators block out,
    	// no recalculation is required, so check this first.
    	Valuable v = m_rates.get(); 
    	if (v instanceof Parameter<?>) {
    		Parameter<?> p = (Parameter<?>) v;
    		if (p.somethingIsDirty()) {
    			if (m_frequencies.isDirtyCalculation()) {
			    	return super.requiresRecalculation();
    			}
        		Parameter<Boolean> indicator2 = indicator.get(); 
    			for (int i = 0; i < p.getDimension(); i++) {
    				if (indicator2.getValue(i) && p.isDirty(i)) {
    			    	return super.requiresRecalculation();
    				}
    			}
    			// no calculation is affected
    			return false;
    		}
    	}

    	return super.requiresRecalculation();
    }
}
