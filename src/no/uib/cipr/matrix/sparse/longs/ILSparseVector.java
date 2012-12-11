/**
 * May 21, 2007
 * @author Samuel Halliday, ThinkTank Maths Limited
 * Copyright ThinkTank Maths Limited 2007
 */
package no.uib.cipr.matrix.sparse.longs;


/**
 * @author Samuel Halliday, ThinkTank Maths Limited
 */
public interface ILSparseVector extends LVector {

	/**
	 * Returns the indices
	 */
	public long[] getIndex();

	/**
	 * Number of entries used in the sparse structure
	 */
	public int getUsed();
}
