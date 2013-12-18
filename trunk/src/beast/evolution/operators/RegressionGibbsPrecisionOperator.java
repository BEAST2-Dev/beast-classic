package beast.evolution.operators;



import beast.core.Description;
import beast.core.Input;
import beast.core.Operator;
import beast.core.parameter.RealParameter;
import beast.math.distributions.Gamma;
import beast.util.Randomizer;
import beast.inference.distribution.*;


/**
 * @author Marc Suchard
 */
@Description("???")
public class RegressionGibbsPrecisionOperator extends Operator {
    public Input<LinearRegression> linearModelInput = new Input<LinearRegression>("linearModel", "description here");
    public Input<RealParameter> precisionInput = new Input<RealParameter>("precision", "description here");
    public Input<Gamma> priorInput = new Input<Gamma>("prior", "description here");

    public static final String GIBBS_OPERATOR = "regressionGibbsPrecisionOperator";

    private LinearRegression linearModel;
    private RealParameter precision;
    private int dim;
    private int N;
    private int[] scaleDesign;
    private Gamma prior;

    @Override
    public void initAndValidate() throws Exception {
    //if (!(prior instanceof GammaDistribution))
        //      throw new RuntimeException("Precision prior must be Gamma");
        this.prior = priorInput.get();
        this.linearModel = linearModelInput.get();
        this.precision = precisionInput.get();
        this.dim = precision.getDimension();
        scaleDesign = linearModel.getScaleDesign();
        N = linearModel.getDependentVariable().getDimension();
    }

    public int getStepCount() {
        return 1;
    }

    @Override
    public double proposal() {

        Double[] Y = linearModel.getTransformedDependentParameter();
        double[] xBeta = linearModel.getXBeta();

        
        double alpha = prior.alphaInput.get().getValue();
        double beta= prior.alphaInput.get().getValue();        
        final double priorMean = alpha * beta; // prior.mean();
        final double priorVariance = alpha * beta * beta; // prior.variance();

        double priorRate;
        double priorShape;

        if (priorMean == 0) {
            priorRate = 0;
            priorShape = -0.5; // Uninformative prior
        } else {
            priorRate = priorMean / priorVariance;
            priorShape = priorMean * priorRate;
        }

        for (int k = 0; k < dim; k++) { // Do draw for precision[k]

            // Calculate weighted sum-of-squares
            double SSE = 0;
            int n = 0;

            for(int i=0; i<N; i++) {
                if(scaleDesign[i] == k) {
                    SSE += (Y[i] - xBeta[i])*(Y[i] - xBeta[i]);
                    n++;
                }
            }

            final double shape = priorShape + n / 2.0;
            final double rate = priorRate + 0.5 * SSE;

            final double draw = Randomizer.nextGamma(shape, rate); // Gamma( \alpha + n/2 , \beta + (1/2)*SSE )
            precision.setValue(k, draw);
        }

        return Double.POSITIVE_INFINITY;
    }

    public String getPerformanceSuggestion() {
        return null;
    }

    public String getOperatorName() {
        return GIBBS_OPERATOR;
    }
	

}