package edu.jhu.hltcoe.util.vector;

import edu.jhu.hltcoe.util.Utilities;

/**
 * Infinite length sparse vector.
 * 
 * @author mgormley
 *
 */
public class SortedIntDoubleVector extends SortedIntDoubleMap {

    boolean norm2Cached = false;
    double norm2Value;
    
    public SortedIntDoubleVector() {
        super();
    }
    
    public SortedIntDoubleVector(int[] index, double[] data) {
    	super(index, data);
	}
    
    public SortedIntDoubleVector(SortedIntDoubleVector vector) {
    	super(vector);
    }

	// TODO: This could be done with a single binary search instead of two.
    public void add(int idx, double val) {
    	double curVal = getWithDefault(idx, 0.0);
    	put(idx, curVal + val);
    }
    
    public void set(int idx, double val) {
    	put(idx, val);
    }
    
    @Override
	public double get(int idx) {
		return getWithDefault(idx, 0.0);
	}
    
    public void scale(double multiplier) {
    	for (int i=0; i<used; i++) {
    		values[i] *= multiplier;
    	}
    }

	public void set(SortedIntDoubleVector other) {
		// TODO: this could be done much faster with a merge of the two arrays.
		for (IntDoubleEntry ve : other) {
			set(ve.index(), ve.get());
		}
	}

    public double dot(SortedIntDoubleVector y) {
        if (y instanceof SortedIntDoubleVector) {
            SortedIntDoubleVector other = ((SortedIntDoubleVector) y);
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
    public static SortedIntDoubleVector getWithNoZeroValues(SortedIntDoubleVector row, double zeroThreshold) {
        int[] origIndex = row.getIndices();
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
            int[] newIndex = new int[numNonZeros];
            double[] newData = new double[numNonZeros];

            int newIdx = 0;
            for (int i = 0; i < row.getUsed(); i++) {
                if (isNonZero[i]) {
                    newIndex[newIdx] = origIndex[i];
                    newData[newIdx] = origData[i];
                    newIdx++;
                }
            }
            return new SortedIntDoubleVector(newIndex, newData);
        } else {
            return row;
        }
    }
    

    /**
     * TODO: Make a FastSparseLVectorWithExplicitZeros class and move this method there.
     * 
     * Here we override the zero method so that it doesn't set the number of
     * used values to 0. This ensures that we keep explicit zeros in.
     */
    public SortedIntDoubleVector zero() {
        java.util.Arrays.fill(values, 0);
        //used = 0;
        return this;
    }

    /**
     * Computes the Hadamard product (or entry-wise product) of this vector with
     * another.
     */
    public SortedIntDoubleVector hadamardProd(SortedIntDoubleVector other) {
    	SortedIntDoubleVector ip = new SortedIntDoubleVector();
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
    public boolean equals(SortedIntDoubleVector other, double delta) {
        if (other.size() != this.size()) {
            return false;
        }
        // This is slow, but correct.
        for (IntDoubleEntry ve : this) {
            if (!Utilities.equals(ve.get(), other.get(ve.index()), delta)) {
                return false;
            }
        }
        for (IntDoubleEntry ve : other) {
            if (!Utilities.equals(ve.get(), this.get(ve.index()), delta)) {
                return false;
            }
        }
        return true;
    }

}
