package edu.jhu.hltcoe.util.vector;

import edu.jhu.hltcoe.util.Utilities;


/**
 * Infinite length sparse vector.
 * 
 * @author mgormley
 *
 */
public class SortedIntLongVector extends SortedIntLongMap {

    boolean norm2Cached = false;
    double norm2Value;
    
    public SortedIntLongVector() {
        super();
    }
    
    public SortedIntLongVector(int[] index, long[] data) {
    	super(index, data);
	}
    
    public SortedIntLongVector(SortedIntLongVector vector) {
    	super(vector);
    }

	public SortedIntLongVector(long[] denseRow) {
		this(Utilities.getIndexArray(denseRow.length), denseRow);
	}
	
	// TODO: This could be done with a single binary search instead of two.
    public void add(int idx, long val) {
    	long curVal = getWithDefault(idx, 0);
    	put(idx, curVal + val);
    }
    
    public void set(int idx, long val) {
    	put(idx, val);
    }
    
    @Override
	public long get(int idx) {
		return getWithDefault(idx, 0);
	}
    
    public void scale(long multiplier) {
    	for (int i=0; i<used; i++) {
    		values[i] *= multiplier;
    	}
    }

    public void add(SortedIntLongVector other) {
        // TODO: this could be done much faster with a merge of the two arrays.
        for (IntLongEntry ve : other) {
            add(ve.index(), ve.get());
        }
    }
    
	public void set(SortedIntLongVector other) {
		// TODO: this could be done much faster with a merge of the two arrays.
		for (IntLongEntry ve : other) {
			set(ve.index(), ve.get());
		}
	}

    public long dot(SortedIntLongVector y) {
        if (y instanceof SortedIntLongVector) {
            SortedIntLongVector other = ((SortedIntLongVector) y);
            long ret = 0;
            int oc = 0;
            for (int c = 0; c < used; c++) {
                while (oc < other.used) {
                    if (other.indices[oc] < indices[c]) {
                        oc++;
                    } else if (indices[c] == other.indices[oc]) {
                        ret += values[c] * other.values[oc];
                        break;
                    } else {
                        break;
                    }
                }
            }
            return ret;
        } else {
        	throw new IllegalArgumentException("Unhandled type: " + y.getClass());
        }
    }
    

    /**
     * @return A new vector without zeros OR the same vector if it has none.
     */
    public static SortedIntLongVector getWithNoZeroValues(SortedIntLongVector row) {
        int[] origIndex = row.getIndices();
        long[] origData = row.getValues();
        
        // Count and keep track of nonzeros.
        int numNonZeros = 0;
        boolean[] isNonZero = new boolean[row.getUsed()];
        for (int i = 0; i < row.getUsed(); i++) {
            if (origData[i] != 0) {
                isNonZero[i] = true;
                numNonZeros++;
            } else {
                isNonZero[i] = false;
            }
        }
        int numZeros = row.getUsed() - numNonZeros;
        
        if (numZeros > 0) {
            // Create the new vector without the zeros.
            int[] newIndex = new int[numNonZeros];
            long[] newData = new long[numNonZeros];

            int newIdx = 0;
            for (int i = 0; i < row.getUsed(); i++) {
                if (isNonZero[i]) {
                    newIndex[newIdx] = origIndex[i];
                    newData[newIdx] = origData[i];
                    newIdx++;
                }
            }
            return new SortedIntLongVector(newIndex, newData);
        } else {
            return row;
        }
    }
    

    /**
     * TODO: Make a SortedIntLongVectorWithExplicitZeros class and move this method there.
     * 
     * Here we override the zero method so that it doesn't set the number of
     * used values to 0. This ensures that we keep explicit zeros in.
     */
    public SortedIntLongVector zero() {
        java.util.Arrays.fill(values, 0);
        //used = 0;
        return this;
    }

    /**
     * Computes the Hadamard product (or entry-wise product) of this vector with
     * another.
     */
    public SortedIntLongVector hadamardProd(SortedIntLongVector other) {
    	SortedIntLongVector ip = new SortedIntLongVector();
        int oc = 0;
        for (int c = 0; c < used; c++) {
            while (oc < other.used) {
                if (other.indices[oc] < indices[c]) {
                    oc++;
                } else if (indices[c] == other.indices[oc]) {
                    ip.set(indices[c], values[c] * other.values[oc]);
                    break;
                } else {
                    break;
                }
            }
        }
        return ip;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < used; i++) {
            sb.append(indices[i]);
            sb.append(":");
            sb.append(values[i]);
            if (i + 1 < used) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * Returns true if the input vector is equal to this one.
     */
    @Override
    public boolean equals(Object obj) {
    	if (obj instanceof SortedIntLongVector) {
    		SortedIntLongVector other = (SortedIntLongVector) obj;

	        if (other.size() != this.size()) {
	            return false;
	        }
	        // This is slow, but correct.
	        for (IntLongEntry ve : this) {
	            if (ve.get() != other.get(ve.index())) {
	                return false;
	            }
	        }
	        for (IntLongEntry ve : other) {
	            if (ve.get() != this.get(ve.index())) {
	                return false;
	            }
	        }
	        return true;
    	}
    	return false;
    }
    
    @Override
    public int hashCode() {
    	throw new RuntimeException("not implemented");
    }

}
