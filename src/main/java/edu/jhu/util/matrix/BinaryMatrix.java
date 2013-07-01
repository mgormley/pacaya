package edu.jhu.util.matrix;

import java.io.Serializable;

public interface BinaryMatrix extends Serializable {

    public int getColumnCount(int k);
    
    public int getRowCount(int row);

    public int getNumRows();

    public int getNumColumns();

    /**
     * @return Previous value of Z[row][k]
     */
    public boolean decrement(int row, int k);

    /**
     * @return Previous value of Z[row][k]
     */
    public boolean increment(int row, int k);

    public boolean[][] getMatrix();

	public boolean get(int r, int k);

    public void set(BinaryMatrix matrix);
    
}