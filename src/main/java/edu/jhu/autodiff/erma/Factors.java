package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.ModuleTensor;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;

/** Struct for potential tables of a factor graph. */
public class Factors implements ModuleTensor {
    
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
        clone.f = Beliefs.copyOfVarTensorArray(this.f);
        return clone;
    }

    public void fill(double val) {
        Beliefs.fillVarTensorArray(f, val);
    }

    public Factors copyAndFill(double val) {
        Factors clone = copy();
        clone.fill(val);
        return clone;
    }
    
    public int size() {
        return Beliefs.count(f);
    }

    public double setValue(int idx, double val) {
        return Beliefs.setValue(idx, val, f);
    }
    
}