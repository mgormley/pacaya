package edu.jhu.gm.inf;

import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarSet;
import edu.jhu.prim.util.math.FastMath;

/**
 * Inference by brute force summation.
 * 
 * @author mgormley
 *
 */
public class BruteForceInferencer implements FgInferencer {
    
    public static class BruteForceInferencerPrm implements FgInferencerFactory {
        public boolean logDomain = true;
        public BruteForceInferencerPrm(boolean logDomain) {
            this.logDomain = logDomain;
        }
        public FgInferencer getInferencer(FactorGraph fg) {
            return new BruteForceInferencer(fg, this.logDomain);
        }
        public boolean isLogDomain() {
            return logDomain;
        }
    }
    
    private FactorGraph fg;
    private VarTensor joint;
    private boolean logDomain;
    
    public BruteForceInferencer(FactorGraph fg, boolean logDomain) {
        this.fg = fg;
        this.logDomain = logDomain;
    }

    /**
     * Gets the product of all the factors in the factor graph. If working in
     * the log-domain, this will do factor addition.
     * 
     * @param logDomain
     *            Whether to work in the log-domain.
     * @return The product of all the factors.
     */
    private static VarTensor getProductOfAllFactors(FactorGraph fg, boolean logDomain) {
        VarTensor joint = new VarTensor(new VarSet(), logDomain ? 0.0 : 1.0);
        for (int a=0; a<fg.getNumFactors(); a++) {
            Factor f = fg.getFactor(a);
            VarTensor factor = safeGetDenseFactor(f);
            assert !factor.containsBadValues(logDomain) : factor;
            if (logDomain) {
                joint.add(factor);
            } else {
                joint.prod(factor);
            }
        }
        return joint;
    }

    /** Gets this factor as a DenseFactor. This will construct such a factor if it is not already one. */
    public static VarTensor safeGetDenseFactor(Factor f) {
        VarTensor factor;
        if (f instanceof ExplicitFactor) {
            factor = (VarTensor) f;
        } else {
            // Create a DenseFactor which the values of this non-explicitly represented factor.
            factor = new VarTensor(f.getVars());
            for (int c=0; c<factor.size(); c++) {
                factor.setValue(c, f.getUnormalizedScore(c));
            }
        }
        return factor;
    }
    
    @Override
    public void run() {        
        joint = getProductOfAllFactors(fg, logDomain);
    }

    /** Gets the unnormalized joint factor over all variables. */
    public VarTensor getJointFactor() {
        return joint;
    }
    
    protected VarTensor getVarBeliefs(Var var) {
        if (logDomain) {
            return joint.getLogMarginal(new VarSet(var), true);
        } else {
            return joint.getMarginal(new VarSet(var), true);
        }
    }

    protected VarTensor getFactorBeliefs(Factor factor) {
        if (logDomain) {
            return joint.getLogMarginal(factor.getVars(), true);
        } else {
            return joint.getMarginal(factor.getVars(), true);
        }        
    }

    public double getPartitionBelief() {
        if (joint.getVars().size() == 0) {
            return logDomain ? 0.0 : 1.0;
        }
        if (logDomain) {
            return joint.getLogSum();
        } else {
            return joint.getSum();
        }
    }
    
    /* ------------------------- FgInferencer Methods -------------------- */
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getMarginals(Var var) {
        VarTensor marg = getVarBeliefs(var);
        if (logDomain) {
            marg.convertLogToReal();
        }
        return marg;
    }
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getMarginals(Factor factor) {
        VarTensor marg = getFactorBeliefs(factor);
        if (logDomain) {
            marg.convertLogToReal();
        }
        return marg;
    }
        
    /** @inheritDoc */
    @Override
    public VarTensor getMarginalsForVarId(int varId) {
        return getMarginals(fg.getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public VarTensor getMarginalsForFactorId(int factorId) {
        return getMarginals(fg.getFactor(factorId));
    }

    /** @inheritDoc */
    @Override
    public double getPartition() {
        double pb = getPartitionBelief();
        if (logDomain) {
            pb = FastMath.exp(pb);
        }
        return pb; 
    }    

    /** @inheritDoc
     */
    @Override
    public VarTensor getLogMarginals(Var var) {
        VarTensor marg = getVarBeliefs(var);
        if (!logDomain) {
            marg.convertRealToLog();
        }
        return marg;
    }
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getLogMarginals(Factor factor) {
        VarTensor marg = getFactorBeliefs(factor);
        if (!logDomain) {
            marg.convertRealToLog();
        }
        return marg;
    }
        
    /** @inheritDoc */
    @Override
    public VarTensor getLogMarginalsForVarId(int varId) {
        return getLogMarginals(fg.getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public VarTensor getLogMarginalsForFactorId(int factorId) {
        return getLogMarginals(fg.getFactor(factorId));
    }

    /** @inheritDoc */
    @Override
    public double getLogPartition() {
        double pb = getPartitionBelief();
        if (!logDomain) {
            pb = FastMath.log(pb);
        }
        return pb; 
    }

    @Override
    public boolean isLogDomain() {
        return logDomain;
    }

}
