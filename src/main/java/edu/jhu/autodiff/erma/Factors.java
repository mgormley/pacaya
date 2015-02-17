package edu.jhu.autodiff.erma;

import java.util.List;

import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.MVecArray;
import edu.jhu.autodiff.Module;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;

/** Struct for potential tables of a factor graph. */
public class Factors extends MVecArray<VarTensor> implements MVec {
    
    public VarTensor[] f;
    public Algebra s;
    public List<Module<?>> facMods;
    
    public Factors(Algebra s, List<Module<?>> facMods) {
        super(s);
        this.facMods = facMods;
    }

    public Factors(VarTensor[] facBeliefs) {
        super(facBeliefs);
    }
    
    @Override
    public Factors copy() {
        Factors clone = new Factors(s, facMods);
        clone.f = MVecArray.copyOfArray(this.f);
        return clone;
    }

    @Override
    public Factors copyAndConvertAlgebra(Algebra newS) {
        Factors clone = new Factors(newS, facMods);
        clone.f = MVecArray.copyAndConvertAlgebraOfArray(this.f, newS);
        return clone;
    }
    
    /** Gets the module that created the a'th factor. */
    public Module<?> getFactorModule(int a) {
        return facMods.get(a);
    }
    
}