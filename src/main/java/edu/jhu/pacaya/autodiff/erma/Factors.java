package edu.jhu.pacaya.autodiff.erma;

import java.util.List;

import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;

/** Struct for potential tables of a factor graph. */
public class Factors extends MVecArray<VarTensor> implements MVec {
    
    public List<Module<?>> facMods;
    public FactorGraph fg; // Optional
    
    /**
     * This special constructor does not fully initialize the Factors object. The field {@code Factors.f}
     * must be set manually.
     * 
     * @param s The algebra.
     * @param facMods The input factor modules used to construct this.
     */
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

    /** Get the {@link FactorGraph} that constructed these factors. */
    public FactorGraph getFactorGraph() {
        return fg;
    }
    
}