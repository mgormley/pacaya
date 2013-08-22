package edu.jhu.gm;

import edu.jhu.gm.BeliefPropagation.FgInferencerFactory;

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
    }
    
    private FactorGraph fg;
    private DenseFactor joint;
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
    private static DenseFactor getProductOfAllFactors(FactorGraph fg, boolean logDomain) {
        DenseFactor joint = new DenseFactor(new VarSet(), logDomain ? 0.0 : 1.0);
        for (int a=0; a<fg.getNumFactors(); a++) {
            if (fg.getFactor(a) instanceof ExplicitFactor) {
                DenseFactor factor = (DenseFactor) fg.getFactor(a);
                if (logDomain) {
                    joint.add(factor);
                } else {
                    joint.prod(factor);
                }
            } else {
                throw new RuntimeException("BruteForceInferencer only applies to DenseFactors");
            }
        }
        return joint;
    }
    
    @Override
    public void run() {
        joint = getProductOfAllFactors(fg, logDomain);
    }

    /** Gets the unnormalized joint factor over all variables. */
    public DenseFactor getJointFactor() {
        return joint;
    }
    
    @Override
    public DenseFactor getMarginals(Var var) {
        if (logDomain) {
            return joint.getLogMarginal(new VarSet(var), true);
        } else {
            return joint.getMarginal(new VarSet(var), true);
        }
    }

    @Override
    public DenseFactor getMarginals(VarSet varSet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public DenseFactor getMarginals(Factor factor) {
        if (logDomain) {
            return joint.getLogMarginal(factor.getVars(), true);
        } else {
            return joint.getMarginal(factor.getVars(), true);
        }        
    }

    @Override
    public DenseFactor getMarginalsForVarId(int varId) {
        return getMarginals(fg.getVar(varId));
    }

    @Override
    public DenseFactor getMarginalsForFactorId(int factorId) {
        return getMarginals(fg.getFactor(factorId));
    }

    @Override
    public double getPartition() {
        if (joint.getVars().size() == 0) {
            return logDomain ? 0.0 : 1.0;
        }
        if (logDomain) {
            return joint.getLogSum();
        } else {
            return joint.getSum();
        }
    }

    @Override
    public boolean isLogDomain() {
        return logDomain;
    }

    @Override
    public void clear() {
        joint = null;
    }

}
