package edu.jhu.pacaya.autodiff.erma;

import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public abstract class AbstractFgInferencer implements FgInferencer {
    
    /** Gets the normalized variable beliefs in the semiring of the inferencer. */
    protected abstract VarTensor getVarBeliefs(Var var);

    /** Gets the normalized factor beliefs in the semiring of the inferencer. */
    protected abstract VarTensor getFactorBeliefs(Factor factor);
    
    /** Gets the partition belief in the semiring of the inferencer. */
    public abstract double getPartitionBelief();
    
    public abstract FactorGraph getFactorGraph();
    
    public abstract Algebra getAlgebra();
    
    /* ------------------------- FgInferencer Methods -------------------- */

    /** @inheritDoc
     */
    @Override
    public VarTensor getMarginals(Var var) {
        VarTensor marg = getVarBeliefs(var);
        marg = ensureRealSemiring(marg);
        return marg;
    }
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getMarginals(Factor factor) {
        VarTensor marg = getFactorBeliefs(factor);
        marg = ensureRealSemiring(marg);
        return marg;
    }    

    /** @inheritDoc
     */
    @Override
    public VarTensor getLogMarginals(Var var) {
        VarTensor marg = getVarBeliefs(var);
        marg = ensureLogSemiring(marg);
        return marg;
    }
    
    /** @inheritDoc
     */
    @Override
    public VarTensor getLogMarginals(Factor factor) {
        VarTensor marg = getFactorBeliefs(factor);
        marg = ensureLogSemiring(marg);
        return marg;
    }
        
    /** @inheritDoc */
    @Override
    public VarTensor getMarginalsForVarId(int varId) {
        return getMarginals(getFactorGraph().getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public VarTensor getMarginalsForFactorId(int factorId) {
        return getMarginals(getFactorGraph().getFactor(factorId));
    }
        
    /** @inheritDoc */
    @Override
    public VarTensor getLogMarginalsForVarId(int varId) {
        return getLogMarginals(getFactorGraph().getVar(varId));
    }

    /** @inheritDoc */
    @Override
    public VarTensor getLogMarginalsForFactorId(int factorId) {
        return getLogMarginals(getFactorGraph().getFactor(factorId));
    }

    /** @inheritDoc */
    @Override
    public double getPartition() {
        double pb = getPartitionBelief();
        return getAlgebra().toReal(pb);
    }

    /** @inheritDoc */
    @Override
    public double getLogPartition() {
        double pb = getPartitionBelief();
        return getAlgebra().toLogProb(pb);
    }   

    public static VarTensor ensureRealSemiring(VarTensor marg) {
        if (!marg.getAlgebra().equals(RealAlgebra.getInstance())) {
            marg = marg.copyAndConvertAlgebra(RealAlgebra.getInstance());
        }
        return marg;
    }
    
    public static VarTensor ensureLogSemiring(VarTensor marg) {
        if (!marg.getAlgebra().equals(LogSemiring.getInstance())) {
            marg = marg.copyAndConvertAlgebra(LogSemiring.getInstance());
        }
        return marg;
    }
    
}
