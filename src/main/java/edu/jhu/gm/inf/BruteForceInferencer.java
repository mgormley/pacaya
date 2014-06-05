package edu.jhu.gm.inf;

import edu.jhu.autodiff.erma.AbstractFgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

/**
 * Inference by brute force summation.
 * 
 * @author mgormley
 *
 */
public class BruteForceInferencer extends AbstractFgInferencer implements FgInferencer {
    
    public static class BruteForceInferencerPrm implements FgInferencerFactory {
        
        public boolean logDomain = true;
        public Algebra s = null;
        
        @Deprecated
        public BruteForceInferencerPrm(boolean logDomain) {
            this.logDomain = logDomain;
        }
        
        public BruteForceInferencerPrm(Algebra s) {
            this.s = s;
        }
        
        @Override
        public FgInferencer getInferencer(FactorGraph fg) {
            if (s == null) {
                return new BruteForceInferencer(fg, logDomain);
            } else {
                return new BruteForceInferencer(fg, s);
            }
        }

        @Override
        public Algebra getAlgebra() {
            if (s == null) {
                return logDomain ? Algebras.LOG_SEMIRING : Algebras.REAL_ALGEBRA;
            } else {
                return s;
            }
        }
        
    }
    

    private Algebra s;
    private FactorGraph fg;
    private VarTensor joint;
    
    @Deprecated
    public BruteForceInferencer(FactorGraph fg, boolean logDomain) {
        this(fg, logDomain ? Algebras.LOG_SEMIRING : Algebras.REAL_ALGEBRA);
    }

    public BruteForceInferencer(FactorGraph fg, Algebra s) {
        this.fg = fg;
        this.s = s;
    }
    
    /**
     * Gets the product of all the factors in the factor graph. If working in
     * the log-domain, this will do factor addition.
     * 
     * @param logDomain
     *            Whether to work in the log-domain.
     * @return The product of all the factors.
     */
    private static VarTensor getProductOfAllFactors(FactorGraph fg, Algebra s) {
        VarTensor joint = new VarTensor(s, new VarSet(), s.one());
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            VarTensor factor = safeNewVarTensor(s, f);
            assert !factor.containsBadValues() : factor;
            joint.prod(factor);
        }
        return joint;
    }

    
    /** Gets this factor as a VarTensor. This will always return a new object. See also safeGetVarTensor(). */
    public static VarTensor safeNewVarTensor(Algebra s, Factor f) {
        VarTensor factor;
        if (f instanceof VarTensor) {
            factor = ((VarTensor) f).copyAndConvertAlgebra(s);
        } else {
            // Create a VarTensor which the values of this non-explicitly represented factor.
            factor = new VarTensor(s, f.getVars());
            for (int c=0; c<factor.size(); c++) {
                factor.setValue(c, s.fromLogProb(f.getLogUnormalizedScore(c)));
            }
        }
        return factor;
    }
    
    @Override
    public void run() {        
        joint = getProductOfAllFactors(fg, s);
    }

    /** Gets the unnormalized joint factor over all variables. */
    public VarTensor getJointFactor() {
        return joint;
    }
    
    protected VarTensor getVarBeliefs(Var var) {
        return joint.getMarginal(new VarSet(var), true);
    }

    protected VarTensor getFactorBeliefs(Factor factor) {
        return joint.getMarginal(factor.getVars(), true);
    }

    public double getPartitionBelief() {
        if (joint.getVars().size() == 0) {
            return s.one();
        }
        return joint.getSum();
    }
    
    public FactorGraph getFactorGraph() {
        return fg;
    }
    
    public Algebra getAlgebra() {
        return s;
    }

}
