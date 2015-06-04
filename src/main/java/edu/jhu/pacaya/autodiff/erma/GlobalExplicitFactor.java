package edu.jhu.pacaya.autodiff.erma;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.pacaya.autodiff.AbstractMutableModule;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.MutableModule;
import edu.jhu.pacaya.autodiff.Scalar;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.inf.FgInferencer;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.IFgModel;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;

/**
 * FOR TESTING ONLY. Treats an ExplicitFactor as a GlobalFactor.
 * 
 * @author mgormley
 */
public class GlobalExplicitFactor extends ExplicitFactor implements AutodiffGlobalFactor {

    private static final long serialVersionUID = 1L;

    public GlobalExplicitFactor(VarSet vars) {
        super(vars);
    }
    
    public GlobalExplicitFactor(VarTensor vt) {
        super(vt);
    }

    @Override
    public void createMessages(VarTensor[] inMsgs, VarTensor[] outMsgs) {
        MVecArrayIdentity<VarTensor> modIn = new MVecArrayIdentity<VarTensor>(new MVecArray<VarTensor>(inMsgs));
        MutableModule<MVecArray<VarTensor>> modOut = getCreateMessagesModule(modIn, null);
        modOut.setOutput(new MVecArray<VarTensor>(outMsgs));
        modOut.forward();
    }

    @Override
    public double getExpectedLogBelief(VarTensor[] inMsgs) {
        double bethe = 0.0;
        if (inMsgs.length == 0) { 
            return bethe;
        }
        // Get the factor belief in the semiring of the incoming messages.
        Algebra s = inMsgs[0].getAlgebra();
        VarTensor beliefs = BruteForceInferencer.safeNewVarTensor(s, this);
        for (VarTensor inMsg : inMsgs) {
            beliefs.prod(inMsg);
        }
        beliefs.normalize();
        
        int numConfigs = this.getVars().calcNumConfigs();
        for (int c=0; c<numConfigs; c++) {                
            // Since we want multiplication by 0 to always give 0 (not the case for Double.POSITIVE_INFINITY or Double.NaN.
            double b_c = beliefs.getValue(c);
            if (b_c != s.zero()) {
                double r_b_c = s.toReal(b_c);
                double log_b_c = s.toLogProb(b_c);
                double log_chi_c = this.getLogUnormalizedScore(c);
                bethe += r_b_c * (log_b_c - log_chi_c);
            }
        }
        return bethe;
    }

    @Override
    public void addExpectedPartials(IFgModel counts, double multiplier, FgInferencer inferencer, int factorId) {
        // No-op since this factor has no FEATURES.
    }

    @Override
    public MutableModule<MVecArray<VarTensor>> getCreateMessagesModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm) {
        return new GEFCreateMessagesModule(modIn, fm, this);
    }

    @Override
    public Module<Scalar> getExpectedLogBeliefModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm) {
        throw new RuntimeException("Not implemented");
    }

    // Conflicts with ExplicitFactor's implementation.
    //    @Override
    //    public Module<?> getFactorModule(Module<MVecFgModel> modIn, Algebra s) {
    //        return new ParamFreeGlobalFactorModule(s, this, new ArrayList<Module<MVec>>());
    //    }
    
    private static class GEFCreateMessagesModule extends AbstractMutableModule<MVecArray<VarTensor>> implements MutableModule<MVecArray<VarTensor>> {

        private Module<MVecArray<VarTensor>> modIn;
        private Module<?> fm;
        private GlobalFactor gf;

        public GEFCreateMessagesModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm, GlobalFactor gf) {
            super(modIn.getAlgebra());
            this.modIn = modIn;
            this.fm = fm;
            this.gf = gf;
        }

        @Override
        public MVecArray<VarTensor> forward() {
            VarTensor[] inMsgs = modIn.getOutput().f;
            if (inMsgs.length == 0) { 
                return new MVecArray<VarTensor>(modIn.getAlgebra());
            }
            VarTensor[] outMsgs = y.f;
            Algebra s = inMsgs[0].getAlgebra();
            VarTensor f = BruteForceInferencer.safeNewVarTensor(s, gf);
            for (int i=0; i<inMsgs.length; i++) {
                VarTensor msg = new VarTensor(f);
                for (int j=0; j<inMsgs.length; j++) {
                    if (i == j) { continue; }
                    msg.prod(inMsgs[j]);
                }
                VarTensor marg = msg.getMarginal(outMsgs[i].getVars(), false);
                outMsgs[i].set(marg);
            }
            return y;
        }

        @Override
        public void backward() {
            // TODO: Increment the adjoint for the potentials. 
            // Then add the factor creation module as one of the inputs to this one.

            // Increment the adjoint for each variable to factor message.
            VarTensor[] inMsgs = modIn.getOutput().f;
            VarTensor[] inMsgsAdj = modIn.getOutputAdj().f;
            VarTensor[] outMsgsAdj = yAdj.f;
            if (inMsgs.length == 0) { 
                return;
            }
            VarTensor f = BruteForceInferencer.safeNewVarTensor(s, gf);
            for (int i=0; i<inMsgs.length; i++) {
                for (int j=0; j<inMsgs.length; j++) {
                    if (i == j) { continue; }
                    VarTensor prod = new VarTensor(f);
                    prod.prod(outMsgsAdj[i]);
                    for (int k=0; k<inMsgs.length; k++) {
                        if (k==i || k==j) { continue; }
                        prod.prod(inMsgs[k]);
                    }
                    VarTensor marg = prod.getMarginal(inMsgs[j].getVars(), false);
                    inMsgsAdj[j].add(marg);
                }
            }
        }

        @Override
        public List<? extends Module<? extends MVec>> getInputs() {
            return Lists.getList(modIn); // TODO: add fm, but see note above.
        }
        
    }

}
