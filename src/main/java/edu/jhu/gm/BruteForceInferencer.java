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
    private Factor joint;
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
    private static Factor getProductOfAllFactors(FactorGraph fg, boolean logDomain) {
        Factor joint = new Factor(new VarSet(), logDomain ? 0.0 : 1.0);
        for (int a=0; a<fg.getNumFactors(); a++) {
            if (logDomain) {
                joint.add(fg.getFactor(a));
            } else {
                joint.prod(fg.getFactor(a));
            }
        }
        return joint;
    }
    
    @Override
    public void run() {
        joint = getProductOfAllFactors(fg, logDomain);
    }

    /** Gets the unnormalized joint factor over all variables. */
    public Factor getJointFactor() {
        return joint;
    }
    
    @Override
    public Factor getMarginals(Var var) {
        if (logDomain) {
            return joint.getLogMarginal(new VarSet(var), true);
        } else {
            return joint.getMarginal(new VarSet(var), true);
        }
    }

    @Override
    public Factor getMarginals(VarSet varSet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginals(Factor factor) {
        if (logDomain) {
            return joint.getLogMarginal(factor.getVars(), true);
        } else {
            return joint.getMarginal(factor.getVars(), true);
        }        
    }

    @Override
    public Factor getMarginalsForVarId(int varId) {
        return getMarginals(fg.getVar(varId));
    }

    @Override
    public Factor getMarginalsForFactorId(int factorId) {
        return getMarginals(fg.getFactor(factorId));
    }

    @Override
    public double getPartition() {
        if (logDomain) {
            return joint.getLogSum();
        } else {
            return joint.getSum();
        }
    }

}
