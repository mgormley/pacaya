package edu.jhu.pacaya.autodiff;

import java.io.Serializable;
import java.util.Arrays;

import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.Algebras;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.util.Lambda;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.prim.vector.IntDoubleVector;

/**
 * Tensor of doubles (i.e. a multi-dimensional array). In contrast to a {@link Tensor}, this class
 * is backed by an IntDoubleVector.
 * 
 * @author mgormley
 */
public class VTensor implements MVec, Serializable {

    private static final long serialVersionUID = 1L;
    protected int[] dims;
    protected int[] strides;
    protected IntDoubleVector values;
    protected final int offset;
    protected final int size;
    protected final Algebra s;
    
    /**
     * Standard constructor of multi-dimensional array.
     * @param dimensions The dimensions of this tensor.
     */
    public VTensor(Algebra s, int... dimensions) {
        this(s, 0, new IntDoubleDenseVector(IntArrays.prod(dimensions)), dimensions);
        fill(s.zero());
    }

    /** Copy constructor. */
    public VTensor(VTensor other) {
        this.dims = IntArrays.copyOf(other.dims);
        this.strides = IntArrays.copyOf(other.strides);
        this.offset = other.offset;
        this.values = other.values.copy();
        this.s = other.s;
        this.size = other.size;
    }

    /** Constructor which will be backed by the given values starting at offset. */
    public VTensor(Algebra s, int offset, IntDoubleVector values, int... dimensions) {
        // For now, we only support the reals because IntDoubleVector uses zero for missing entries.
        if (!RealAlgebra.getInstance().equals(s)) { throw new IllegalArgumentException("Unsupported Algebra: " + s); }
        this.size = IntArrays.prod(dimensions);
        this.dims = dimensions;
        this.strides = getStrides(dims);
        this.offset = offset;
        this.values = values;
        this.s = s;
    }
    
    /* --------------------- Multi-Dimensional View --------------------- */

    /** 
     * Gets the value of the entry corresponding to the given indices.
     * @param indices The indices of the multi-dimensional array.
     * @return The current value.
     */
    public double get(int... indices) {
        checkIndices(indices);
        int c = getConfigIdx(indices);
        return values.get(c);
    }

    /** 
     * Sets the value of the entry corresponding to the given indices.
     * @param indices The indices of the multi-dimensional array.
     * @param val The value to set.
     * @return The previous value.
     */
    public double set(int[] indices, double val) {
        checkIndices(indices);
        int c = getConfigIdx(indices);
        return values.set(c, val);
    }

    /** 
     * Adds the value to the entry corresponding to the given indices.
     * @param indices The indices of the multi-dimensional array.
     * @param val The value to add.
     * @return The previous value.
     */
    public double add(int[] indices, double val) {
        checkIndices(indices);
        int c = getConfigIdx(indices);
        return values.set(c, s.plus(values.get(c), val));
    }
    
    /** 
     * Subtracts the value to the entry corresponding to the given indices.
     * @param indices The indices of the multi-dimensional array.
     * @param val The value to subtract.
     * @return The previous value.
     */
    public double subtract(int[] indices, double val) {
        checkIndices(indices);
        int c = getConfigIdx(indices);
        return values.set(c, s.minus(values.get(c), val));
    }
    
    /** Convenience method for setting a value with a variable number of indices. */
    public double set(double val, int... indices) {
        return set(indices, val);
    }

    /** Convenience method for adding a value with a variable number of indices. */
    public double add(double val, int... indices) {
        return add(indices, val);
    }
    
    /** Convenience method for adding a value with a variable number of indices. */
    public double subtract(double val, int... indices) {
        return subtract(indices, val);
    }
    
    /** Gets the index into the values array that corresponds to the indices. */
    public int getConfigIdx(int... indices) {
        int c = offset;
        for (int i=0; i<indices.length; i++) {
            c += strides[i] * indices[i];
        }
        return c;
    }

    private int get1dConfigIdx(int idx) {
        return offset + idx;
    }
    
    /**
     * Gets the strides for the given dimensions. The stride for dimension i
     * (stride[i]) denotes the step forward in values array necessary to
     * increase the index for that dimension by 1.
     */
    private static int[] getStrides(int[] dims) {
        int rightmost = dims.length - 1;
        int[] strides = new int[dims.length];
        if (dims.length > 0) {          
            strides[rightmost] = 1;
            for (int i=rightmost-1; i >= 0; i--) {
                strides[i] = dims[i+1]*strides[i+1];
            }
        }
        return strides;
    }

