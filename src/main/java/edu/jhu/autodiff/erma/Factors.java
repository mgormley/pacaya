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

    public Factors copy() {
        Factors clone = new Factors(s);
        clone.f = MVecArray.copyOfArray(this.f);
        return clone;
    }

    public Factors copyAndConvertAlgebra(Algebra newS) {
        Factors clone = new Factors(newS);
        clone.f = MVecArray.copyAndConvertAlgebraOfArray(this.f, newS);
        return clone;
    }

    public void fill(double val) {
        MVecArray.fillArray(f, val);
    }

    public Factors copyAndFill(double val) {
        Factors clone = copy();
        clone.fill(val);
        return clone;
    }
    
    public int size() {
        return MVecArray.count(f);
    }

    public double getValue(int idx) {
        return MVecArray.getValue(idx, f);
    }
    
    public double setValue(int idx, double val) {
        return MVecArray.setValue(idx, val, f);
    }
    
    public void elemAdd(Factors addend) {
        MVecArray.addArray(this.f, addend.f);
    }

    public Algebra getAlgebra() {
        return s;
    }
    
}