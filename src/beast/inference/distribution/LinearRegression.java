package beast.inference.distribution;


import beast.core.*;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;

@Description("(Log-) linear regression model")
public class LinearRegression extends GeneralizedLinearModel {
	public Input<Boolean> logTransformInput = new Input<Boolean>("logTransform", "linear model if false, log-linear model if true", false);
	public Input<RealParameter> scaleInput = new Input<RealParameter>("scale", "description here", Validate.REQUIRED);
	public Input<RealParameter> scaleDesignInput = new Input<RealParameter>("scaleDesign", "description here");

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		this.logTransform = logTransformInput.get();
		
		if (scaleDesignInput.get() == null) {
			RealParameter scaleDesign = new RealParameter();
			scaleDesign.setDimension(dependentParam.getDimension());
			addScaleParameter(scaleInput.get(), scaleDesign);
		} else {
			addScaleParameter(scaleInput.get(), scaleDesignInput.get());
		}
	}

	private static final double normalizingConstant = -0.5 * Math.log(2 * Math.PI);

	private boolean logTransform = false;

	public Double[] getTransformedDependentParameter() {
		Double[] y = dependentParam.getValues();
		if (logTransform) {
			for (int i = 0; i < y.length; i++)
				y[i] = Math.log(y[i]);
		}
		return y;
	}

	public double calculateLogLikelihood() {
		double logLikelihood = 0;
		double[] xBeta = getXBeta();
		double[] precision = getScale();
		Double[] y = getTransformedDependentParameter();

		for (int i = 0; i < N; i++) { // assumes that all observations are
										// independent given fixed and random
										// effects
			if (logTransform)
				logLikelihood -= y[i]; // Jacobian
			logLikelihood += 0.5 * Math.log(precision[i]) - 0.5 * (y[i] - xBeta[i]) * (y[i] - xBeta[i]) * precision[i];

		}
		return N * normalizingConstant + logLikelihood;
	}

	public double calculateLogLikelihoodAndGradient(double[] beta, double[] gradient) {
		throw new RuntimeException("Optimization not yet implemented.");
	}

	public boolean requiresScale() {
		return true;
	}

	public double calculateLogLikelihood(double[] beta) {
		throw new RuntimeException("Optimization not yet implemented.");
	}

	public boolean confirmIndependentParameters() {
		return true;
	}

}