    /** Checks that the indices are valid. */
    private void checkIndices(int... indices) {
        if (indices.length != dims.length) {
            throw new IllegalArgumentException(String.format(
                    "Indices array is not the correct length. expected=%d actual=%d", 
                    indices.length, dims.length));
        }
        for (int i=0; i<indices.length; i++) {
            if (indices[i] < 0 || dims[i] <= indices[i]) {
                throw new IllegalArgumentException(String.format(
                        "Indices array contains an index that is out of bounds: i=%d index=%d", 
                        i, indices[i]));
            }
        }
    }
    
    /* --------------------- 1-Dimensional View --------------------- */

    /**
     * Gets the value of the idx'th entry.
     */
    public double getValue(int idx) {
        return values.get(get1dConfigIdx(idx));
    }

    /** 
     * Sets the value of the idx'th entry.
     */
    public double setValue(int idx, double val) {
        return values.set(get1dConfigIdx(idx), val);
    }

    /** 
     * Adds the value to the idx'th entry.
     */
    public void addValue(int idx, double val) {
        int c = get1dConfigIdx(idx);
        values.set(c, s.plus(values.get(c), val)); 
    }

    /** 
     * Subtracts the value from the idx'th entry.
     */
    public void subtractValue(int idx, double val) {
        int c = get1dConfigIdx(idx);
        values.set(c, s.minus(values.get(c), val));
    }

    /** 
     * Multiplies the value with the idx'th entry.
     */
    public void multiplyValue(int idx, double val) {
        int c = get1dConfigIdx(idx);
        values.set(c, s.times(values.get(c), val));
    }

    /** 
     * Divides the value from the idx'th entry.
     */
    public void divideValue(int idx, double val) {
        int c = get1dConfigIdx(idx);
        values.set(c, s.divide(values.get(c), val));
    }
    
    /* --------------------- Scalar Operations --------------------- */
    
    /** Add the addend to each value. */    
    public void add(double addend) {  
        for (int c = 0; c < this.size(); c++) {
            addValue(c, addend);
        }
    }

    public void subtract(double val) {
        for (int c = 0; c < this.size(); c++) {
            subtractValue(c, val);
        }
    }
    
    /** Scale each value by lambda. */
    public void multiply(double val) {
        for (int c = 0; c < this.size(); c++) {
            multiplyValue(c, val);
        }
    }

    /** Divide each value by lambda. */
    public void divide(double val) {
        for (int c = 0; c < this.size(); c++) {
            divideValue(c, val);
        }
    }

    /** Set all the values to the given value. */
    public void fill(double val) {
        for (int c=0; c<this.size(); c++) {
            this.setValue(c, val);
        }
    }

    /* --------------------- Element-wise Operations --------------------- */

    /**
     * Adds a factor elementwise to this one.
     * @param other The addend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemAdd(VTensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.size(); c++) {
            addValue(c, other.getValue(c));
        }
    }

    /** Implements {@link MVec#elemAdd(MVec)}. */
    @Override
    public void elemAdd(MVec addend) {
        if (addend instanceof VTensor) {
            elemAdd((VTensor)addend);
        } else {
            throw new IllegalArgumentException("Addend must be of type " + this.getClass());
        }
    }
    

    /**
     * Subtracts a factor elementwise from this one.
     * @param other The subtrahend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemSubtract(VTensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.size(); c++) {
            subtractValue(c, other.getValue(c));
        }
    }

    /**
     * Multiply a factor elementwise with this one.
     * @param other The multiplier.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemMultiply(VTensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.size(); c++) {
            multiplyValue(c, other.getValue(c));
        }
    }

    /**
     * Divide a factor elementwise from this one.
     * @param other The divisor.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemDivide(VTensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.size(); c++) {
            divideValue(c, other.getValue(c));
        }
    }
    
    /**
     * Adds a factor elementwise to this one.
     * @param other The addend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemApply(Lambda.FnIntDoubleToDouble fn) {
        for (int c=0; c<this.size; c++) {
            this.setValue(c, fn.call(c, this.getValue(c)));
        }
    }

    public void elemOp(VTensor other, Lambda.LambdaBinOpDouble fn) {
        checkEqualSize(this, other);
        for (int c=0; c<this.size(); c++) {
            this.setValue(c, fn.call(this.getValue(c), other.getValue(c)));
        }
    }

    /** Take the exp of each entry. */
    public void exp() {
        for (int c=0; c<this.size(); c++) {
            this.setValue(c, s.exp(this.getValue(c)));
        }
    }

    /** Take the log of each entry. */
    public void log() {
        for (int c=0; c<this.size(); c++) {
            this.setValue(c, s.log(this.getValue(c)));
        }
    }

