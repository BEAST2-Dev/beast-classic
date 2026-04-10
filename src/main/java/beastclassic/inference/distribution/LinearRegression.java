package beastclassic.inference.distribution;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Input.Validate;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;
import beast.base.spec.inference.parameter.RealVectorParam;

@Description("(Log-) linear regression model")
public class LinearRegression extends GeneralizedLinearModel {
	public Input<Boolean> logTransformInput = new Input<>("logTransform", "linear model if false, log-linear model if true", false);
	public Input<RealVector<? extends Real>> scaleInput = new Input<>("scale", "description here", Validate.REQUIRED);
	public Input<RealVector<? extends Real>> scaleDesignInput = new Input<>("scaleDesign", "description here");

	@Override
	public void initAndValidate() {
		super.initAndValidate();
		this.logTransform = logTransformInput.get();

		if (scaleDesignInput.get() == null) {
			RealVectorParam<Real> scaleDesign = new RealVectorParam<>();
			scaleDesign.setDimension(N);
			addScaleParameter(scaleInput.get(), scaleDesign);
		} else {
			addScaleParameter(scaleInput.get(), scaleDesignInput.get());
		}
	}

	private static final double normalizingConstant = -0.5 * Math.log(2 * Math.PI);

	private boolean logTransform = false;

	public double[] getTransformedDependentParameter() {
		double[] y = new double[N];
		for (int i = 0; i < N; i++) {
			y[i] = dependentParam.get(i);
			if (logTransform) {
				y[i] = Math.log(y[i]);
			}
		}
		return y;
	}

	public double calculateLogLikelihood() {
		double logLikelihood = 0;
		double[] xBeta = getXBeta();
		double[] precision = getScale();
		double[] y = getTransformedDependentParameter();

		for (int i = 0; i < N; i++) {
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
