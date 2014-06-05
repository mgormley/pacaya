package edu.jhu.autodiff.erma;

import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

public abstract class AbstractFgInferencer implements FgInferencer {
    
    protected abstract VarTensor getVarBeliefs(Var var);

    protected abstract VarTensor getFactorBeliefs(Factor factor);
    
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

    private VarTensor ensureRealSemiring(VarTensor marg) {
        if (!marg.getAlgebra().equals(Algebras.REAL_ALGEBRA)) {
            marg = marg.copyAndConvertAlgebra(Algebras.REAL_ALGEBRA);
        }
        return marg;
    }
    
    private VarTensor ensureLogSemiring(VarTensor marg) {
        if (!marg.getAlgebra().equals(Algebras.LOG_SEMIRING)) {
            marg = marg.copyAndConvertAlgebra(Algebras.LOG_SEMIRING);
        }
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

}
