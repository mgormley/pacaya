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
public class HeadAutomataFactor extends AbstractConstraintFactor implements GlobalFactor, AutodiffGlobalFactor {

    private static final long serialVersionUID = 1L;
 
    private static final Logger log = LoggerFactory.getLogger(HeadAutomataFactor.class);
    
    // The sentence length.
    private final int n;
    // 1-indexed position of head.
    private int head;
    // Whether this is a right (true) or left (false) head automata.
    private boolean isRight;
    // The parent-child variables, ordered by increasing distance from parent.
    private LinkVar[] varsByDist;
    // The standard VarSet containing all from childVars.
    private final VarSet vars;
    
    // TODO: Decide whether this should only include the scores for the sibling pairs for this factor.
    // Module for creating the scores. 1-indexed by head, modifier, sibling.
    // 0 in modifier position indicates the start symbol.
    // 0 in sibling position indicates the end symbol.
    private Module<Tensor> scoresIn;

    /**
     * 
     * @param rootVars
     * @param varsByDist
     * @param scores Tensor representing the scores (1-indexed by head, modifier, sibling)
     */
    public HeadAutomataFactor(int head, boolean isRight, LinkVar[] varsByDist, Tensor scores) {
        this(head, isRight, varsByDist, new Identity<Tensor>(scores));
    }
    
    public HeadAutomataFactor(int head, boolean isRight, LinkVar[] varsByDist, Module<Tensor> scoresIn) {    
        super();
        this.head = head;
        this.isRight = isRight;
        this.n = varsByDist.length;
        this.varsByDist = varsByDist;
        this.vars = new VarSet();
        for (int d=0; d<varsByDist.length; d++) {
            vars.add(varsByDist[d]);
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
            
            // Gather trellis edge weights from incoming messages and sibling scores.
            
            // Run forward-backward on the sparse trellis in O(n^2) time.
            
            // Compute the outgoing messages from the sums of the trellis weights.
            
            return y;
        }

        @Override
        public void backward() {
            // Gather the adjoints from the outgoing messages.
            
            // Perform a backward pass through forward-backward on the trellis.
            
            // Add to the adjoints of the incoming messages.
            
        }

        @Override
        public List<? extends Module<? extends MVec>> getInputs() {
            return Lists.getList(modIn, fm);
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
        int maxDist = varsByDist.length;
        int dir = isRight ? 1 : -1;
        int firstChild = 0; // 1-indexed.
        int lastChild = 0; // 1-indexed.
        // For each modifier (ordered by distance from the head).
        for (int cDist = 1; cDist <= maxDist; cDist++) {
            int c = head + dir*cDist;
            if (head == c) { continue; }
            if (vc.getState(getVarByDist(cDist)) == LinkVar.FALSE) { continue; }
            if (firstChild == 0) { firstChild = c; }
            lastChild = c;
            // For each sibling (ordered by distance from the head).
            for (int bDist = cDist+1; bDist <= maxDist; bDist++) {
                int b = head + dir*bDist;
                if (head == b || c == b) { continue; }
                if (vc.getState(getVarByDist(bDist)) == LinkVar.FALSE) { continue; }
                double sc = scores.get(head, c, b);
                logScore = s.times(logScore, Algebras.convertAlgebra(sc, scores.getAlgebra(), s));
                break;
            }
        }
        // Include scores for start and end bigrams.
        // <#, firstChild> 
        double fcSc = scores.get(head, 0, firstChild);
        logScore = s.times(logScore, Algebras.convertAlgebra(fcSc, scores.getAlgebra(), s));
        // <lastChild, #>
        double lcSc = scores.get(head, lastChild, 0);
        logScore = s.times(logScore, Algebras.convertAlgebra(lcSc, scores.getAlgebra(), s));
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
    
    @Override
    public Module<Tensor> getExpectedLogBeliefModule(Module<MVecArray<VarTensor>> modIn, Module<?> fm) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public double getExpectedLogBelief(VarTensor[] inMsgs) {
        throw new RuntimeException("not implemented");
    }

    /**
     * Gets the variables for this factor ordered from the head outward.
     * @param head The 0-indexed head (i.e. -1 denotes the wall).
     * @param isRight Whether or not this is a right or left head automata.
     * @param rootVars The link vars with the wall as parent.
     * @param childVars The other child variables.
     * @return An array of variables order by increasing distance from the head.
     */
    public static LinkVar[] getVarsByDist(int head, boolean isRight, LinkVar[] rootVars, LinkVar[][] childVars) {
        int dir = isRight ? 1 : -1;
        int maxDist = isRight ? rootVars.length - head - 1: head - 1;
        LinkVar[] varsByDist = new LinkVar[maxDist];
        for (int cDist = 1; cDist <= maxDist; cDist++) {
            int c = head + dir*cDist;
            if (head == -1) {
                varsByDist[cDist-1] = rootVars[c];
            } else {
                varsByDist[cDist-1] = childVars[head][c];
            }
        }
        return varsByDist;
    }

    public LinkVar getVarByDist(int dist) {
        return varsByDist[dist-1];
    }
    
}
