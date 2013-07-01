package edu.jhu.util.matrix.infinite;

public interface InfiniteBinaryMatrix {

    public int getColumnCount(int k);

    public int getNumRows();

    public int getCurMaxColumn();

    /**
     * @return Previous value of Z[row][k]
     */
    public boolean decrement(int row, int k);

    /**
     * @return Previous value of Z[row][k]
     */
    public boolean increment(int row, int k);

    public void incrementInactives(int row, int numInactives);

	public int incrementInactive(int row); 
	
    public boolean[][] getMatrix();

    public Iterable<Integer> getActiveColumns();

    
}