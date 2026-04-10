package beastclassic.evolution.substitutionmodel;

import java.io.PrintStream;

import beast.base.core.BEASTInterface;
import beast.base.spec.type.RealVector;

public class LogLinear extends GlmModel {

	// number of entries in covariates
	// should be #states * (#states-1)
	private int entryCount;

	@Override
	public void initAndValidate() {
		entryCount = (int) covariatesInput.get().get(0).size();

		int n = 1 + (int) Math.sqrt(entryCount);
		if (entryCount != n * (n-1)) {
			throw new IllegalArgumentException("Covariate has " + entryCount + " entries, but expected for "
					+ n + " states to get n*(n-1)=" + n*(n-1) + " states");
		}

		// make sure all covariates have same dimension
		for (int i = 1; i < covariatesInput.get().size(); i++) {
			if (covariatesInput.get().get(i).size() != entryCount) {
				String id0 = covariatesInput.get().get(0) instanceof BEASTInterface bi ? bi.getID() : "?";
				String idI = covariatesInput.get().get(i) instanceof BEASTInterface bi ? bi.getID() : "?";
				throw new IllegalArgumentException("Covariates " + id0 + " and "
						+ idI + " have different dimensions (" +
						entryCount + " and " + covariatesInput.get().get(i).size() + ")");
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
			if (constantErrorInput.get().size() < 1) {
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
			if (indicatorInput.get().get(j)) {
				RealVector<?> covariate = covariatesInput.get().get(j);
				for (int k = 0; k < logrates.length; k++) {
					logrates[k] += scalerInput.get().get(j) * covariate.get(k);
				}
			}
		}

		if (errorInput.get() != null) {
			for (int k = 0; k < logrates.length; k++) {
				logrates[k] += errorInput.get().get(k);
			}
		}

		if (constantErrorInput.get() != null) {
			for (int k = 0; k < logrates.length; k++) {
				logrates[k] += constantErrorInput.get().get(k);
			}
		}

		double [] rates = new double[entryCount];

		double clockRate = clockInput.get().get();
		for (int k = 0; k < entryCount; k++) {
			rates[k] = clockRate * Math.exp(logrates[k]);
		}

		return rates;
	}

	@Override
	public void init(PrintStream out) {
		for (int i = 0; i < scalerInput.get().size(); i++) {
			String covId = covariatesInput.get().get(i) instanceof BEASTInterface bi ? bi.getID() : "" + i;
			out.print(String.format("%sscaler.%s\t", getID(), covId));
		}
	}

	@Override
	public void log(long sample, PrintStream out) {
		for (int i = 0; i < scalerInput.get().size(); i++) {
			if (indicatorInput.get().get(i)) {
				out.print(scalerInput.get().get(i) + "\t");
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
