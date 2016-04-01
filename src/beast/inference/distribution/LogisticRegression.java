package beast.inference.distribution;


import beast.core.*;


@Description("A Logistic Regression Model")
public class LogisticRegression extends GeneralizedLinearModel {

	@Override
    public void initAndValidate() {
    	super.initAndValidate();
    }



	public double calculateLogLikelihoodAndGradient(double[] beta, double[] gradient) {
		return 0;  // todo
	}

	public double calculateLogLikelihood(double[] beta) {
		// logLikelihood calculation for logistic regression
		throw new RuntimeException("Not yet implemented for optimization");
	}

	public boolean requiresScale() {
		return false;
	}

	public double calculateLogLikelihood() {
		// logLikelihood calculation for logistic regression
		double logLikelihood = 0;

		double[] xBeta = getXBeta();

		for (int i = 0; i < N; i++) {
			// for each "pseudo"-datum
			logLikelihood += dependentParam.getArrayValue(i) * xBeta[i]
					- Math.log(1.0 + Math.exp(xBeta[i]));

		}
		return logLikelihood;
	}


	public boolean confirmIndependentParameters() {
		// todo -- check that independent parameters \in {0,1} only
		return true;
	}
}