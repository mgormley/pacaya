package edu.jhu.pacaya.gm.model.globalfac;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.pacaya.autodiff.AbstractMutableModule;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.MVec;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.MutableModule;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.erma.AutodiffGlobalFactor;
import edu.jhu.pacaya.autodiff.erma.LazyVarTensor;
import edu.jhu.pacaya.autodiff.erma.MVecFgModel;
import edu.jhu.pacaya.autodiff.erma.ParamFreeGlobalFactorModule;
import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.Algebras;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

/**
 * Global factor which adds consecutive sibling factors following Smith & Eisner (2008).
 * 
 * @author mgormley
 */
public class ConsecutiveSiblingsFactor extends AbstractConstraintFactor implements GlobalFactor, AutodiffGlobalFactor {

    private static final long serialVersionUID = 1L;
 
    private static final Logger log = LoggerFactory.getLogger(ConsecutiveSiblingsFactor.class);
    
    private final VarSet vars;
    /** The sentence length. */
    private final int n;
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    // 1-indexed by head, modifier, sibling.
    // TODO: Remove? private Tensor scores;
    // Optional module for creating the scores above.
    private Module<Tensor> scoresIn;
    
    /**
     * 
     * @param rootVars
     * @param childVars
     * @param scores Tensor representing the scores (1-indexed by head, modifier, sibling)
     */
    public ConsecutiveSiblingsFactor(LinkVar[] rootVars, LinkVar[][] childVars, Tensor scores) {
        this(rootVars, childVars, new Identity<Tensor>(scores));
    }
    
    public ConsecutiveSiblingsFactor(LinkVar[] rootVars, LinkVar[][] childVars, Module<Tensor> scoresIn) {    
        super();
        this.n = rootVars.length;
        this.rootVars = rootVars;
        this.childVars = childVars;
        this.vars = new VarSet();
        for (int c=0; c<rootVars.length; c++) {
            vars.add(rootVars[c]);
            for (int p=0; p<rootVars.length; p++) {
                if (p==c) { continue; }
                vars.add(childVars[p][c]);
            }
        }
        this.scoresIn = scoresIn;
    }
    
    @Override
    public MutableModule<MVecArray<VarTensor>> getCreateMessagesModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm) {
        return new CSFCreateMessagesModule(modIn, fm);
    }
        
    // TODO: This could be put in an abstract class.
    @Override
    public void createMessages(VarTensor[] inMsgs, VarTensor[] outMsgs) {
        Identity<MVecArray<VarTensor>> modIn = new Identity<MVecArray<VarTensor>>(new MVecArray<VarTensor>(inMsgs));
        MutableModule<MVecArray<VarTensor>> modOut = getCreateMessagesModule(modIn, null);
        modOut.setOutput(new MVecArray<VarTensor>(outMsgs));
        modOut.forward();
    }

    private class CSFCreateMessagesModule extends AbstractMutableModule<MVecArray<VarTensor>> implements MutableModule<MVecArray<VarTensor>> {

        private Module<MVecArray<VarTensor>> modIn;
        private Module<?> fm;
        
        public CSFCreateMessagesModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm) {
            super(modIn.getAlgebra());
            this.modIn = modIn;
            this.fm = fm;
        }

        @Override
        public MVecArray<VarTensor> forward() {
            if (y == null) { throw new IllegalStateException("setOutput() must be called before calling forward."); }
            
            return y;
        }

        @Override
        public void backward() {
            // TDOO:
        }

        @Override
        public List<? extends Module<? extends MVec>> getInputs() {
            return Lists.getList(modIn, fm);
        }
    }
        
    @Override
    public Factor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() == 0) {
            // None clamped.
            return this;
        } else if (clmpVarConfig.size() == vars.size()) {
            // All clamped.
            return new ConsecutiveSiblingsFactor(new LinkVar[]{}, new LinkVar[][]{{}}, new Tensor(RealAlgebra.getInstance(), 0, 0, 0));
        } else {
            // Some clamped.
            throw new IllegalStateException("Unable to clamp these variables.");
        }
    }

    @Override
    public double getLogUnormalizedScore(int configId) {
        VarConfig vc = vars.getVarConfig(configId);
        // TODO: This would be faster: int[] cfg = vars.getVarConfigAsArray(configId);
        return getLogUnormalizedScore(vc);
    }

    @Override
    public double getLogUnormalizedScore(VarConfig vc) {
        LogSemiring s = LogSemiring.getInstance();
        Tensor scores = scoresIn.getOutput();
        double logScore = s.one();
        for (int p = 0; p <= n; p++) {
            for (int c = 1; c <= n; c++) {
                if (p == c) { continue; }
                boolean pcEdge = vc.getState(getLinkVar1Idx(p, c)) == LinkVar.TRUE;
                if (pcEdge) {
                    for (int b = c + 1; b <= n; b++) {
                        if (p == b || c == b) { continue; }
                        boolean pbEdge = vc.getState(getLinkVar1Idx(p, b)) == LinkVar.TRUE;
                        if (pbEdge) {
                            double sc = scores.get(p, c, b);
                            logScore = s.times(logScore, Algebras.convertAlgebra(sc, scores.getAlgebra(), s));
                            break;
                        }
                    }
                }
            }
        }
        return logScore;
    }
    
    @Override
    public Module<LazyVarTensor> getFactorModule(Module<MVecFgModel> modIn, Algebra s) {
        return new ParamFreeGlobalFactorModule(s, this, Lists.getList(modIn, this.scoresIn));
    }
    
    @Override
    public VarSet getVars() {
        return vars;
    }
    
    /**
     * Get the link var corresponding to the specified parent and child position.
     * 
     * @param parent The parent word position (1-indexed), or 0 to indicate the wall node.
     * @param child The child word position (1-indexed).
     * @return The link variable.
     */
    public LinkVar getLinkVar1Idx(int parent, int child) {
        if (parent == 0) {
            return rootVars[child-1];
        } else {
            return childVars[parent-1][child-1];
        }
    }
    
    /**
     * Get the link var corresponding to the specified parent and child position.
     * 
     * @param parent The parent word position, or -1 to indicate the wall node.
     * @param child The child word position.
     * @return The link variable.
     */
    public LinkVar getLinkVar(int parent, int child) {
        if (parent == -1) {
            return rootVars[child];
        } else {
            return childVars[parent][child];
        }
    }

    @Override
    public Module<Tensor> getExpectedLogBeliefModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public double getExpectedLogBelief(VarTensor[] inMsgs) {
        throw new RuntimeException("not implemented");
    }
    
}
