package edu.jhu.prim.matrix.infinite;

public interface InfiniteBinaryMatrix {

    public int getColumnCount(int col);

    public int getNumRows();

    public int getCurMaxColumn();

    /**
     * @return Previous value of Z[row][col]
     */
    public boolean decrement(int row, int col);

    /**
     * @return Previous value of Z[row][col]
     */
    public boolean increment(int row, int col);

    public void incrementInactives(int row, int numInactives);

	public int incrementInactive(int row); 
	
    public boolean[][] getMatrix();

    public Iterable<Integer> getActiveColumns();

    
}