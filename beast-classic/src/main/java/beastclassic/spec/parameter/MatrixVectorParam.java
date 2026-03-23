package beastclassic.spec.parameter;

import beast.base.core.Description;
import beast.base.core.Input;
import beast.base.spec.domain.Real;
import beast.base.spec.inference.parameter.RealVectorParam;

/**
 * A RealVectorParam that interprets its flat array as a matrix with a given
 * number of columns (minor dimension). Provides matrix-style access methods
 * (getMatrixValue, setMatrixValue, getMatrixValues1) needed by the continuous
 * trait likelihood classes.
 *
 * @param <D> the real domain type
 */
@Description("A real-valued vector parameter with matrix (minor dimension) interpretation")
public class MatrixVectorParam<D extends Real> extends RealVectorParam<D> {

    public final Input<Integer> minorDimensionInput = new Input<>("minordimension",
            "minor-dimension when the parameter is interpreted as a matrix (default 1)", 1);

    protected int minorDimension = 1;

    @Override
    public void initAndValidate() {
        super.initAndValidate();
        minorDimension = minorDimensionInput.get();
        if (minorDimension > 1 && size() > 0 && size() % minorDimension != 0) {
            throw new IllegalArgumentException("Dimension (" + size() +
                    ") must be divisible by minordimension (" + minorDimension + ")");
        }
    }

    /**
     * @return the number of columns (minor dimension)
     */
    public int getMinorDimension1() {
        return minorDimension;
    }

    /**
     * @return the number of rows (dimension / minorDimension)
     */
    public int getMinorDimension2() {
        return size() / minorDimension;
    }

    /**
     * Get a matrix element.
     * @param row the row index
     * @param column the column index
     * @return value at (row, column)
     */
    public double getMatrixValue(int row, int column) {
        return get(row * minorDimension + column);
    }

    /**
     * Set a matrix element.
     * @param row the row index
     * @param column the column index
     * @param value the value to set
     */
    public void setMatrixValue(int row, int column, double value) {
        set(row * minorDimension + column, value);
    }

    /**
     * Copy a row of the matrix into the given array.
     * @param row the row index
     * @param dest destination array (must have length == minorDimension)
     */
    public void getMatrixValues1(int row, double[] dest) {
        for (int j = 0; j < minorDimension; j++) {
            dest[j] = get(row * minorDimension + j);
        }
    }

    public void setMinorDimension(int dim) {
        minorDimension = dim;
    }
}
