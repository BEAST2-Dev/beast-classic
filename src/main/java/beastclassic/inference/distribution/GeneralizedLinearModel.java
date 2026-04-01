package beastclassic.inference.distribution;



import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.SingularValueDecomposition;
import beast.base.core.BEASTInterface;
import beast.base.inference.Distribution;
import beast.base.inference.State;
import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.core.Log;
import beast.base.spec.domain.Real;
import beast.base.spec.type.RealVector;


@Description("A Generalized Linear Model")
public class GeneralizedLinearModel extends Distribution {
    public Input<RealVector<? extends Real>> dependentParamInput = new Input<>("dependentParam", "description here");
    public Input<List<RealVector<? extends Real>>> independentParamInput = new Input<>("dependentParam", "description here", new ArrayList<>());
    public Input<List<RealVector<? extends Real>>> designMatrixInput = new Input<>("dependentParam", "description here", new ArrayList<>());

    @Override
    public void initAndValidate() {
        this.dependentParam = dependentParamInput.get();

        if (dependentParam != null) {
            N = (int) dependentParam.size();
        } else
            N = 0;

        this.independentParam = independentParamInput.get();

    }

    protected RealVector<? extends Real> dependentParam;
    protected List<RealVector<? extends Real>> independentParam;
    protected List<RealVector<? extends Real>> indParamDelta;
    protected List<double[][]> designMatrix; // fixed constants, access as double[][] to save overhead

//    protected double[][] scaleDesignMatrix;
    protected int[] scaleDesign;
    protected RealVector<? extends Real> scaleParameter;

    protected int numIndependentVariables = 0;
    protected int numRandomEffects = 0;
    protected int N;

    protected List<RealVector<? extends Real>> randomEffects = null;


//    public double[][] getScaleDesignMatrix() { return scaleDesignMatrix; }
    public int[] getScaleDesign() { return scaleDesign; }

    public void addRandomEffectsParameter(RealVector<? extends Real> effect) {
        if (randomEffects == null) {
            randomEffects = new ArrayList<>();
        }
        if (N != 0 && effect.size() != N) {
            throw new RuntimeException("Random effects have the wrong dimension");
        }
        randomEffects.add(effect);
        numRandomEffects++;
    }

    public void addIndependentParameter(RealVector<? extends Real> effect, RealVector<? extends Real> matrix,
                                        int nRows, int nCols, RealVector<? extends Real> delta) {
        if (designMatrix == null)
            designMatrix = new ArrayList<>();
        if (independentParam == null)
            independentParam = new ArrayList<>();
        if (indParamDelta == null)
            indParamDelta = new ArrayList<>();

        if (N == 0) {
            N = nRows;
        }
        double [][] _matrix = new double[nRows][nCols];
        for (int i = 0; i < nRows; i++) {
            for (int j = 0; j < nCols; j++) {
                _matrix[i][j] = matrix.get(i * nCols + j);
            }
        }
        designMatrix.add(_matrix);
        independentParam.add(effect);
        indParamDelta.add(delta);

        if (designMatrix.size() != independentParam.size())
            throw new RuntimeException("Independent variables and their design matrices are out of sync");
        numIndependentVariables++;
        if (effect instanceof BEASTInterface biEffect && matrix instanceof BEASTInterface biMatrix) {
            Log.info.println("\tAdding independent predictors '" + biEffect.getID() + "' with design matrix '" + biMatrix.getID() + "'");
        }
    }

    public boolean getAllIndependentVariablesIdentifiable() {

        int totalColDim = 0;
        for (double[][] mat : designMatrix) {
            totalColDim += mat[0].length;
        }

        double[][] grandDesignMatrix = new double[N][totalColDim];

        int offset = 0;
        for (double[][] mat: designMatrix) {
            final int length = mat[0].length;
            for (int i = 0; i < N; ++i) {
                for (int j = 0; j < length; ++j) {
                    grandDesignMatrix[i][offset + j] = mat[i][j];
                }
            }
            offset += length;
        }

        SingularValueDecomposition svd = new SingularValueDecomposition(
                new Array2DRowRealMatrix(grandDesignMatrix));

        int rank = svd.getRank();
        boolean isFullRank = (totalColDim == rank);
        Logger.getLogger("dr.inference").info("\tTotal # of predictors = " + totalColDim + " and rank = " + rank);
        return isFullRank;
    }

