package beast.continuous;

import beast.core.Description;
import beast.core.Input;
import beast.core.Input.Validate;
import beast.core.parameter.RealParameter;
import beast.evolution.substitutionmodel.*;
import beast.evolution.tree.Tree;
import dr.math.distributions.MultivariateNormalDistribution;



@Description("something to do with multi variate diffusion...")
public class MultivariateDiffusionModel extends ContinuousSubstitutionModel {
	public Input<RealParameter> diffusionPrecisionMatrixInput = new Input<RealParameter>("precisionMatrix","precision matrix for diffusion process", Validate.REQUIRED);
 
	public static final String DIFFUSION_PROCESS = "multivariateDiffusionModel";
    public static final String DIFFUSION_CONSTANT = "precisionMatrix";   
    public static final String PRECISION_TREE_ATTRIBUTE = "precision";

    public static final double LOG2PI = Math.log(2*Math.PI);

    /**
     * Construct a diffusion model.
     */
    int dimension;

    @Override
    public void initAndValidate() {
    	//diffusionPrecisionMatrix = 
        this.diffusionPrecisionMatrixParameter = diffusionPrecisionMatrixInput.get();
        dimension = (int) Math.sqrt(diffusionPrecisionMatrixParameter.getDimension());
        if (dimension * dimension != diffusionPrecisionMatrixParameter.getDimension()) {
        	throw new IllegalArgumentException ("Dimension of diffusion matrix should be a square");
        }
        calculatePrecisionInfo();
    }

    public RealParameter getPrecisionParameter() {
        return diffusionPrecisionMatrixParameter;
    }

    public double[][] getPrecisionmatrix() {
        if (diffusionPrecisionMatrixParameter != null) {
            return getParameterAsMatrix();
        }
        return null;
    }

    public double getDeterminantPrecisionMatrix() { return determinatePrecisionMatrix; }

    /**
     * @return the log likelihood of going from start to stop in the given time
     */
    public double getLogLikelihood(double[] start, double[] stop, double time) {

        if (time == 0) {
            boolean equal = true;
            for(int i=0; i<start.length; i++) {
                if( start[i] != stop[i] ) {
                    equal = false;
                    break;
                }
            }
            if (equal)
                return 0.0;
            return Double.NEGATIVE_INFINITY;                  
        }
        
        return calculateLogDensity(start, stop, time);
    }

    protected double calculateLogDensity(double[] start, double[] stop, double time) {
        final double logDet = Math.log(determinatePrecisionMatrix);
        return MultivariateNormalDistribution.logPdf(stop, start, diffusionPrecisionMatrix, logDet, time);
    }

    public double[][] getParameterAsMatrix() {
        final int I = dimension;
        final int J = dimension;
        double[][] parameterAsMatrix = new double[I][J];
        for (int i = 0; i < I; i++) {
            for (int j = 0; j < J; j++)
                parameterAsMatrix[i][j] = diffusionPrecisionMatrixParameter.getValue(i * J + j);
        }
        return parameterAsMatrix;
    }

    protected void calculatePrecisionInfo() {
        diffusionPrecisionMatrix = getParameterAsMatrix();
        determinatePrecisionMatrix =
                MultivariateNormalDistribution.calculatePrecisionMatrixDeterminate(
                        diffusionPrecisionMatrix);
    }

    @Override
    protected void store() {
        savedDeterminatePrecisionMatrix = determinatePrecisionMatrix;
        savedDiffusionPrecisionMatrix = diffusionPrecisionMatrix;
    }

    @Override
    protected void restore() {
        determinatePrecisionMatrix = savedDeterminatePrecisionMatrix;
        diffusionPrecisionMatrix = savedDiffusionPrecisionMatrix;
    }

    @Override
    protected boolean requiresRecalculation() {
        calculatePrecisionInfo();
    	return true;
    };
    
    protected void acceptState() {
    } // no additional state needs accepting

    public String[] getTreeAttributeLabel() {
        return new String[] {PRECISION_TREE_ATTRIBUTE};
    }
    
    public String toSymmetricString() {
        StringBuffer sb = new StringBuffer("{");
        int dim = dimension;
        int total = dim * (dim + 1) / 2;
        for (int i = 0; i < dim; i++) {
            for (int j = i; j < dim; j++) {
                sb.append(String.format("%5.4e", diffusionPrecisionMatrixParameter.getValue(i * dimension + j)));
                total--;
                if (total > 0)
                    sb.append(",");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public String[] getAttributeForTree(Tree tree) {
        if (diffusionPrecisionMatrixParameter != null) {
            return new String[] {toSymmetricString()};
        }
        return new String[] { "null" };
    }
  
    // **************************************************************
    // Private instance variables
    // **************************************************************

    protected RealParameter diffusionPrecisionMatrixParameter;
    private double determinatePrecisionMatrix;
    private double savedDeterminatePrecisionMatrix;
    private double[][] diffusionPrecisionMatrix;
    private double[][] savedDiffusionPrecisionMatrix;

    // TODO: should be a test, no?
    public static void main(String[] args) {
    	try {
        double[] start = {1, 2};
        double[] stop = {0, 0};
        Double[] precision = {2.0, 0.5, 0.5, 1.0};
        double scale = 0.2;
        RealParameter precMatrix = new RealParameter(precision);
        MultivariateDiffusionModel model = new MultivariateDiffusionModel();
        model.initByName("precisionMatrix", precMatrix);
        System.err.println("logPDF = " + model.calculateLogDensity(start, stop, scale));
        System.err.println("Should be -19.948");
    	} catch (Exception e) {
			e.printStackTrace();
    	}
    }
}