    /** Normalizes the values so that they sum to 1 */
    public double normalize() {
        double propSum = this.getSum();
        if (propSum == s.zero()) {
            this.fill(s.divide(s.one(), s.fromReal(this.size())));
        } else if (propSum == s.posInf()) {
            int count = count(s.posInf());
            if (count == 0) {
                throw new RuntimeException("Unable to normalize since sum is infinite but contains no infinities: " + this.toString());
            }
            double constant = s.divide(s.one(), s.fromReal(count));
            for (int d=0; d<this.size(); d++) {
                if (this.getValue(d) == s.posInf()) {
                    this.setValue(d, constant);
                } else {
                    this.setValue(d, s.zero());
                }
            }
        } else {
            this.divide(propSum);
            assert !this.containsNaN();
        }
        return propSum;
    }

    /* --------------------- Summary Statistics --------------------- */

    /** Gets the number of entries in the Tensor. */
    public int size() {
        return size;
    }
    
    /** Gets the sum of all the entries in this tensor. */
    public double getSum() {
        double sum = s.zero();
        for (int c = 0; c < this.size(); c++) {
            sum = s.plus(sum, getValue(c));
        }
        return sum;
    }

    /** Gets the product of all the entries in this tensor. */
    public double getProd() {
        double prod = s.one();
        for (int c = 0; c < this.size(); c++) {
            prod = s.times(prod, getValue(c));
        }
        return prod;
    }

    /** Gets the max value in the tensor. */
    public double getMax() {
        double max = s.minValue();
        for (int c = 0; c < this.size(); c++) {
            double val = getValue(c);
            if (s.gte(val, max)) {
                max = val;
            }
        }
        return max;
    }

    /** Gets the ID of the configuration with the maximum value. */
    public int getArgmaxConfigId() {
        int argmax = -1;
        double max = s.minValue();
        for (int c = 0; c < this.size(); c++) {
            double val = getValue(c);
            if (s.gte(val, max)) {
                max = val;
                argmax = c;
            }
        }
        return argmax;
    }

    /**
     * Gets the infinity norm of this tensor. Defined as the maximum absolute
     * value of the entries.
     */
    public double getInfNorm() {
        double max = s.negInf();
        for (int c = 0; c < this.size(); c++) {
            double abs = s.abs(getValue(c));
            if (s.gte(abs, max)) {
                max = abs;
            }
        }
        return max;
    }
    
    /** Gets the number of times a given value occurs in the Tensor (exact match). */
    public int count(double val) {
        int count = 0;
        for (int i=0; i<this.size(); i++) {
            if (this.getValue(i) == val) {
                count++;
            }
        }
        return count;
    }
    
    /** Computes the sum of the entries of the pointwise product of two tensors with identical domains. */
    public double getDotProduct(VTensor other) {
        checkEqualSize(this, other);
        double dot = s.zero();
        for (int c = 0; c < this.size(); c++) {
            dot = s.plus(dot, s.times(this.getValue(c), other.getValue(c)));
        }
        return dot;
    }
    
    /* --------------------- Reshaping --------------------- */

    /**
     * Sets the dimensions and values to be the same as the given tensor.
     * Assumes that the size of the two vectors are equal.
     */
    public void set(VTensor other) {
        checkEqualSize(this, other);
        this.dims = IntArrays.copyOf(other.dims);
        for (int c = 0; c < this.size(); c++) {
            this.setValue(c, other.getValue(c));
        }
    }

