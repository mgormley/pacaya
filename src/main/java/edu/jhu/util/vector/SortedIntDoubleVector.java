package edu.jhu.util.vector;

import edu.jhu.util.Lambda;
import edu.jhu.util.SafeCast;
import edu.jhu.util.Utilities;
import edu.jhu.util.Lambda.LambdaBinOpDouble;
import edu.jhu.util.collections.PDoubleArrayList;
import edu.jhu.util.collections.PIntArrayList;

/**
 * Infinite length sparse vector.
 * 
 * @author mgormley
 *
 */
public class SortedIntDoubleVector extends SortedIntDoubleMap {

    private static final double ZERO = (double) 0;
    
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

	public SortedIntDoubleVector(double[] denseRow) {
		this(Utilities.getIndexArray(denseRow.length), denseRow);
	}

	// TODO: This could be done with a single binary search instead of two.
    public void add(int idx, double val) {
    	double curVal = getWithDefault(idx, ZERO);
    	put(idx, curVal + val);
    }
    
    public void set(int idx, double val) {
    	put(idx, val);
    }
    
    @Override
	public double get(int idx) {
		return getWithDefault(idx, ZERO);
	}
    
    public void scale(double multiplier) {
    	for (int i=0; i<used; i++) {
    		values[i] *= multiplier;
    	}
    }

    /** Computes the dot product of this vector with the given vector. */
    public double dot(double[] other) {
        double ret = 0;
        for (int c = 0; c < used && indices[c] < other.length; c++) {
            if (indices[c] > Integer.MAX_VALUE) {
                break;
            }
            ret += values[c] * other[indices[c]];
        }
        return ret;
    }

    /** Computes the dot product of this vector with the column of the given matrix. */
    public double dot(double[][] matrix, int col) {
        double ret = 0;
        for (int c = 0; c < used && indices[c] < matrix.length; c++) {
            if (indices[c] > Integer.MAX_VALUE) {
                break;
            }
            ret += values[c] * matrix[indices[c]][col];
        }
        return ret;
    }
    
    /** Computes the dot product of this vector with the given vector. */   
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
     * TODO: Make a SortedIntDoubleVectorWithExplicitZeros class and move this method there.
     * 
     * Here we override the zero method so that it doesn't set the number of
     * used values to 0. This ensures that we keep explicit zeros in.
     */
    public SortedIntDoubleVector zero() {
        java.util.Arrays.fill(values, 0);
        //used = 0;
        return this;
    }

    /** Sets all values in this vector to those in the other vector. */
    public void set(SortedIntDoubleVector other) {
        this.used = other.used;
        this.indices = Utilities.copyOf(other.indices);
        this.values = Utilities.copyOf(other.values);
    }
    
    /**
     * Computes the Hadamard product (or entry-wise product) of this vector with
     * another.
     */
    // TODO: this could just be a binaryOp call.
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

    public void add(SortedIntDoubleVector other) {
        binaryOp(other, new Lambda.DoubleAdd());
    }
    
    public void subtract(SortedIntDoubleVector other) {
        binaryOp(other, new Lambda.DoubleSubtract());
    }
    
    public void binaryOp(SortedIntDoubleVector other, LambdaBinOpDouble lambda) {
        PIntArrayList newIndices = new PIntArrayList(Math.max(this.indices.length, other.indices.length));
        PDoubleArrayList newValues = new PDoubleArrayList(Math.max(this.indices.length, other.indices.length));
        int i=0; 
        int j=0;
        while(i < this.used && j < other.used) {
            int e1 = this.indices[i];
            double v1 = this.values[i];
            int e2 = other.indices[j];
            double v2 = other.values[j];
            
            int diff = e1 - e2;
            if (diff == 0) {
                // Elements are equal. Add both of them.
                newIndices.add(e1);
                newValues.add(lambda.call(v1, v2));
                i++;
                j++;
            } else if (diff < 0) {
                // e1 is less than e2, so only add e1 this round.
                newIndices.add(e1);
                newValues.add(lambda.call(v1, ZERO));
                i++;
            } else {
                // e2 is less than e1, so only add e2 this round.
                newIndices.add(e2);
                newValues.add(lambda.call(ZERO, v2));
                j++;
            }
        }

        // If there is a list that we didn't get all the way through, add all
        // the remaining elements. There will never be more than one such list. 
        assert (!(i < this.used && j < other.used));
        for (; i < this.used; i++) {
            int e1 = this.indices[i];
            double v1 = this.values[i];
            newIndices.add(e1);
            newValues.add(lambda.call(v1, ZERO));
        }
        for (; j < other.used; j++) {
            int e2 = other.indices[j];
            double v2 = other.values[j];
            newIndices.add(e2);
            newValues.add(lambda.call(ZERO, v2));
        }
        
        this.used = newIndices.size();
        this.indices = newIndices.toNativeArray();
        this.values = newValues.toNativeArray();
    }
    
    /**
     * Counts the number of unique indices in two arrays.
     * @param indices1 Sorted array of indices.
     * @param indices2 Sorted array of indices.
     */
    public static int countUnique(int[] indices1, int[] indices2) {
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
    
    public SortedIntDoubleVector getElementwiseSum(SortedIntDoubleVector other) {
        SortedIntDoubleVector sum = new SortedIntDoubleVector(this);
        sum.add(other);
        return sum;
    }
    
    public SortedIntDoubleVector getElementwiseDiff(SortedIntDoubleVector other) {
        SortedIntDoubleVector sum = new SortedIntDoubleVector(this);
        sum.subtract(other);
        return sum;
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
        // This is slow, but correct.
        SortedIntDoubleVector v1 = SortedIntDoubleVector.getWithNoZeroValues(this, delta);
        SortedIntDoubleVector v2 = SortedIntDoubleVector.getWithNoZeroValues(other, delta);
                
        if (v2.size() != v1.size()) {
            return false;
        }

        for (IntDoubleEntry ve : v1) {
            if (!Utilities.equals(ve.get(), v2.get(ve.index()), delta)) {
                return false;
            }
        }
        for (IntDoubleEntry ve : v2) {
            if (!Utilities.equals(ve.get(), v1.get(ve.index()), delta)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        throw new RuntimeException("not implemented");
    }

}
