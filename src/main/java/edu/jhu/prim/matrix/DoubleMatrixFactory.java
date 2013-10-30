package edu.jhu.prim.matrix;

public class DoubleMatrixFactory {

    public static DenseDoubleMatrix getDenseDoubleIdentity(int dimension) {
        DenseDoubleMatrix matrix = new DenseDoubleMatrix(dimension, dimension);
        for (int i=0; i<dimension; i++) {
            matrix.set(i, i, 1.0);
        }
        return matrix;
    }
    
}
