package beast.math.distributions;


import beast.core.*;


@Description("...")
class MultivariateNormalDistribution extends Plugin {
    Input<double []> mean = new Input<double []>("mean", "description here");
    Input<double [][]> precision = new Input<double [][]>("precision", "description here");



    dr.math.distributions.MultivariateNormalDistribution multivariatenormaldistribution;


    @Override
    public void initAndValidate() throws Exception {
        multivariatenormaldistribution = new dr.math.distributions.MultivariateNormalDistribution(
                             mean.get(),
                             precision.get());
    }


    void main(String [] arg0) {
        multivariatenormaldistribution.main(arg0);
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

}