package beastclassic.evolution.substitutionmodel;

import beast.base.evolution.substitutionmodel.EigenDecomposition;
import beast.base.evolution.substitutionmodel.EigenSystem;
import beast.base.math.matrixalgebra.RobustEigenDecomposition;
import org.apache.commons.math4.legacy.linear.Array2DRowRealMatrix;
import org.apache.commons.math4.legacy.linear.LUDecomposition;
import org.apache.commons.math4.legacy.linear.RealMatrix;

/**
 * @author Denise Kuehnert
 *         Date: Sep 22, 2011
 */
public class RobustEigenSystem implements EigenSystem {

    private final int stateCount;

    public RobustEigenSystem(int stateCount) {
        this.stateCount = stateCount;
    }

    /**
     * set instantaneous rate matrix
     */
    public EigenDecomposition decomposeMatrix(double[][] qMatrix) {

        double[][] Evec;
        double[][] Ievc;
        double[] eigenVReal;
        double[] eigenVImag;

        try {
            RobustEigenDecomposition robustEigen = new RobustEigenDecomposition(qMatrix);
            Evec = robustEigen.getV();

            RealMatrix eigenVMatrix = new Array2DRowRealMatrix(Evec);
            Ievc = new LUDecomposition(eigenVMatrix).getSolver().getInverse().getData();

            eigenVReal = robustEigen.getRealEigenvalues();
            eigenVImag = robustEigen.getImagEigenvalues();
        } catch (Exception ae) {
            throw new RuntimeException(ae);
        }

        double[] flatEvec = new double[stateCount * stateCount];
        double[] flatIevc = new double[stateCount * stateCount];

        for (int i = 0; i < stateCount; i++) {
            System.arraycopy(Evec[i], 0, flatEvec, i * stateCount, stateCount);
            System.arraycopy(Ievc[i], 0, flatIevc, i * stateCount, stateCount);
        }

        return new EigenDecomposition(flatEvec, flatIevc, eigenVReal, eigenVImag);
    }
}
