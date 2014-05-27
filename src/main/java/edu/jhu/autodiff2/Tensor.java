package edu.jhu.autodiff2;

import java.util.Arrays;

import edu.jhu.gm.model.IndexFor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.IntArrays;
import edu.jhu.prim.util.Lambda;


public class Tensor {

    private int[] dims;
    private double[] values;

    /**
     * Standard constructor of multi-dimensional array.
     * @param dimensions The dimensions of this tensor.
     */
    public Tensor(int... dimensions) {
        int numConfigs = IntArrays.prod(dimensions);
        this.dims = dimensions;
        this.values = new double[numConfigs];
    }

    /** Copy constructor. */
    public Tensor(Tensor input) {
        this.dims = IntArrays.copyOf(dims);
        this.values = DoubleArrays.copyOf(values);
    }

    /**
     * Gets the value of the c'th entry.
     * @param idx The index, c
     */
    public double getValue(int idx) {
        return values[idx];
    }

    /** 
     * Sets the value of the c'th entry.
     * @param The index, c 
     */
    public double setValue(int idx, double val) {
        return values[idx] = val;
    }

    /** 
     * Adds the value to the c'th entry.
     * @param The index, c 
     */
    public double addValue(int idx, double val) {
        return values[idx] += val; 
    }
    
    /** Add the addend to each value. */    
    public void add(double addend) {
        DoubleArrays.add(values, addend);        
    }

    public void subtract(double val) {
        DoubleArrays.add(values, val);
    }
    
    /** Scale each value by lambda. */
    public void multiply(double val) {
        DoubleArrays.scale(values, val);        
    }

    /** Divide each value by lambda. */
    public void divide(double val) {
        DoubleArrays.scale(values, 1.0 / val);
    }

    /** Set all the values to the given value. */
    public void fill(double val) {
        Arrays.fill(values, val);
    }

    /**
     * Adds a factor elementwise to this one.
     * @param other The addend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemAdd(Tensor other) {
        checkEqualSize(this, other);
        DoubleArrays.add(this.values, other.values);        
    }

    /**
     * Subtracts a factor elementwise from this one.
     * @param other The subtrahend.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemSubtract(Tensor other) {
        checkEqualSize(this, other);
        DoubleArrays.subtract(this.values, other.values);        
    }

    /**
     * Multiply a factor elementwise with this one.
     * @param other The multiplier.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemMultiply(Tensor other) {
        checkEqualSize(this, other);
        DoubleArrays.multiply(this.values, other.values);        
    }

    /**
     * Divide a factor elementwise from this one.
     * @param other The divisor.
     * @throws IllegalArgumentException If the two tensors have different sizes.
     */
    public void elemDivide(Tensor other) {
        checkEqualSize(this, other);
        DoubleArrays.divide(this.values, other.values);        
    }
    
    public void elemOp(Tensor other, Lambda.LambdaBinOpDouble fn) {
        checkEqualSize(this, other);
        for (int c=0; c<this.values.length; c++) {
            this.values[c] = fn.call(this.values[c], other.values[c]);
        }
    }

    public double getSum() {
        return DoubleArrays.sum(this.values);
    }

    public double getProd() {
        return DoubleArrays.prod(this.values);
    }

    /** Gets the ID of the configuration with the maximum value. */
    public int getArgmaxConfigId() {
        return DoubleArrays.argmax(values);
    }    

    /**
     * Gets the infinity norm of this tensor. Defined as the maximum absolute
     * value of the entries.
     */
    public double getInfNorm() {
        return DoubleArrays.infinityNorm(values);
    }

    /** Computes the sum of the entries of the pointwise product of two tensors with identical domains. */
    public double getDotProduct(Tensor other) {
        checkEqualSize(this, other);
        return DoubleArrays.dotProduct(this.values, other.values);
    }
    
    /** Gets the number of entries in the Tensor. */
    public int size() {
        return values.length;
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


    private static void checkEqualSize(Tensor t1, Tensor t2) {
        if (t1.size() != t2.size()) {
            throw new IllegalArgumentException("Input tensors are not the same size");
        }
    }
    
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
            if (!Primitives.equals(values[i], other.values[i], delta))
                return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Factor [\n");
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
            sb.append(String.format("  |  %f\n", values[c]));
        }
        sb.append("]");
        return sb.toString();
    }
    
    /** For testing only. */
    double[] getValues() {
        return values;
    }
    
}
