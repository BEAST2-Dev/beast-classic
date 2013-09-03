package beast.evolution.substitutionmodel;

import beast.evolution.substitutionmodel.EigenDecomposition;
import beast.evolution.substitutionmodel.EigenSystem;
import beast.math.matrixalgebra.RobustEigenDecomposition;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.matrix.linalg.Property;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * @author Denise Kuehnert
 *         Date: Sep 22, 2011
 *         Time: 2:30:18 PM
 */
public class RobustEigenSystem implements EigenSystem{

    private final int stateCount;

    public RobustEigenSystem(int stateCount) {

        this.stateCount = stateCount;

    }

    /**
     * set instantaneous rate matrix
     */
    public EigenDecomposition decomposeMatrix(double[][] qMatrix) {

        try {
            robustEigen = new RobustEigenDecomposition(new DenseDoubleMatrix2D(qMatrix));
            eigenV = robustEigen.getV(); // Eigenvector matrix
            eigenVInv = algebra.inverse(eigenV);
        } catch (Exception ae) {
            throw new RuntimeException(ae);
//            System.err.println("amat = \n" + new Matrix(qMatrix));
        }

        double[][] Evec = eigenV.toArray();
        double[][] Ievc = eigenVInv.toArray();

        eigenVReal = robustEigen.getRealEigenvalues();
        eigenVImag = robustEigen.getImagEigenvalues();

        double[] flatEvec = new double[stateCount * stateCount];
        double[] flatIevc = new double[stateCount * stateCount];

         for (int i = 0; i < stateCount; i++) {
             System.arraycopy(Evec[i], 0, flatEvec, i * stateCount, stateCount);
             System.arraycopy(Ievc[i], 0, flatIevc, i * stateCount, stateCount);
         }

        return new EigenDecomposition(flatEvec, flatIevc, eigenVReal.toArray(), eigenVImag.toArray() );
    }

    // Eigenvalues, eigenvectors, and inverse eigenvectors
    DoubleMatrix2D eigenV;
    DoubleMatrix1D eigenVReal;
    DoubleMatrix1D eigenVImag;
    DoubleMatrix2D eigenVInv;
    RobustEigenDecomposition robustEigen;

    protected static final double minProb = Property.DEFAULT.tolerance();
    private static final Algebra algebra = new Algebra(minProb);

}