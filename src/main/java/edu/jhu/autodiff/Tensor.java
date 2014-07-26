package edu.jhu.autodiff;

import java.util.Arrays;

import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.util.Lambda;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

/**
 * Tensor of doubles (i.e. a multi-dimensional array).
 * 
 * @author mgormley
 */
public class Tensor implements MVec<Tensor> {

    protected int[] dims;
    protected int[] strides;
    protected double[] values;
    protected final Algebra s;
    
    /**
     * Standard constructor of multi-dimensional array.
     * @param dimensions The dimensions of this tensor.
     */
    public Tensor(Algebra s, int... dimensions) {
        int numConfigs = IntArrays.prod(dimensions);
        this.dims = dimensions;
        this.strides = getStrides(dims);
        this.values = new double[numConfigs];
        this.s = s;
    }

    /** Copy constructor. */
    public Tensor(Tensor other) {
        this.dims = IntArrays.copyOf(other.dims);
        this.strides = IntArrays.copyOf(other.strides);
        this.values = DoubleArrays.copyOf(other.values);
        this.s = other.s;
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
        return values[c];
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
        double prev = values[c];
        values[c] = val;
        return prev;
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
        double prev = values[c];
        values[c] = s.plus(values[c], val);
        return prev;
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
        double prev = values[c];
        values[c] = s.minus(values[c], val);
        return prev;
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
        int c = 0;
        for (int i=0; i<indices.length; i++) {
            c += strides[i] * indices[i];
        }
        return c;
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
            if (indices[i] >= dims[i]) {
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
        return values[idx];
    }

    /** 
     * Sets the value of the idx'th entry.
     */
    public double setValue(int idx, double val) {
        double prev = values[idx];
        values[idx] = val;
        return prev;
    }

    /** 
     * Adds the value to the idx'th entry.
     */
    public void addValue(int idx, double val) {
        values[idx] = s.plus(values[idx], val); 
    }

    /** 
     * Subtracts the value from the idx'th entry.
     */
    public void subtractValue(int idx, double val) {
        values[idx] = s.minus(values[idx], val); 
    }

    /** 
     * Multiplies the value with the idx'th entry.
     */
    public void multiplyValue(int idx, double val) {
        values[idx] = s.times(values[idx], val); 
    }

    /** 
     * Divides the value from the idx'th entry.
     */
    public void divideValue(int idx, double val) {
        values[idx] = s.divide(values[idx], val); 
    }
    
    /* --------------------- Scalar Operations --------------------- */
    
    /** Add the addend to each value. */    
    public void add(double addend) {  
        for (int c = 0; c < this.values.length; c++) {
            addValue(c, addend);
        }
    }

    public void subtract(double val) {
        for (int c = 0; c < this.values.length; c++) {
            subtractValue(c, val);
        }
    }
    
    /** Scale each value by lambda. */
    public void multiply(double val) {
        for (int c = 0; c < this.values.length; c++) {
            multiplyValue(c, val);
        }
    }

    /** Divide each value by lambda. */
    public void divide(double val) {
        for (int c = 0; c < this.values.length; c++) {
            divideValue(c, val);
        }
    }

    /** Set all the values to the given value. */
    public void fill(double val) {
        Arrays.fill(values, val);
    }

    /* --------------------- Element-wise Operations --------------------- */

    /**
     * Adds a factor elementwise to this one.
     * @param other The addend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemAdd(Tensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.values.length; c++) {
            addValue(c, other.values[c]);
        }
    }

    /**
     * Subtracts a factor elementwise from this one.
     * @param other The subtrahend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemSubtract(Tensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.values.length; c++) {
            subtractValue(c, other.values[c]);
        }
    }

    /**
     * Multiply a factor elementwise with this one.
     * @param other The multiplier.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemMultiply(Tensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.values.length; c++) {
            multiplyValue(c, other.values[c]);
        }     
    }

    /**
     * Divide a factor elementwise from this one.
     * @param other The divisor.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemDivide(Tensor other) {
        checkEqualSize(this, other);
        for (int c = 0; c < this.values.length; c++) {
            divideValue(c, other.values[c]);
        }
    }
    
    /**
     * Adds a factor elementwise to this one.
     * @param other The addend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemApply(Lambda.FnIntDoubleToDouble fn) {
        for (int c=0; c<this.values.length; c++) {
            this.values[c] = fn.call(c, this.values[c]);
        }
    }
    
    public void elemOp(Tensor other, Lambda.LambdaBinOpDouble fn) {
        checkEqualSize(this, other);
        for (int c=0; c<this.values.length; c++) {
            this.values[c] = fn.call(this.values[c], other.values[c]);
        }
    }

    /** Take the exp of each entry. */
    public void exp() {
        for (int c=0; c<this.values.length; c++) {
            this.values[c] = s.exp(this.values[c]);
        }
    }
    
    /** Take the log of each entry. */
    public void log() {
        for (int c=0; c<this.values.length; c++) {
            this.values[c] = s.log(this.values[c]);
        }
    }
    
    /* --------------------- Summary Statistics --------------------- */

    /** Gets the number of entries in the Tensor. */
    public int size() {
        return values.length;
    }
    
    /** Gets the sum of all the entries in this tensor. */
    public double getSum() {
        double sum = s.zero();
        for (int c = 0; c < this.values.length; c++) {
            sum = s.plus(sum, values[c]);
        }
        return sum;
    }

    /** Gets the product of all the entries in this tensor. */
    public double getProd() {
        double prod = s.one();
        for (int c = 0; c < this.values.length; c++) {
            prod = s.times(prod, values[c]);
        }
        return prod;
    }

    /** Gets the max value in the tensor. */
    public double getMax() {
        double max = s.minValue();
        for (int c = 0; c < this.values.length; c++) {
            if (s.gte(values[c], max)) {
                max = values[c];
            }
        }
        return max;
    }

    /** Gets the ID of the configuration with the maximum value. */
    public int getArgmaxConfigId() {
        int argmax = -1;
        double max = s.minValue();
        for (int c = 0; c < this.values.length; c++) {
            if (s.gte(values[c], max)) {
                max = values[c];
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
        for (int c = 0; c < this.values.length; c++) {
            double abs = s.abs(values[c]);
            if (s.gte(abs, max)) {
                max = abs;
            }
        }
        return max;
    }

    /** Computes the sum of the entries of the pointwise product of two tensors with identical domains. */
    public double getDotProduct(Tensor other) {
        checkEqualSize(this, other);
        double dot = s.zero();
        for (int c = 0; c < this.values.length; c++) {
            dot = s.plus(dot, s.times(this.values[c], other.values[c]));
        }
        return dot;
    }
    
    /* --------------------- Reshaping --------------------- */

    /**
     * Sets the dimensions and values to be the same as the given tensor.
     * Assumes that the size of the two vectors are equal.
     */
    public void set(Tensor other) {
        checkEqualSize(this, other);
        this.dims = IntArrays.copyOf(other.dims);
        DoubleArrays.copy(other.values, this.values);
    }

    public void setValuesOnly(Tensor other) {
        checkEqualSize(this, other);
        DoubleArrays.copy(other.values, this.values);
    }

    /**
     * Selects a sub-tensor from this one. This can be though of as fixing a particular dimension to
     * a given index.
     * 
     * @param dim The dimension to treat as fixed.
     * @param idx The index of that dimension to fix.
     * @return The sub-tensor selected.
     */
    public Tensor select(int dim, int idx) {
        int[] yDims = IntArrays.removeEntry(this.getDims(), dim);
        Tensor y = new Tensor(s, yDims);
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
    public void addTensor(Tensor addend, int dim, int idx) {
        checkSameAlgebra(this, addend);
        DimIter yIter = new DimIter(addend.getDims());
        while (yIter.hasNext()) {
            int[] yIdx = yIter.next();
            int[] xIdx = IntArrays.insertEntry(yIdx, dim, idx);
            this.add(xIdx, addend.get(yIdx));
        }
    }

    public static void checkEqualSize(Tensor t1, Tensor t2) {
        if (t1.size() != t2.size()) {
            throw new IllegalArgumentException("Input tensors are not the same size");
        }
        checkSameAlgebra(t1, t2);
    }

    public static void checkSameAlgebra(Tensor t1, Tensor t2) {
        if (! t1.s.equals(t2.s)) {
            throw new IllegalArgumentException("Input tensors must have the same abstract algebra: " + t1.s + " " + t2.s);
        }
    }
    
    /* --------------------- Inspection --------------------- */

    /** Special equals with a tolerance. */
    public boolean equals(Tensor other, double delta) {
        if (this == other)
            return true;
        if (other == null)
            return false;
        if (!Arrays.equals(dims, other.dims))
            return false;
        if (this.values.length != other.values.length)
            return false;
        for (int i=0; i<values.length; i++) {
            if (!s.eq(values[i], other.values[i], delta))
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
        for (int c=0; c<values.length; c++) {
            int[] states = iter.next();
            for (int state : states) {
                sb.append(String.format("%5d", state));
            }
            sb.append(String.format("  |  %g\n", values[c]));
        }
        sb.append("]");
        return sb.toString();
    }
    
    /** Gets the internal values array. For testing only. */
    public double[] getValues() {
        return values;
    }

    /** Gets the internal dimensions array. */
    public int[] getDims() {
        return dims;
    }

    /** Gets the abstract algebra for this tensor. */
    public Algebra getAlgebra() {
        return s;
    }

    /** Returns true if this tensor contains any NaNs. */
    public boolean containsNaN() {
        for (int i = 0; i < values.length; i++) {
            if (s.isNaN(values[i])) {
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
    public static Tensor combine(Tensor t1, Tensor t2) {
        checkSameDims(t1, t2);
        checkSameAlgebra(t1, t2);
        
        int[] dims3 = IntArrays.insertEntry(t1.getDims(), 0, 2);
        Tensor y = new Tensor(t1.s, dims3);
        y.addTensor(t1, 0, 0);
        y.addTensor(t2, 0, 1);
        return y;
    }

    private static void checkSameDims(Tensor t1, Tensor t2) {
        if (!Arrays.equals(t1.getDims(), t2.getDims())) {
            throw new IllegalStateException("Input tensors are not the same dimension.");
        }
    }

    public static Tensor getScalarTensor(Algebra s, double val) {
        Tensor er = new Tensor(s, 1);
        er.setValue(0, val);
        return er;
    }
    
    public Tensor copy() {
        return new Tensor(this);
    }

    public Tensor zeroedCopy() {
        return copyAndFill(0);
    }

    public Tensor copyAndFill(double val) {
        Tensor other = this.copy();
        other.fill(val);
        return other;
    }

    public Tensor copyAndConvertAlgebra(Algebra newS) {
        Tensor t = new Tensor(newS, this.getDims());
        t.setFromDiffAlgebra(this);
        return t;
    }

    protected void setFromDiffAlgebra(Tensor other) {       
        for (int c=0; c<this.values.length; c++) {
            this.values[c] = Algebras.convertAlgebra(other.values[c], other.s, this.s);
        }
    }
    
}
