package edu.jhu.prim.matrix;

import java.io.Serializable;

public interface BinaryMatrix extends Serializable {

    public int getColumnCount(int col);
    
    public int getRowCount(int row);

    public int getNumRows();

    public int getNumColumns();

    /**
     * @return Previous value of matrix[row][col]
     */
    public boolean decrement(int row, int col);

    /**
     * @return Previous value of matrix[row][col]
     */
    public boolean increment(int row, int col);

    public boolean[][] getMatrix();

	public boolean get(int row, int col);

    public void set(BinaryMatrix matrix);
    
}