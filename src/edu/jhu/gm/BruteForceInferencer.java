package edu.jhu.gm;

/**
 * Inference by brute force summation.
 * 
 * @author mgormley
 *
 */
public class BruteForceInferencer implements FgInferencer {

    private FactorGraph fg;
    
    // TODO: add log-domain option.
    public BruteForceInferencer(FactorGraph fg) {
        this.fg = fg;
    }
    
    @Override
    public void run() {
        // Do nothing.
    }

    @Override
    public Factor getMarginals(Var var) {
        Factor joint = new Factor(new VarSet());
        for (int a=0; a<fg.getNumFactors(); a++) {
            joint.add(fg.getFactor(a));
        }
        return joint.getLogMarginal(new VarSet(var), true);        
    }

    @Override
    public Factor getMarginals(VarSet varSet) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginals(Factor factor) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginalsForVarId(int varId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Factor getMarginalsForFactorId(int factorId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public double getLogPartition() {
        // TODO Auto-generated method stub
        return 0;
    }

}
