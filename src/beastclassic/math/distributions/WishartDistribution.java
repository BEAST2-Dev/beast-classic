package beastclassic.math.distributions;

import java.util.List;
import java.util.Random;

import beast.base.core.Description;
import beast.base.core.Function;
import beast.base.core.Input;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.inference.parameter.RealParameter;
import beast.base.math.matrixalgebra.Matrix;




@Description("WishartDistribution ported from BEAST1")
public class WishartDistribution extends Distribution {
    public Input<Double> df = new Input<>("df", "description here");
    public Input<RealParameter> scaleMatrix = new Input<>("scaleMatrix", "description here");
    public Input<Function> argInput = new Input<>("arg", "argument of distribution");

    beastclassic.dr.math.distributions.WishartDistribution wishartdistribution;
    Function arg;
    
    @Override
    public void initAndValidate() {
    	if (scaleMatrix.get() != null) {
    		wishartdistribution = new beastclassic.dr.math.distributions.WishartDistribution(df.get(), scaleMatrix.get().getValues());
    	} else {
    		wishartdistribution = new beastclassic.dr.math.distributions.WishartDistribution((int)(double) df.get());
    	}
        arg = argInput.get();
    }

    @Override
    public double calculateLogP() {
    	double [] y = new double[arg.getDimension()];
    	for (int i = 0; i < y.length; i++) {
    		y[i] = arg.getArrayValue(i);
    	}
    	logP = logPdf(y);
    	return logP;
    }

    void main(String [] arg0) {
        wishartdistribution.main(arg0);
     }
    String getType() {
        return wishartdistribution.getType();
     }
    public double df() {
        return wishartdistribution.df();
     }
    public double [][] scaleMatrix() {
        return wishartdistribution.scaleMatrix();
     }
//    void computeNormalizationConstant() {
//        wishartdistribution.computeNormalizationConstant();
//     }
    double computeNormalizationConstant(Matrix Sinv, double df, int dim) {
        return wishartdistribution.computeNormalizationConstant(Sinv, df, dim);
     }
    double [][] getScaleMatrix() {
        return wishartdistribution.getScaleMatrix();
     }
    double [] getMean() {
        return wishartdistribution.getMean();
     }
    void testMe() {
        wishartdistribution.testMe();
     }
    public double [][] nextWishart() {
        return wishartdistribution.nextWishart();
     }
    public static double [][] nextWishart(double arg0, double [][] arg1) {
        return beastclassic.dr.math.distributions.WishartDistribution.nextWishart(arg0, arg1);
     }
    double logPdf(double [] arg0) {
        return wishartdistribution.logPdf(arg0);
     }
    double logPdf(Matrix W, Matrix Sinv, double df, int dim, double logNormalizationConstant) {
        return wishartdistribution.logPdf(W, Sinv, df, dim, logNormalizationConstant);
     }
    double logPdf2D(double [] arg0, double [] arg1, double arg2, int arg3, double arg4) {
        return wishartdistribution.logPdf2D(arg0, arg1, arg2, arg3, arg4);
     }
    double logPdfSlow(double [] arg0) {
        return wishartdistribution.logPdfSlow(arg0);
     }
    void testBivariateMethod() {
        wishartdistribution.testBivariateMethod();
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