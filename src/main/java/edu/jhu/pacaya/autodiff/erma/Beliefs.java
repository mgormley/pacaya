package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;

/** Struct for beliefs (i.e. approximate marginals) of a factor graph. */
public class Beliefs implements MVec {
    
    public VarTensor[] varBeliefs;
    public VarTensor[] facBeliefs;
    public Algebra s;
    
    public Beliefs(Algebra s) {
        this.s = s;
    }

    public Beliefs(VarTensor[] varBeliefs, VarTensor[] facBeliefs) {
        this.varBeliefs = varBeliefs;
        this.facBeliefs = facBeliefs;
    }

    public Beliefs copy() {
        Beliefs clone = new Beliefs(s);
        clone.varBeliefs = MVecArray.copyOfArray(this.varBeliefs);
        clone.facBeliefs = MVecArray.copyOfArray(this.facBeliefs);
        return clone;
    }
    
    public Beliefs copyAndConvertAlgebra(Algebra newS) {
        Beliefs clone = new Beliefs(newS);
        clone.varBeliefs = MVecArray.copyAndConvertAlgebraOfArray(this.varBeliefs, newS);
        clone.facBeliefs = MVecArray.copyAndConvertAlgebraOfArray(this.facBeliefs, newS);
        return clone;
    }

    public void fill(double val) {
        MVecArray.fillArray(varBeliefs, val);
        MVecArray.fillArray(facBeliefs, val);
    }

    public Beliefs copyAndFill(double val) {
        Beliefs clone = copy();
        clone.fill(val);
        return clone;
    }
    
    public int size() {
        return MVecArray.count(varBeliefs) + MVecArray.count(facBeliefs);
    }

    /**
     * Gets a particular value by treating this object as a vector.
     * 
     * NOTE: This implementation is O(n).
     * 
     * @param idx The index of the value to get.
     * @return The value at that index.
     */
    @Override
    public double getValue(int idx) {
        int vSize = MVecArray.count(varBeliefs);
        if (idx < vSize) {
            return MVecArray.getValue(idx, varBeliefs);
        } else {
            return MVecArray.getValue(idx - vSize, facBeliefs);
        }
    }

    /**
     * Sets a particular value by treating this object as a vector.
     * 
     * NOTE: This implementation is O(n).
     * 
     * @param idx The index of the value to set.
     * @param val The value to set.
     * @return The previous value at that index.
     */
    public double setValue(int idx, double val) {
        int vSize = MVecArray.count(varBeliefs);
        if (idx < vSize) {
            return MVecArray.setValue(idx, val, varBeliefs);
        } else {
            return MVecArray.setValue(idx - vSize, val, facBeliefs);
        }
    }

    @Override    
    public void elemAdd(MVec addend) {
        if (addend instanceof Beliefs) {
            elemAdd((Beliefs)addend);
        } else {
            throw new IllegalArgumentException("Addend must be of type " + this.getClass());
        }
    }
    
    public void elemAdd(Beliefs addend) {
        MVecArray.addArray(this.varBeliefs, addend.varBeliefs);
        MVecArray.addArray(this.facBeliefs, addend.facBeliefs);
    }
    
    @Override
    public Algebra getAlgebra() {
        return s;
    }
    
}