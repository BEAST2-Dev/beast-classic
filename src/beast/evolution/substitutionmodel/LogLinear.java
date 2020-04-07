package beast.evolution.substitutionmodel;

import java.io.PrintStream;

import beast.core.Function;

public class LogLinear extends GlmModel {

	// number of entries in covariates
	// should be #states * (#states-1)
	private int entryCount;

	@Override
	public void initAndValidate() {
		entryCount = covariatesInput.get().get(0).getDimension();
		
		int n = 1 + (int) Math.sqrt(entryCount);
		if (entryCount != n * (n-1)) {
			throw new IllegalArgumentException("Covariate has " + entryCount + " entries, but expected for "
					+ n + " states to get n*(n-1)=" + n*(n-1) + " states");
		}
		
		// make sure all covariates have same dimension
		for (int i = 1; i < covariatesInput.get().size(); i++) {
			if (covariatesInput.get().get(i).getDimension() != entryCount) {
				throw new IllegalArgumentException("Covariates " + covariatesInput.get().get(0).getID() + " and " 
						+ covariatesInput.get().get(i).getID() + " have different dimensions (" + 
						entryCount + " and " + covariatesInput.get().get(i).getDimension() + ")");
			}
		}
		
		// set the dimension of the scalers, indicators and potentially the
		// error term
		scalerInput.get().setDimension(entryCount);
		indicatorInput.get().setDimension(entryCount);

		if (errorInput.get() != null) {
			errorInput.get().setDimension(1);
		}

		if (constantErrorInput.get() != null) {
			if (constantErrorInput.get().getDimension() < 1) {
				constantErrorInput.get().setDimension(entryCount);
			}
		}
	}

	@Override
	public double[] getRates() {
		double [] logrates = new double[entryCount];

		for (int j = 0; j < logrates.length; j++)
			logrates[j] = 0;

		for (int j = 0; j < covariatesInput.get().size(); j++) {
			if (indicatorInput.get().getValue(j)) {
				Function covariate = covariatesInput.get().get(j);
				for (int k = 0; k < logrates.length; k++) {
					logrates[k] += scalerInput.get().getArrayValue(j) * covariate.getArrayValue(k);
				}
			}
		}

		if (errorInput.get() != null) {
			for (int k = 0; k < logrates.length; k++) {
				logrates[k] += errorInput.get().getArrayValue(k);
			}
		}

		if (constantErrorInput.get() != null) {
			for (int k = 0; k < logrates.length; k++) {
				logrates[k] += constantErrorInput.get().getArrayValue(k);
			}
		}

		double [] rates = new double[entryCount];

		double clockRate = clockInput.get().getArrayValue();
		for (int k = 0; k < entryCount; k++) {
			rates[k] = clockRate * Math.exp(logrates[k]);
		}

		return rates;
	}

	@Override
	public void init(PrintStream out) {
		for (int i = 0; i < scalerInput.get().getDimension(); i++) {
			out.print(String.format("%sscaler.%s\t", getID(), covariatesInput.get().get(i).getID()));
		}
	}

	@Override
	public void log(int sample, PrintStream out) {
		for (int i = 0; i < scalerInput.get().getDimension(); i++) {
			if (indicatorInput.get().getArrayValue(i) > 0.5) {
				out.print(scalerInput.get().getArrayValue(i) + "\t");
			} else {
				out.print("0.0\t");
			}
		}

	}

	@Override
	public void close(PrintStream out) {
		// nothing to do
	}

}
