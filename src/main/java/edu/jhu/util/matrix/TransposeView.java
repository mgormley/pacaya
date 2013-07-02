package edu.jhu.util.matrix;

import edu.jhu.util.vector.IntDoubleEntry;

public class TransposeView implements DoubleMatrix {

    private static final long serialVersionUID = -3478557435074039018L;
    private DoubleMatrix mat;
    
    public TransposeView(DoubleMatrix matrix) {
        this.mat = matrix;
    }

    @Override
    public int getNumRows() {
        return mat.getNumColumns();
    }

    @Override
    public int getNumColumns() {
        return mat.getNumRows();
    }

    @Override
    public double get(int row, int col) {
        return mat.get(col, row);
    }

    @Override
    public void set(int row, int col, double value) {
        set(col, row, value);
    }

    @Override
    public void decrement(int row, int col, double decr) {
        decrement(col, row, decr);
    }

    @Override
    public void increment(int row, int col, double incr) {
        increment(col, row, incr);            
    }

    @Override
    public void set(DoubleMatrix other) {
        if (other instanceof DenseDoubleMatrix) {
            DenseDoubleMatrix dim = (DenseDoubleMatrix)other;
            int numCols = mat.getNumColumns();
            assert(numCols == dim.numRows);
            int numRows = mat.getNumRows();
            assert(numRows == dim.numCols);
            for (int row=0; row<numRows; row++) {
                for (int col=0; col<numCols; col++) {
                    mat.set(col, row, dim.get(col, row));
                }
            }
        } else {
            throw new IllegalArgumentException("unhandled type: " + other.getClass().getCanonicalName());
        }
    }

    @Override
    public Iterable<IntDoubleEntry> getRowEntries(int row) {
        // TODO Auto-generated method stub
        return null;
    }
    
}