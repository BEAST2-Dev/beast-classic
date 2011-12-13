package beast.math.distributions;

import beast.core.*;
import beast.core.parameter.RealParameter;
import dr.math.matrixAlgebra.Matrix;


@Description("...")
public class WishartDistribution extends Plugin {
    public Input<Double> df = new Input<Double>("df", "description here");
    public Input<RealParameter> scaleMatrix = new Input<RealParameter>("scaleMatrix", "description here");

    dr.math.distributions.WishartDistribution wishartdistribution;

    @Override
    public void initAndValidate() throws Exception {
        wishartdistribution = new dr.math.distributions.WishartDistribution(
                             df.get(),
                             scaleMatrix.get().getValues());
    }


    void main(String [] arg0) {
        wishartdistribution.main(arg0);
     }
    String getType() {
        return wishartdistribution.getType();
     }
    double df() {
        return wishartdistribution.df();
     }
    double [][] scaleMatrix() {
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
    double [][] nextWishart() {
        return wishartdistribution.nextWishart();
     }
    double [][] nextWishart(double arg0, double [][] arg1) {
        return wishartdistribution.nextWishart(arg0, arg1);
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

}