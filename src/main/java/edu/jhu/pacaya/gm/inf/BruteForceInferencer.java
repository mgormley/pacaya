package edu.jhu.pacaya.gm.inf;

import edu.jhu.pacaya.autodiff.erma.AbstractFgInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

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
                return logDomain ? LogSemiring.LOG_SEMIRING : RealAlgebra.REAL_ALGEBRA;
            } else {
                return s;
            }
        }
        
    }
    

    private Algebra s;
    private FactorGraph fg;
    private VarTensor joint;
    
    //@Deprecated
    public BruteForceInferencer(FactorGraph fg, boolean logDomain) {
        this(fg, logDomain ? LogSemiring.LOG_SEMIRING : RealAlgebra.REAL_ALGEBRA);
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
        // Create a VarTensor which the values of this non-explicitly represented factor.
        factor = new VarTensor(s, f.getVars());
        for (int c=0; c<factor.size(); c++) {
            factor.setValue(c, s.fromLogProb(f.getLogUnormalizedScore(c)));
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
