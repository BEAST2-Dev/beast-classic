package beast.inference.distribution;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import cern.colt.matrix.linalg.SingularValueDecomposition;
import beast.core.*;
import beast.core.parameter.RealParameter;
import beast.core.util.Log;


@Description("...")
public class GeneralizedLinearModel extends Distribution implements Function {
    public Input<RealParameter> dependentParamInput = new Input<RealParameter>("dependentParam", "description here");
    public Input<List<RealParameter>> independentParamInput = new Input<List<RealParameter>>("dependentParam", "description here", new ArrayList<RealParameter>());
    public Input<List<RealParameter>> designMatrixInput = new Input<List<RealParameter>>("dependentParam", "description here", new ArrayList<RealParameter>());

    @Override
    public void initAndValidate() throws Exception {
        this.dependentParam = dependentParamInput.get();

        if (dependentParam != null) {
            N = dependentParam.getDimension();
        } else
            N = 0;
        
        this.independentParam = independentParamInput.get();

    }
    
    protected RealParameter dependentParam;
    protected List<RealParameter> independentParam;
    protected List<RealParameter> indParamDelta;
    protected List<double[][]> designMatrix; // fixed constants, access as double[][] to save overhead

//    protected double[][] scaleDesignMatrix;
    protected int[] scaleDesign;
    protected RealParameter scaleParameter;

    protected int numIndependentVariables = 0;
    protected int numRandomEffects = 0;
    protected int N;

    protected List<RealParameter> randomEffects = null;


//    public double[][] getScaleDesignMatrix() { return scaleDesignMatrix; }
    public int[] getScaleDesign() { return scaleDesign; }

    public void addRandomEffectsParameter(RealParameter effect) {
        if (randomEffects == null) {
            randomEffects = new ArrayList<RealParameter>();
        }
        if (N != 0 && effect.getDimension() != N) {
            throw new RuntimeException("Random effects have the wrong dimension");
        }
        //addVariable(effect);
        randomEffects.add(effect);
        numRandomEffects++;
    }

    public void addIndependentParameter(RealParameter effect, RealParameter matrix, RealParameter delta) {
        if (designMatrix == null)
            designMatrix = new ArrayList<double[][]>();
        if (independentParam == null)
            independentParam = new ArrayList<RealParameter>();
        if (indParamDelta == null)
            indParamDelta = new ArrayList<RealParameter>();

        if (N == 0) {
            N = matrix.getMinorDimension1();
        }
        double [][] _matrix = new double[matrix.getMinorDimension1()][matrix.getMinorDimension2()];
        Double [] array = matrix.getValues();
        int dim2 = matrix.getMinorDimension2();
        for (int i = 0; i < _matrix.length; i++) {
        	System.arraycopy(array, i * dim2, _matrix[i], 0, dim2);
        }
        designMatrix.add(_matrix);
        independentParam.add(effect);
        indParamDelta.add(delta);

        if (designMatrix.size() != independentParam.size())
            throw new RuntimeException("Independent variables and their design matrices are out of sync");
        //addVariable(effect);
        //addVariable(matrix);
        //if(delta != null)
        //    addVariable(delta);
        numIndependentVariables++;
        Log.info.println("\tAdding independent predictors '" + effect.getID() + "' with design matrix '" + matrix.getID() + "'");
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
                new DenseDoubleMatrix2D(grandDesignMatrix));

        int rank = svd.rank();
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
        	RealParameter beta = independentParam.get(j);
        	RealParameter delta = indParamDelta.get(j);
            double[][] X = designMatrix.get(j);
            final int K = beta.getDimension();
            for (int k = 0; k < K; k++) {
                double betaK = beta.getArrayValue(k);
                if (delta != null)
                    betaK *= delta.getArrayValue(k);
                for (int i = 0; i < N; i++)
                    xBeta[i] += X[i][k] * betaK;
            }
        }

        for (int j=0; j<numRandomEffects; j++) {
        	RealParameter effect = randomEffects.get(j);
            for (int i=0; i<N; i++) {
                xBeta[i] += effect.getArrayValue(i);
            }
        }

        return xBeta;
    }

    public RealParameter getFixedEffect(int j) {
        return independentParam.get(j);
    }

    public RealParameter getRandomEffect(int j) {
        return randomEffects.get(j);
    }

    public RealParameter getDependentVariable() {
        return dependentParam;
    }

    public double[] getXBeta(int j) {

        double[] xBeta = new double[N];

        RealParameter beta = independentParam.get(j);
        RealParameter delta = indParamDelta.get(j);
        double[][] X = designMatrix.get(j);
        final int K = beta.getDimension();
        for (int k = 0; k < K; k++) {
            double betaK = beta.getArrayValue(k);
            if (delta != null)
                betaK *= delta.getArrayValue(k);
            for (int i = 0; i < N; i++)
                xBeta[i] += X[i][k] * betaK;
        }

        if (numRandomEffects != 0)
            throw new RuntimeException("Attempting to retrieve fixed effects without controlling for random effects");

        return xBeta;

    }

    public int getEffectNumber(RealParameter effect) {
        return independentParam.indexOf(effect);
    }