    public int getNumberOfFixedEffects() {
        return numIndependentVariables;
    }

    public int getNumberOfRandomEffects() {
        return numRandomEffects;
    }

    public double[] getXBeta() {

        double[] xBeta = new double[N];

        for (int j = 0; j < numIndependentVariables; j++) {
            RealVector<?> beta = independentParam.get(j);
            RealVector<?> delta = indParamDelta.get(j);
            double[][] X = designMatrix.get(j);
            final int K = (int) beta.size();
            for (int k = 0; k < K; k++) {
                double betaK = beta.get(k);
                if (delta != null)
                    betaK *= delta.get(k);
                for (int i = 0; i < N; i++)
                    xBeta[i] += X[i][k] * betaK;
            }
        }

        for (int j = 0; j < numRandomEffects; j++) {
            RealVector<?> effect = randomEffects.get(j);
            for (int i = 0; i < N; i++) {
                xBeta[i] += effect.get(i);
            }
        }

        return xBeta;
    }

    public RealVector<? extends Real> getFixedEffect(int j) {
        return independentParam.get(j);
    }

    public RealVector<? extends Real> getRandomEffect(int j) {
        return randomEffects.get(j);
    }

    public RealVector<? extends Real> getDependentVariable() {
        return dependentParam;
    }

    public double[] getXBeta(int j) {

        double[] xBeta = new double[N];

        RealVector<?> beta = independentParam.get(j);
        RealVector<?> delta = indParamDelta.get(j);
        double[][] X = designMatrix.get(j);
        final int K = (int) beta.size();
        for (int k = 0; k < K; k++) {
            double betaK = beta.get(k);
            if (delta != null)
                betaK *= delta.get(k);
            for (int i = 0; i < N; i++)
                xBeta[i] += X[i][k] * betaK;
        }

        if (numRandomEffects != 0)
            throw new RuntimeException("Attempting to retrieve fixed effects without controlling for random effects");

        return xBeta;

    }

    public int getEffectNumber(RealVector<?> effect) {
        return independentParam.indexOf(effect);
    }

    public double[][] getX(int j) {
        return designMatrix.get(j);
    }


    public double[] getScale() {

        double[] scale = new double[N];

        for (int k = 0; k < N; k++)
            scale[k] = scaleParameter.get(scaleDesign[k]);

        return scale;
    }


    public double[][] getScaleAsMatrix() {
        throw new RuntimeException("Not yet implemented: GeneralizedLinearModel.getScaleAsMatrix()");
    }

    public double calculateLogLikelihood(double[] beta) {return Double.NEGATIVE_INFINITY;}

    public double calculateLogLikelihood() {return Double.NEGATIVE_INFINITY;}

    public boolean confirmIndependentParameters() {return false;}

    public boolean requiresScale() {return false;}

    public void addScaleParameter(RealVector<? extends Real> scaleParameter, RealVector<? extends Real> design) {
        this.scaleParameter = scaleParameter;
        scaleDesign = new int[(int) design.size()];
        for (int i = 0; i < scaleDesign.length; i++)
            scaleDesign[i] = (int) design.get(i);
    }

    public double evaluate(double[] beta) {
        return calculateLogLikelihood(beta);
    }

    public int getNumArguments() {
        int total = 0;
        for (RealVector<?> effect : independentParam)
            total += (int) effect.size();
        return total;
    }

    public double getLowerBound(int n) {
        int which = n;
        int k = 0;
        while (which > independentParam.get(k).size()) {
            which -= (int) independentParam.get(k).size();
            k++;
        }
        return independentParam.get(k).getLower();
    }

    public double getUpperBound(int n) {
        int which = n;
        int k = 0;
        while (which > independentParam.get(k).size()) {
            which -= (int) independentParam.get(k).size();
            k++;
        }
        return independentParam.get(k).getUpper();
    }

    public void storeState() {
        // No internal states to save
    }

    public void restoreState() {
        // No internal states to restore
    }

    public void acceptState() {
        // Nothing to do
    }

    public double getLogLikelihood() {
        return calculateLogLikelihood();
    }

    @Override
    public String toString() {
        return super.toString() + ": " + getLogLikelihood();
    }

    public void makeDirty() {
    }

    @Override
    public List<String> getArguments() {
        return null;
    }

    @Override
    public List<String> getConditions() {
        return null;
    }

    @Override
    public void sample(State state, Random random) {
    }

}