    public void setValuesOnly(VTensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.size(); c++) {
            this.setValue(c, other.getValue(c));
        }
    }

    /**
     * Selects a sub-tensor from this one. This can be though of as fixing a particular dimension to
     * a given index.
     * 
     * @param dim The dimension to treat as fixed.
     * @param idx The index of that dimension to fix.
     * @return The sub-tensor selected.
     */
    public VTensor select(int dim, int idx) {
        int[] yDims = IntArrays.removeEntry(this.getDims(), dim);
        VTensor y = new VTensor(s, yDims);
        DimIter yIter = new DimIter(y.getDims());
        while (yIter.hasNext()) {
            int[] yIdx = yIter.next();
            int[] xIdx = IntArrays.insertEntry(yIdx, dim, idx);
            y.set(yIdx, this.get(xIdx));
        }
        return y;
    }
    
    /**
     * Adds a smaller tensor to this one, inserting it at a specified dimension
     * and index. This can be thought of as selecting the sub-tensor of this tensor adding the
     * smaller tensor to it.
     * 
     * This is the larger tensor (i.e. the augend).
     * 
     * @param addend The smaller tensor (i.e. the addend)
     * @param dim The dimension which will be treated as fixed on the larger tensor.
     * @param idx The index at which that dimension will be fixed.
     */
    public void addTensor(VTensor addend, int dim, int idx) {
        checkSameAlgebra(this, addend);
        DimIter yIter = new DimIter(addend.getDims());
        while (yIter.hasNext()) {
            int[] yIdx = yIter.next();
            int[] xIdx = IntArrays.insertEntry(yIdx, dim, idx);
            this.add(xIdx, addend.get(yIdx));
        }
    }

    public static void checkEqualSize(VTensor t1, VTensor t2) {
        if (t1.size() != t2.size()) {
            throw new IllegalArgumentException("Input tensors are not the same size");
        }
        checkSameAlgebra(t1, t2);
    }

    public static void checkSameAlgebra(VTensor t1, VTensor t2) {
        if (! t1.s.equals(t2.s)) {
            throw new IllegalArgumentException("Input tensors must have the same abstract algebra: " + t1.s + " " + t2.s);
        }
    }
    
    /* --------------------- Inspection --------------------- */

    /** Special equals with a tolerance. */
    public boolean equals(VTensor other, double delta) {
        return equals(this, other, delta);
    }

    /** Special equals with a tolerance. */
    public static boolean equals(VTensor t1, VTensor t2, double delta) {
        if (t1 == t2)
            return true;
        if (t2 == null)
            return false;
        if (!Arrays.equals(t1.dims, t2.dims))
            return false;
        if (!t1.s.equals(t2.s))
            return false;
        if (t1.size() != t2.size())
            return false;
        for (int i=0; i<t1.size(); i++) {
            if (!t1.s.eq(t1.getValue(i), t2.getValue(i), delta))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Tensor (" + s + ") [\n");
        for (int i=0; i<dims.length; i++) {
            sb.append(String.format("%5s", i));
        }
        sb.append(String.format("  |  %s\n", "value"));
        DimIter iter = new DimIter(dims);
        for (int c=0; c<this.size(); c++) {
            int[] states = iter.next();
            for (int state : states) {
                sb.append(String.format("%5d", state));
            }
            sb.append(String.format("  |  %g\n", this.getValue(c)));
        }
        sb.append("]");
        return sb.toString();
    }
    
    /** Gets the internal values array. For testing only. */
    public IntDoubleVector getValues() {
        return values;
    }
    
    /** Gets a copy of the internal values as a native array. */
    public double[] getValuesAsNativeArray() {
        double[] vs = new double[this.size()];
        for (int c = 0; c < vs.length; c++) {
            vs[c] = getValue(c);
        }
        return vs;
    }

    /** Gets the internal dimensions array. */
    public int[] getDims() {
        return dims;
    }

    /** Gets the size of the i'th dimension. */
    public int getDim(int i) {
        return dims[i];
    }

    /** Gets the abstract algebra for this tensor. */
    public Algebra getAlgebra() {
        return s;
    }

    /** Returns true if this tensor contains any NaNs. */
    public boolean containsNaN() {
        for (int c = 0; c < this.size(); c++) {
            if (s.isNaN(this.getValue(c))) {
                return true;
            }
        }
        return false;
    }
    
    /* --------------------- Factory Methods --------------------- */

    /** 
     * Combines two identically sized tensors by adding an initial dimension of size 2. 
     * @param t1 The first tensor to add.
     * @param t2 The second tensor to add.
     * @return The combined tensor.
     */
    public static VTensor combine(VTensor t1, VTensor t2) {
        checkSameDims(t1, t2);
        checkSameAlgebra(t1, t2);
        
        int[] dims3 = IntArrays.insertEntry(t1.getDims(), 0, 2);
        VTensor y = new VTensor(t1.s, dims3);
        y.addTensor(t1, 0, 0);
        y.addTensor(t2, 0, 1);
        return y;
    }

    private static void checkSameDims(VTensor t1, VTensor t2) {
        if (!Arrays.equals(t1.getDims(), t2.getDims())) {
            throw new IllegalStateException("Input tensors are not the same dimension.");
        }
    }

    public static VTensor getScalarTensor(Algebra s, double val) {
        VTensor er = new VTensor(s, 1);
        er.setValue(0, val);
        return er;
    }
    
    public VTensor copy() {
        return new VTensor(this);
    }

    public VTensor zeroedCopy() {
        return copyAndFill(0);
    }

    public VTensor copyAndFill(double val) {
        VTensor other = this.copy();
        other.fill(val);
        return other;
    }

    public VTensor copyAndConvertAlgebra(Algebra newS) {
        VTensor t = new VTensor(newS, this.getDims());
        t.setFromDiffAlgebra(this);
        return t;
    }

    protected void setFromDiffAlgebra(VTensor other) {       
        for (int c=0; c<this.size(); c++) {
            this.setValue(c, Algebras.convertAlgebra(other.getValue(c), other.s, this.s));
        }
    }
    
}