//	public double[][] getXtScaleX(int j) {
//
//		final Parameter beta = independentParam.get(j);
//		double[][] X = designMatrix.get(j);
//		final int dim = X[0].length;
//
//		if( dim != beta.getDimension() )
//			throw new RuntimeException("should have checked eariler");
//
//		double[] scale = getScale();
//
//
//	}

    public double[][] getX(int j) {
        return designMatrix.get(j);
    }


    public double[] getScale() {

        double[] scale = new double[N];

//        final int K = scaleParameter.getDimension();
//        for (int k = 0; k < K; k++) {
//            final double scaleK = scaleParameter.getParameterValue(k);
//            for (int i = 0; i < N; i++)
//                scale[i] += scaleDesignMatrix[i][k] * scaleK;
//        }
        for(int k=0; k<N; k++)
            scale[k] = scaleParameter.getArrayValue(scaleDesign[k]);

        return scale;
    }


    public double[][] getScaleAsMatrix() {

//        double[][] scale = new double[N][N];
//
//        return scale;
        throw new RuntimeException("Not yet implemented: GeneralizedLinearModel.getScaleAsMatrix()");
    }

//	protected abstract double calculateLogLikelihoodAndGradient(double[] beta, double[] gradient);

    public double calculateLogLikelihood(double[] beta) {return Double.NEGATIVE_INFINITY;}

    public double calculateLogLikelihood() {return Double.NEGATIVE_INFINITY;}

    public boolean confirmIndependentParameters() {return false;}

    public boolean requiresScale() {return false;}

    public void addScaleParameter(RealParameter scaleParameter, RealParameter design) {
        this.scaleParameter = scaleParameter;
//        this.scaleDesignMatrix = matrix.getParameterAsMatrix();
        scaleDesign = new int[design.getDimension()];
        for(int i=0; i<scaleDesign.length; i++)
            scaleDesign[i] = (int) design.getArrayValue(i);
        //addVariable(scaleParameter);
    }

/*	// **************************************************************
	// RealFunctionOfSeveralVariablesWithGradient IMPLEMENTATION
	// **************************************************************


	public double eval(double[] beta, double[] gradient) {
		return calculateLogLikelihoodAndGradient(beta, gradient);
	}


	public double eval(double[] beta) {
		return calculateLogLikelihood(beta);
	}


	public int getNumberOfVariables() {
		return independentParam.getDimension();
	}*/

    // ************
    //       Mutlivariate implementation
    // ************


    public double evaluate(double[] beta) {
        return calculateLogLikelihood(beta);
    }

    public int getNumArguments() {
        int total = 0;
        for (RealParameter effect : independentParam)
            total += effect.getDimension();
        return total;
    }

    public double getLowerBound(int n) {
        int which = n;
        int k = 0;
        while (which > independentParam.get(k).getDimension()) {
            which -= independentParam.get(k).getDimension();
            k++;
        }
        return independentParam.get(k).getLower();//getBounds().getLowerLimit(which);
    }

    public double getUpperBound(int n) {
        int which = n;
        int k = 0;
        while (which > independentParam.get(k).getDimension()) {
            which -= independentParam.get(k).getDimension();
            k++;
        }
        return independentParam.get(k).getUpper();//getBounds().getUpperLimit(which);
    }

//    protected void handleModelChangedEvent(Model model, Object object, int index) {
//
//    }
//
//    protected void handleVariableChangedEvent(Variable variable, int index, Parameter.ChangeType type) {
////        fireModelChanged();
//    }

    public void storeState() {
        // No internal states to save
    }

    public void restoreState() {
        // No internal states to restore
    }

    public void acceptState() {
        // Nothing to do
    }

//    public Model getModel() {
//        return this;
//    }

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
