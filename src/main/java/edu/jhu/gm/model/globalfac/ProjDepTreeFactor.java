package edu.jhu.gm.model.globalfac;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.autodiff.erma.ProjDepTreeModule;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.depparse.DepParseHypergraph;
import edu.jhu.hypergraph.depparse.HyperDepParser;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.WallDepTreeNode;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogSemiring;

/**
 * Global factor which constrains O(n^2) variables to form a projective
 * dependency tree, following Smith & Eisner (2008).
 * 
 * @author mgormley
 */
public class ProjDepTreeFactor extends AbstractConstraintFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
 
    /**
     * Link variable. When true it indicates that there is an edge between its
     * parent and child.
     * 
     * @author mgormley
     */
    public static class LinkVar extends Var {

        private static final long serialVersionUID = 1L;

        // The variable states.
        public static final int TRUE = 1;
        public static final int FALSE = 0;
        
        private static final List<String> BOOLEANS = Lists.getList("FALSE", "TRUE");
        private int parent;
        private int child;     
        
        public LinkVar(VarType type, String name, int parent, int child) {
            super(type, BOOLEANS.size(), name, BOOLEANS);
            this.parent = parent;
            this.child = child;
        }

        public int getParent() {
            return parent;
        }

        public int getChild() {
            return child;
        }

        public static String getDefaultName(int i, int j) {
            return String.format("Link_%d_%d", i, j);
        }
        
    }
    
    private static final Logger log = Logger.getLogger(ProjDepTreeFactor.class);
    
    private final VarSet vars;
    /** The sentence length. */
    private final int n;
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
        
    /**
     * Constructor.
     * @param n The length of the sentence.
     */
    public ProjDepTreeFactor(int n, VarType type) {    
        super();
        this.vars = createVarSet(n, type);
        this.n = n;

        // TODO: We created the VarSet statically and then find extract the vars
        // again from the VarSet only because we're subclassing Factor. In the
        // future, we should drop this.
        rootVars = new LinkVar[n];
        childVars = new LinkVar[n][n];
        VarSet vars = this.getVars();
        for (Var var : vars) {
            LinkVar link = (LinkVar) var;
            if (link.getParent() == -1) {
                rootVars[link.getChild()] = link;
            } else {
                childVars[link.getParent()][link.getChild()] = link;
            }
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

    private static VarSet createVarSet(int n, VarType type) {
        VarSet vars = new VarSet();
        // Add a variable for each pair of tokens.
        for (int i=0; i<n; i++) {
            for (int j=0; j<n; j++) {
                if (i != j) {
                    String name = LinkVar.getDefaultName(i, j);
                    vars.add(new LinkVar(type, name, i, j));
                }
            }
        }
        // Add a variable for each variable being connected to the wall node.
        for (int j=0; j<n; j++) {
            String name = String.format("Link_%d_%d", WallDepTreeNode.WALL_POSITION, j);
            vars.add(new LinkVar(type, name, WallDepTreeNode.WALL_POSITION, j));
        }
        return vars;
    }
        
    @Override
    public void createMessages(VarTensor[] inMsgs, VarTensor[] outMsgs) {
        forwardAndBackward(inMsgs, outMsgs, null, null, true);
    }

    @Override
    public void backwardCreateMessages(VarTensor[] inMsgs, VarTensor[] outMsgsAdj, VarTensor[] inMsgsAdj) {
        forwardAndBackward(inMsgs, null, outMsgsAdj, inMsgsAdj, false);
    }
    
    public void forwardAndBackward(VarTensor[] inMsgs, VarTensor[] outMsgs, VarTensor[] outMsgsAdj, VarTensor[] inMsgsAdj, boolean isForward) {
        Algebra s = inMsgs[0].getAlgebra();

        // Get the incoming messages at time (t).
        Tensor tmTrueIn = getMsgs(inMsgs, LinkVar.TRUE, s);        
        Tensor tmFalseIn = getMsgs(inMsgs, LinkVar.FALSE, s);
        
        // Construct the circuit.
        TensorIdentity mTrueIn = new TensorIdentity(tmTrueIn);
        TensorIdentity mFalseIn = new TensorIdentity(tmFalseIn);        
        Algebra tmpS = (isForward) ? Algebras.LOG_SEMIRING : Algebras.LOG_SIGN_ALGEBRA;
        ProjDepTreeModule dep = new ProjDepTreeModule(mTrueIn, mFalseIn, tmpS);
        dep.forward();
        
        if (isForward) {
            Tensor pair = dep.getOutput();
            Tensor tmTrueOut = pair.select(0, 1);
            Tensor tmFalseOut = pair.select(0, 0);
            
            // Set the outgoing messages at time (t+1).
            setMsgs(outMsgs, tmTrueOut, LinkVar.TRUE, s);
            setMsgs(outMsgs, tmFalseOut, LinkVar.FALSE, s);
        } else {
            // Get the adjoints on outgoing message modules at time (t+1).
            Tensor tTrue = getMsgs(outMsgsAdj, LinkVar.TRUE, s);
            Tensor tFalse = getMsgs(outMsgsAdj, LinkVar.FALSE, s);
            Tensor pairAdj = dep.getOutputAdj();
            pairAdj.addTensor(tTrue, 0, 1);
            pairAdj.addTensor(tFalse, 0, 0);
            
            // Backward pass.
            dep.backward();
            
            // Increment adjoints of the incoming messages at time (t).
            addMsgs(inMsgsAdj, mTrueIn.getOutputAdj(), LinkVar.TRUE, s);
            addMsgs(inMsgsAdj, mFalseIn.getOutputAdj(), LinkVar.FALSE, s);
        }
    }

    public LinkVar[] getRootVars() {
        return rootVars;
    }

    public LinkVar[][] getChildVars() {
        return childVars;
    }
    
    @Override
    public VarSet getVars() {
        return vars;
    }
    
    @Override
    public Factor getClamped(VarConfig clmpVarConfig) {
        if (clmpVarConfig.size() == 0) {
            // None clamped.
            return this;
        } else if (clmpVarConfig.size() == vars.size()) {
            // All clamped.
            return new ProjDepTreeFactor(0, VarType.OBSERVED);
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
        LogSemiring s = Algebras.LOG_SEMIRING;
        if (!hasOneParentPerToken(n, vc)) {
            log.trace("Tree has more than one arc to root.");
            return s.zero();
        }
        int[] parents = getParents(n, vc);
        if (!DepTree.isDepTree(parents, true)) {
            log.trace("Tree is not a valid dependency tree.");
            return s.zero();
        }
        return s.one();
    }
    
    /**
     * Returns whether this variable assignment specifies one parent per token.
     */
    public static boolean hasOneParentPerToken(int n, VarConfig vc) {
        int[] parents = new int[n];
        Arrays.fill(parents, -2);
        for (Var v : vc.getVars()) {
            if (v instanceof LinkVar) {
                LinkVar link = (LinkVar) v;
                if (vc.getState(v) == LinkVar.TRUE) {
                    if (parents[link.getChild()] != -2) {
                        // Multiple parents defined for the same child.
                        return false;
                    }
                    parents[link.getChild()] = link.getParent();
                }
            }
        }
        return !ArrayUtils.contains(parents, -2);
    }

    /**
     * Extracts the parents as defined by a variable assignment for a single
     * sentence.
     * 
     * NOTE: This should NOT be used for decoding since a proper decoder will
     * enforce the tree constraint.
     * 
     * @param n The sentence length.
     * @param vc The variable assignment.
     * @return The parents array.
     */
    public static int[] getParents(int n, VarConfig vc) {
        int[] parents = new int[n];
        Arrays.fill(parents, -2);
        for (Var v : vc.getVars()) {
            if (v instanceof LinkVar) {
                LinkVar link = (LinkVar) v;
                if (vc.getState(v) == LinkVar.TRUE) {
                    if (parents[link.getChild()] != -2) {
                        throw new IllegalStateException(
                                "Multiple parents defined for the same child. Is this VarConfig for only one example?");
                    }
                    parents[link.getChild()] = link.getParent();
                }
            }
        }
        return parents;
    }

    /**
     * Gets messages from the Messages[].
     * 
     * @param inMsgs The input messages.
     * @param tf Whether to get TRUE or FALSE messages.
     * @param s The abstract algebra.
     * @return The messages as a Tensor.
     */
    private Tensor getMsgs(VarTensor[] inMsgs, int tf, Algebra s) {
        EdgeScores es = new EdgeScores(n, s.zero());
        DoubleArrays.fill(es.root, s.zero());
        DoubleArrays.fill(es.child, s.zero());
        for (VarTensor inMsg : inMsgs) {
            LinkVar link = (LinkVar) inMsg.getVars().get(0);            
            double val = inMsg.getValue(tf);
            es.setScore(link.getParent(), link.getChild(), val);
        }
        return es.toTensor(s);
    }
    
    /**
     * Sets messages on a Messages[].
     * 
     * @param msgs The output messages.
     * @param t The input messages.
     * @param tf Whether to set TRUE or FALSE messages.
     * @param s The abstract algebra.
     */
    private void setMsgs(VarTensor[] msgs, Tensor t, int tf, Algebra s) {
        EdgeScores es = EdgeScores.tensorToEdgeScores(t);
        for (VarTensor msg : msgs) {
            LinkVar link = (LinkVar) msg.getVars().get(0);            
            double val = es.getScore(link.getParent(), link.getChild());
            msg.setValue(tf, val);
        }
    }
    
    /**
     * Adds to messages on a Messages[].
     * 
     * @param msgs The output messages.
     * @param t The input messages.
     * @param tf Whether to set TRUE or FALSE messages.
     * @param s The abstract algebra.
     */
    private void addMsgs(VarTensor[] msgs, Tensor t, int tf, Algebra s) {
        EdgeScores es = EdgeScores.tensorToEdgeScores(t);
        for (VarTensor msg : msgs) {
            LinkVar link = (LinkVar) msg.getVars().get(0);
            double val = es.getScore(link.getParent(), link.getChild());
            msg.addValue(tf, val);
        }
    }

    @Override
    public double getExpectedLogBelief(VarTensor[] inMsgs) {
        if (n == 0) {
            return 0.0;
        }
        EdgeScores ratios = getLogOddsRatios(inMsgs);
        double logPi = getLogProductOfAllFalseMessages(inMsgs);

        Algebra s = Algebras.LOG_SIGN_ALGEBRA;
        Pair<DepParseHypergraph, Scores> pair = HyperDepParser.insideEntropyFoe(ratios.root, ratios.child, s, InsideOutsideDepParse.singleRoot);
        DepParseHypergraph graph = pair.get1();
        Scores scores = pair.get2();
        
        int rt = graph.getRoot().getId();        
        double Z = scores.beta[rt];
        double rbar = scores.betaFoe[rt];
        double pi = s.fromLogProb(logPi);
        double partition = s.times(pi, Z);
        double expectation = s.toLogProb(s.divide(pi, partition)) + s.toReal(s.divide(rbar, Z));
        if (log.isTraceEnabled()) {
            log.trace(String.format("Z=%g rbar=%g pi=%g part=%g E=%g", Z, rbar, pi, partition, expectation));
        }
        if (Double.isNaN(expectation)) {
            log.warn("Expected log belief was NaN. Returning zero instead.");
            return 0.0;
        }
        return expectation;
    }
    
    /** Computes the log odds ratio for each edge. w_i = \mu_i(1) / \mu_i(0) */
    private EdgeScores getLogOddsRatios(VarTensor[] inMsgs) {  
        EdgeScores es = new EdgeScores(n, Double.NEGATIVE_INFINITY);
        Algebra s = inMsgs[0].getAlgebra();
        // Compute the odds ratios of the messages for each edge in the tree.
        for (VarTensor inMsg : inMsgs) {
            LinkVar link = (LinkVar) inMsg.getVars().get(0);
            double logOdds = s.toLogProb(inMsg.getValue(LinkVar.TRUE)) - s.toLogProb(inMsg.getValue(LinkVar.FALSE));            
            if (link.getParent() == -1) {
                es.root[link.getChild()] = logOdds;
            } else {
                es.child[link.getParent()][link.getChild()] = logOdds;
            }
        }
        return es;
    }

    /** Computes pi = \prod_i \mu_i(0). */
    private double getLogProductOfAllFalseMessages(VarTensor[] inMsgs) {
        // Precompute the product of all the "false" messages.
        // pi = \prod_i \mu_i(0)
        // Here we store log pi.
        Algebra s = inMsgs[0].getAlgebra();
        double logPi = 0.0;
        for (VarTensor inMsg : inMsgs) {
            logPi += s.toLogProb(inMsg.getValue(LinkVar.FALSE));
        }
        return logPi;
    }

}
