package edu.jhu.autodiff.erma;

import edu.jhu.autodiff.MVec;
import edu.jhu.autodiff.MVecArray;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;

/** Struct for potential tables of a factor graph. */
public class VarTensorArray extends MVecArray<VarTensor> implements MVec {
    
    public VarTensor[] f;
    public Algebra s;
    
    public VarTensorArray(Algebra s) {
        super(s);
    }

    public VarTensorArray(VarTensor[] facBeliefs) {
        super(facBeliefs);
    }
    
}