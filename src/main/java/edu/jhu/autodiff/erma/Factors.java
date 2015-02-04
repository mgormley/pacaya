package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.MVecArray;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;

/** Struct for potential tables of a factor graph. */
public class Factors implements MVec<Factors> {
    
    public VarTensor[] f;
    public Algebra s;
    
    public Factors(Algebra s) {
        this.s = s;
    }

    public Factors(VarTensor[] facBeliefs) {
        this.f = facBeliefs;
    }

    @Override
    public Factors copy() {
        Factors clone = new Factors(s);
        clone.f = MVecArray.copyOfArray(this.f);
        return clone;
    }
    
    @Override
    public Factors copyAndConvertAlgebra(Algebra newS) {
        Factors clone = new Factors(newS);
        clone.f = MVecArray.copyAndConvertAlgebraOfArray(this.f, newS);
        return clone;
    }
    
    @Override
    public void fill(double val) {
        MVecArray.fillArray(f, val);
    }
    
    @Override
    public Factors copyAndFill(double val) {
        Factors clone = copy();
        clone.fill(val);
        return clone;
    }
    
    @Override
    public int size() {
        return MVecArray.count(f);
    }

    @Override
    public double getValue(int idx) {
        return MVecArray.getValue(idx, f);
    }
    
    @Override
    public double setValue(int idx, double val) {
        return MVecArray.setValue(idx, val, f);
    }
    
    @Override    
    public void elemAdd(MVec<?> addend) {
        if (addend instanceof Factors) {
            elemAdd((Factors)addend);
        } else {
            throw new IllegalArgumentException("Addend must be of type " + this.getClass());
        }
    }
    
    public void elemAdd(Factors addend) {
        MVecArray.addArray(this.f, addend.f);
    }

    @Override
    public Algebra getAlgebra() {
        return s;
    }
    
}