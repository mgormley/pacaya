package edu.jhu.hltcoe.util.vector;

import edu.jhu.hltcoe.util.Sort;
import edu.jhu.hltcoe.util.Utilities;

/**
 * Infinite length sparse vector.
 * 
 * @author mgormley
 *
 */
public class SortedLongDoubleVector extends SortedLongDoubleMap {

    boolean norm2Cached = false;
    double norm2Value;
    
    public SortedLongDoubleVector() {
        super();
    }
    
    public SortedLongDoubleVector(long[] index, double[] data) {
    	super(index, data);
	}
    
    public SortedLongDoubleVector(SortedLongDoubleVector vector) {
    	super(vector);
    }

	public SortedLongDoubleVector(double[] denseRow) {
		this(Utilities.getLongIndexArray(denseRow.length), denseRow);
	}

	// TODO: This could be done with a single binary search instead of two.
    public void add(long idx, double val) {
    	double curVal = getWithDefault(idx, 0.0);
    	put(idx, curVal + val);
    }
    
    public void set(long idx, double val) {
    	put(idx, val);
    }
    
    @Override
	public double get(long idx) {
		return getWithDefault(idx, 0.0);
	}
    
    public void scale(double multiplier) {
    	for (int i=0; i<used; i++) {
    		values[i] *= multiplier;
    	}
    }

	public void add(SortedLongDoubleVector other) {
		// TODO: this could be done much faster with a merge of the two arrays.
		for (LongDoubleEntry ve : other) {
			add(ve.index(), ve.get());
		}
	}
	
	public void set(SortedLongDoubleVector other) {
	    //int numUniqueIndices = countUnique(this.indices, other.indices);
	    //long[] tmpIndices = new long[numUniqueIndices];
	    //double[] tmpValues = new double[numUniqueIndices];
		// TODO: this could be done much faster with a merge of the two arrays.
		for (LongDoubleEntry ve : other) {
			set(ve.index(), ve.get());
		}
	}
	
	/**
	 * Counts the number of unique indices in two arrays.
	 * @param indices1 Sorted array of indices.
	 * @param indices2 Sorted array of indices.
	 */
	public static int countUnique(long[] indices1, long[] indices2) {
        int numUniqueIndices = 0;
        int i = 0;
        int j = 0;
        while (i < indices1.length && j < indices2.length) {
            if (indices1[i] < indices2[j]) {
                numUniqueIndices++;
                i++;
            } else if (indices2[j] < indices1[i]) {
                numUniqueIndices++;
                j++;
            } else {
                // Equal indices.
                i++;
                j++;
            }
        }
        for (; i < indices1.length; i++) {
            numUniqueIndices++;
        }
        for (; j < indices2.length; j++) {
            numUniqueIndices++;
        }
        return numUniqueIndices;
	}

    public double dot(SortedLongDoubleVector y) {
        if (y instanceof SortedLongDoubleVector) {
            SortedLongDoubleVector other = ((SortedLongDoubleVector) y);
            double ret = 0;
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
    public static SortedLongDoubleVector getWithNoZeroValues(SortedLongDoubleVector row, double zeroThreshold) {
        long[] origIndex = row.getIndices();
        double[] origData = row.getValues();
        
        // Count and keep track of nonzeros.
        int numNonZeros = 0;
        boolean[] isNonZero = new boolean[row.getUsed()];
        for (int i = 0; i < row.getUsed(); i++) {
            double absVal = Math.abs(origData[i]);
            if (absVal < -zeroThreshold || zeroThreshold < absVal) {
                isNonZero[i] = true;
                numNonZeros++;
            } else {
                isNonZero[i] = false;
            }
        }
        int numZeros = row.getUsed() - numNonZeros;
        
        if (numZeros > 0) {
            // Create the new vector without the zeros.
            long[] newIndex = new long[numNonZeros];
            double[] newData = new double[numNonZeros];

            int newIdx = 0;
            for (int i = 0; i < row.getUsed(); i++) {
                if (isNonZero[i]) {
                    newIndex[newIdx] = origIndex[i];
                    newData[newIdx] = origData[i];
                    newIdx++;
                }
            }
            return new SortedLongDoubleVector(newIndex, newData);
        } else {
            return row;
        }
    }
    

    /**
     * TODO: Make a SortedLongDoubleVectorWithExplicitZeros class and move this method there.
     * 
     * Here we override the zero method so that it doesn't set the number of
     * used values to 0. This ensures that we keep explicit zeros in.
     */
    public SortedLongDoubleVector zero() {
        java.util.Arrays.fill(values, 0);
        //used = 0;
        return this;
    }

    /**
     * Computes the Hadamard product (or entry-wise product) of this vector with
     * another.
     */
    public SortedLongDoubleVector hadamardProd(SortedLongDoubleVector other) {
    	SortedLongDoubleVector ip = new SortedLongDoubleVector();
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
    public boolean equals(SortedLongDoubleVector other, double delta) {
        // This is slow, but correct.
        SortedLongDoubleVector v1 = SortedLongDoubleVector.getWithNoZeroValues(this, delta);
        SortedLongDoubleVector v2 = SortedLongDoubleVector.getWithNoZeroValues(other, delta);
                
        if (v2.size() != v1.size()) {
            return false;
        }

        for (LongDoubleEntry ve : v1) {
            if (!Utilities.equals(ve.get(), v2.get(ve.index()), delta)) {
                return false;
            }
        }
        for (LongDoubleEntry ve : v2) {
            if (!Utilities.equals(ve.get(), v1.get(ve.index()), delta)) {
                return false;
            }
        }
        return true;
    }

}
