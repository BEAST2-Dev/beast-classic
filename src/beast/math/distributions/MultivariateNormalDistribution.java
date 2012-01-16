package beast.math.distributions;


import java.util.List;
import java.util.Random;

import beast.core.*;
import beast.core.parameter.RealParameter;


@Description("...")
public class MultivariateNormalDistribution extends Distribution {
    public Input<RealParameter> mean = new Input<RealParameter>("mean", "description here");
    public Input<RealParameter> precision = new Input<RealParameter>("precision", "description here");
    public Input<Valuable> argInput = new Input<Valuable>("arg", "argument of distribution");

    dr.math.distributions.MultivariateNormalDistribution multivariatenormaldistribution;


    Valuable arg;
    @Override
    public void initAndValidate() throws Exception {
        multivariatenormaldistribution = new dr.math.distributions.MultivariateNormalDistribution(
                             mean.get().getValues(),
                             precision.get().getValues());
        arg = argInput.get();
    }

    @Override
    public double calculateLogP() throws Exception {
    	double [] y = new double[arg.getDimension()];
    	for (int i = 0; i < y.length; i++) {
    		y[i] = arg.getArrayValue(i);
    	}
    	logP = logPdf(y);
    	return logP;
    }

    String getType() {
        return multivariatenormaldistribution.getType();
     }
    double [][] getVariance() {
        return multivariatenormaldistribution.getVariance();
     }
    double [][] getCholeskyDecomposition() {
        return multivariatenormaldistribution.getCholeskyDecomposition();
     }
//    double [][] getCholeskyDecomposition(double [][] arg0) {
//        return multivariatenormaldistribution.getCholeskyDecomposition(arg0);
//     }
    double getLogDet() {
        return multivariatenormaldistribution.getLogDet();
     }
    double calculatePrecisionMatrixDeterminate(double [][] arg0) {
        return multivariatenormaldistribution.calculatePrecisionMatrixDeterminate(arg0);
     }
    double [][] getScaleMatrix() {
        return multivariatenormaldistribution.getScaleMatrix();
     }
    double [] getMean() {
        return multivariatenormaldistribution.getMean();
     }
    double [] nextMultivariateNormal() {
        return multivariatenormaldistribution.nextMultivariateNormal();
     }
    double [] nextMultivariateNormal(double [] arg0) {
        return multivariatenormaldistribution.nextMultivariateNormal(arg0);
     }
    double [] nextMultivariateNormalCholesky(double [] arg0, double [][] arg1) {
        return multivariatenormaldistribution.nextMultivariateNormalCholesky(arg0, arg1);
     }
    double [] nextMultivariateNormalCholesky(double [] arg0, double [][] arg1, double arg2) {
        return multivariatenormaldistribution.nextMultivariateNormalCholesky(arg0, arg1, arg2);
     }
    void nextMultivariateNormalCholesky(double [] arg0, double [][] arg1, double arg2, double [] arg3) {
        multivariatenormaldistribution.nextMultivariateNormalCholesky(arg0, arg1, arg2, arg3);
     }
    double [] nextScaledMultivariateNormal(double [] arg0, double arg1) {
        return multivariatenormaldistribution.nextScaledMultivariateNormal(arg0, arg1);
     }
    void nextScaledMultivariateNormal(double [] arg0, double arg1, double [] arg2) {
        multivariatenormaldistribution.nextScaledMultivariateNormal(arg0, arg1, arg2);
     }
    double logPdf(double [] arg0) {
        return multivariatenormaldistribution.logPdf(arg0);
     }
    double logPdf(double [] arg0, double [] arg1, double [][] arg2, double arg3, double arg4) {
        return multivariatenormaldistribution.logPdf(arg0, arg1, arg2, arg3, arg4);
     }
    double logPdf(double [] arg0, double [] arg1, double arg2, double arg3) {
        return multivariatenormaldistribution.logPdf(arg0, arg1, arg2, arg3);
     }
//    double [][] getInverse(double [][] arg0) {
//        return multivariatenormaldistribution.getInverse(arg0);
//     }
    double [] nextMultivariateNormalPrecision(double [] arg0, double [][] arg1) {
        return multivariatenormaldistribution.nextMultivariateNormalPrecision(arg0, arg1);
     }
    double [] nextMultivariateNormalVariance(double [] arg0, double [][] arg1) {
        return multivariatenormaldistribution.nextMultivariateNormalVariance(arg0, arg1);
     }
    double [] nextMultivariateNormalVariance(double [] arg0, double [][] arg1, double arg2) {
        return multivariatenormaldistribution.nextMultivariateNormalVariance(arg0, arg1, arg2);
     }
    void testPdf() {
        multivariatenormaldistribution.testPdf();
     }
    void testRandomDraws() {
        multivariatenormaldistribution.testRandomDraws();
     }
    public static void main(String[] args) {
    	try {
    		beast.math.distributions.MultivariateNormalDistribution m = new MultivariateNormalDistribution();
	    	RealParameter mean = new RealParameter("0.0");
	    	RealParameter precision = new RealParameter("1.0");
	    	m.initByName("mean", mean, "precision", precision);
	        m.testPdf();
	        m.testRandomDraws();
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }

	@Override
	public List<String> getArguments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getConditions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sample(State state, Random random) {
		// TODO Auto-generated method stub
		
	}

}