package beastclassic.evolution.operators;


import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.inference.Operator;
import beast.base.inference.parameter.RealParameter;
import beast.base.math.matrixalgebra.SymmetricMatrix;
import beastclassic.dr.math.distributions.MultivariateDistribution;
import beastclassic.dr.math.distributions.MultivariateNormalDistribution;
import beastclassic.inference.distribution.LinearRegression;


/**
 * @author Marc Suchard
 */
@Description("Regression Gibbs Effect Operator")
public class RegressionGibbsEffectOperator extends Operator {
    public Input<LinearRegression> linearModelInput = new Input<LinearRegression>("linearModel", "description here");
    public Input<RealParameter> effectInput = new Input<RealParameter>("effect", "description here");
    public Input<RealParameter> indicatorsInput = new Input<RealParameter>("indicators", "description here");
    public Input<MultivariateDistribution> effectPriorInput = new Input<MultivariateDistribution>("effectPrior", "description here");	    

    public static final String GIBBS_OPERATOR = "regressionGibbsEffectOperator";

    private LinearRegression linearModel;
    private RealParameter effect;
    private RealParameter indicators;
    private boolean hasNoIndicators = true;
    private MultivariateDistribution effectPrior;
    private int dim;
    private int effectNumber;
    private int N;
    private int numEffects;
    private double[][] X;

    private double[] mean = null;
    private double[][] variance = null;
    private double[][] precision = null;

    @Override
    public void initAndValidate() {
        this.linearModel = linearModelInput.get();
        this.effect = effectInput.get();
        this.indicators = indicatorsInput.get();
        if (indicators != null) {
            hasNoIndicators = false;
            if (indicators.getDimension() != effect.getDimension())
                throw new RuntimeException("Indicator and effect dimensions must match");
        }
        effectNumber = linearModel.getEffectNumber(effect);
        this.effectPrior = effectPriorInput.get();
        dim = effect.getDimension();
        N = linearModel.getDependentVariable().getDimension();
        numEffects = linearModel.getNumberOfFixedEffects();
        X = linearModel.getX(effectNumber);
    }

    public int getStepCount() {
        return 1;
    }

    public void computeForwardDensity(double[] outMean, double[][] outVariance, double[][] outPrecision) {

         Double[] W = linearModel.getTransformedDependentParameter();
         double[] P = linearModel.getScale();  // outcome precision, fresh copy

         for (int k = 0; k < numEffects; k++) {
             if (k != effectNumber) {
                 double[] thisXBeta = linearModel.getXBeta(k);
                 for (int i = 0; i < N; i++)
                     W[i] -= thisXBeta[i];
             }
         }

         double[] priorBetaMean = effectPrior.getMean();
         double[][] priorBetaScale = effectPrior.getScaleMatrix();

         double[][] XtP = new double[dim][N];
         for (int j = 0; j < dim; j++) {
             if (hasNoIndicators || indicators.getArrayValue(j) == 1) {
                  for (int i = 0; i < N; i++)
                     XtP[j][i] = X[i][j] * P[i];
             } // else already filled with zeros
         }

         double[][] XtPX = new double[dim][dim];
         for (int i = 0; i < dim; i++) {
             if (hasNoIndicators || indicators.getArrayValue(i) == 1) {
                 for (int j = i; j < dim; j++) {// symmetric
                     if (hasNoIndicators || indicators.getArrayValue(j) == 1) {
                         for (int k = 0; k < N; k++)
                             XtPX[i][j] += XtP[i][k] * X[k][j];
                         XtPX[j][i] = XtPX[i][j]; // symmetric
                     }
                 }
             }
         }

         double[][] XtPX_plus_P0 = new double[dim][dim];
         for (int i = 0; i < dim; i++) {
             for (int j = i; j < dim; j++) // symmetric
                 XtPX_plus_P0[j][i] = XtPX_plus_P0[i][j] = XtPX[i][j] + priorBetaScale[i][j];
         }

         double[] XtPW = new double[dim];
         for (int i = 0; i < dim; i++) {
             for (int j = 0; j < N; j++)
                 XtPW[i] += XtP[i][j] * W[j];
         }

         double[] P0Mean0 = new double[dim];
         for (int i = 0; i < dim; i++) {
             for (int j = 0; j < dim; j++)
                 P0Mean0[i] += priorBetaScale[i][j] * priorBetaMean[j];
         }

         double[] unscaledMean = new double[dim];
         for (int i = 0; i < dim; i++)
             unscaledMean[i] = P0Mean0[i] + XtPW[i];

         double[][] variance = new SymmetricMatrix(XtPX_plus_P0).inverse().toComponents();

         for (int i = 0; i < dim; i++) {
             outMean[i] = 0.0;
             for (int j = 0; j < dim; j++) {
                 outMean[i] += variance[i][j] * unscaledMean[j];
                 outVariance[i][j] = variance[i][j];
                 outPrecision[i][j] = XtPX_plus_P0[i][j];
             }
         }
    }

    public double[] getLastMean() { return mean; }

    public double[][] getLastVariance() { return variance; }

    public double[][] getLastPrecision() { return precision; }

    @Override
    public double proposal() {

        if (mean == null)
            mean = new double[dim];

        if (variance == null)
            variance = new double[dim][dim];

        if (precision == null)
            precision = new double[dim][dim];


        computeForwardDensity(mean,variance,precision);

        double[] draw = MultivariateNormalDistribution.nextMultivariateNormalVariance(
                mean, variance);

        for (int i = 0; i < dim; i++)
            effect.setValue(i, draw[i]);

        return 0;
    }

    public String getPerformanceSuggestion() {
        return null;
    }

    public String getOperatorName() {
        return GIBBS_OPERATOR;
    }

}
