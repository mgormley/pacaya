package edu.jhu.gm.model;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;

import edu.jhu.data.DepTree;
import edu.jhu.data.WallDepTreeNode;
import edu.jhu.gm.inf.BeliefPropagation.Messages;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.depparse.FirstOrderDepParseHypergraph;
import edu.jhu.hypergraph.depparse.HyperDepParser;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.parse.dep.ProjectiveDependencyParser.DepIoChart;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.arrays.Multinomials;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.LogPosNegSemiring;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.RealSemiring;
import edu.jhu.util.semiring.Semiring;
import edu.jhu.util.semiring.SemiringExt;

/**
 * Global factor which constrains O(n^2) variables to form a projective
 * dependency tree, following Smith & Eisner (2008).
 * 
 * @author mgormley
 */
public class ProjDepTreeFactor extends AbstractGlobalFactor implements GlobalFactor {

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
    protected void createMessages(FgNode parent, Messages[] msgs, boolean logDomain) {
        // Note on logDomain: all internal computation is done in the logDomain
        // since (for example) pi the product of all incoming false messages
        // would overflow.        
        assert (this == parent.getFactor());        
        double[] root = new double[n];
        double[][] child = new double[n][n];
        getLogOddsRatios(parent, msgs, logDomain, root, child);
        double pi = getProductOfAllFalseMessages(parent, msgs, logDomain);

        // Compute the dependency tree marginals, summing over all projective
        // spanning trees via the inside-outside algorithm.
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);

        // partition = pi * \sum_{y \in Trees} \prod_{edge \in y} weight(edge) 
        // Here we store the log partition.
        double partition = pi + chart.getLogPartitionFunction();

        if (log.isTraceEnabled()) {
            log.trace(String.format("partition: %.2f", partition));
        }
        
        // Create the messages and stage them in the Messages containers.
        for (FgEdge outEdge : parent.getOutEdges()) {
            LinkVar link = (LinkVar) outEdge.getVar();
            
            // The beliefs are computed as follows.
            // beliefTrue = pi * FastMath.exp(chart.getLogSumOfPotentials(link.getParent(), link.getChild()));
            // beliefFalse = partition - beliefTrue;
            // 
            // Then the outgoing messages are computed as:
            // outMsgTrue = beliefTrue / inMsgTrue
            // outMsgFalse = beliefFalse / inMsgFalse
            // 
            // Here we compute the logs of these quantities.

            double beliefTrue = pi + chart.getLogSumOfPotentials(link.getParent(), link.getChild());
            double beliefFalse = safeLogSubtract(partition, beliefTrue);
            
            if (log.isTraceEnabled()) {
                // Detect numerical precision error.
                if (beliefFalse == Double.NEGATIVE_INFINITY) {
                    if (!DoubleArrays.contains(root, Double.NEGATIVE_INFINITY, 1e-13) && !EdgeScores.childContains(child, Double.NEGATIVE_INFINITY, 1e-13)) {
                        log.trace("Found possible numerical precision error.");
                        // For now don't try to fix anything. Just log that there might be a problem.
                        //fixEdge(parent, msgs, logDomain, normalizeMessages, numericalPrecisionEdge);
                        //continue;
                    } else {
                        //log.warn("Unable to account for possible numerical precision error.");
                        //log.warn("Infs: " + Arrays.toString(root) + " " + Arrays.deepToString(child));
                        // TODO: There could still be a numerical precision error. How do we detect it?
                    }
                }
            }

            // Get the incoming messages.
            FgEdge inEdge = outEdge.getOpposing();
            DenseFactor inMsg = msgs[inEdge.getId()].message;
            double inMsgTrue, inMsgFalse;
            if (logDomain) {
                inMsgTrue = inMsg.getValue(LinkVar.TRUE);
                inMsgFalse = inMsg.getValue(LinkVar.FALSE);
            } else {
                inMsgTrue = FastMath.log(inMsg.getValue(LinkVar.TRUE));
                inMsgFalse = FastMath.log(inMsg.getValue(LinkVar.FALSE));                
            }
            
            double outMsgTrue = beliefTrue - inMsgTrue;
            double outMsgFalse = beliefFalse - inMsgFalse;
            
            if (inMsgTrue == Double.NEGATIVE_INFINITY || inMsgFalse == Double.NEGATIVE_INFINITY) {
                // If the incoming message contained infinites, send back the same message.
                outMsgTrue = inMsgTrue;
                outMsgFalse = inMsgFalse;
            }
            
            setOutMsgs(msgs, logDomain, outEdge, link, outMsgTrue, outMsgFalse);
        }
    }
        
    private void fixEdge(FgNode parent, Messages[] msgs, boolean logDomain, boolean normalizeMessages, FgEdge outEdge) {
        log.trace("Fixing edge: " + outEdge);

        // Note on logDomain: all internal computation is done in the logDomain
        // since (for example) pi the product of all incoming false messages
        // would overflow.        
        assert (this == parent.getFactor());        
        double[] root = new double[n];
        double[][] child = new double[n][n];
        getLogOddsRatios(parent, msgs, logDomain, root, child);
        double pi = getProductOfAllFalseMessages(parent, msgs, logDomain);

        // Get the incoming messages.
        FgEdge inEdge = outEdge.getOpposing();
        DenseFactor inMsg = msgs[inEdge.getId()].message;
        double inMsgTrue, inMsgFalse;
        if (logDomain) {
            inMsgTrue = inMsg.getValue(LinkVar.TRUE);
            inMsgFalse = inMsg.getValue(LinkVar.FALSE);
        } else {
            inMsgTrue = FastMath.log(inMsg.getValue(LinkVar.TRUE));
            inMsgFalse = FastMath.log(inMsg.getValue(LinkVar.FALSE));                
        }
        // Divide out the skipped edge ahead of time.
        // This is equivalent to setting the odds ratio to 1.0.
        LinkVar link = (LinkVar) outEdge.getVar();
        if (link.parent == -1) {
            root[link.child] = 0.0;
        } else {
            child[link.parent][link.child] = 0.0;
        }
        pi -= inMsgFalse;
        
        // Compute the dependency tree marginals, summing over all projective
        // spanning trees via the inside-outside algorithm.
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);

        // partition = pi * \sum_{y \in Trees} \prod_{edge \in y} weight(edge) 
        // Here we store the log partition.
        double partition = pi + chart.getLogPartitionFunction();
        if (log.isTraceEnabled()) {
            log.trace(String.format("partition: %.2f", partition));
        } 
        
        // Create the messages and stage them in the Messages containers.
        // The beliefs are computed as follows.
        double beliefTrue = pi + chart.getLogSumOfPotentials(link.getParent(), link.getChild());
        double beliefFalse = safeLogSubtract(partition, beliefTrue);        
        // Don't divide out.
        double outMsgTrue = beliefTrue;
        double outMsgFalse = beliefFalse;        
        setOutMsgs(msgs, logDomain, outEdge, link, outMsgTrue, outMsgFalse);
    }
    
    private double safeLogSubtract(double partition, double beliefTrue) {
        double outMsgFalse;
        if (partition < beliefTrue) {
            // This will happen very frequently if the log-add table is used
            // instead of "exact" log-add.
            if (log.isTraceEnabled()) {
                log.trace(String.format("Partition function less than belief: partition=%.20f belief=%.20f", partition, beliefTrue));
            }
            // To get around the floating point error, we truncate the
            // subtraction to log(0).
            outMsgFalse = Double.NEGATIVE_INFINITY;
            unsafeLogSubtracts++;
        } else {
            outMsgFalse = FastMath.logSubtractExact(partition, beliefTrue);
        }
        logSubtractCount++;
        return outMsgFalse;
    }
    private static int unsafeLogSubtracts = 0;
    private static int logSubtractCount = 0;
    private static int extremeOddsRatios = 0;
    private static int oddsRatioCount = 0;

    /** Computes the log odds ratio for each edge. w_i = \mu_i(1) / \mu_i(0) */
    private void getLogOddsRatios(FgNode parent, Messages[] msgs, boolean logDomain, double[] root, double[][] child) {
        // Keep track of the minimum and maximum odds ratios, in order to detect
        // possible numerical precision issues.
        double minOddsRatio = Double.POSITIVE_INFINITY;
        double maxOddsRatio = Double.NEGATIVE_INFINITY;
        
        // Compute the odds ratios of the messages for each edge in the tree.
        DoubleArrays.fill(root, Double.NEGATIVE_INFINITY);
        DoubleArrays.fill(child, Double.NEGATIVE_INFINITY);
        for (FgEdge inEdge : parent.getInEdges()) {
            LinkVar link = (LinkVar) inEdge.getVar();
            DenseFactor inMsg = msgs[inEdge.getId()].message;
            double oddsRatio;
            if (logDomain) {
                oddsRatio = inMsg.getValue(LinkVar.TRUE) - inMsg.getValue(LinkVar.FALSE);
            } else {
                assert inMsg.getValue(LinkVar.TRUE) >= 0 : inMsg.getValue(LinkVar.TRUE);
                assert inMsg.getValue(LinkVar.FALSE) >= 0 : inMsg.getValue(LinkVar.FALSE);
                // We still need the log of this ratio since the parsing algorithm works in the log domain.
                oddsRatio = FastMath.log(inMsg.getValue(LinkVar.TRUE)) - FastMath.log(inMsg.getValue(LinkVar.FALSE));
            }            
            
            if (link.getParent() == -1) {
                root[link.getChild()] = oddsRatio;
            } else {
                child[link.getParent()][link.getChild()] = oddsRatio;
            }
            
            // Check min/max.
            if (oddsRatio < minOddsRatio && oddsRatio != Double.NEGATIVE_INFINITY) {
                // Don't count *negative* infinities when logging extreme odds ratios.
                minOddsRatio = oddsRatio;
            }
            if (oddsRatio > maxOddsRatio) {
                maxOddsRatio = oddsRatio;
            }
        }

        // Check whether the max/min odds ratios (if added) would result in a
        // floating point error.
        oddsRatioCount++;
        if (FastMath.logSubtractExact(FastMath.logAdd(maxOddsRatio, minOddsRatio), maxOddsRatio) == Double.NEGATIVE_INFINITY) {
            extremeOddsRatios++;            
            log.debug(String.format("maxOddsRatio=%.20g minOddsRatio=%.20g", maxOddsRatio, minOddsRatio));
            log.debug(String.format("Proportion extreme odds ratios:  %f (%d / %d)", (double) extremeOddsRatios/ oddsRatioCount, extremeOddsRatios, oddsRatioCount));
            // We log the proportion of unsafe log-subtracts here only as a convenient way of highlighting the two floating point errors together.
            log.debug(String.format("Proportion unsafe log subtracts:  %f (%d / %d)", (double) unsafeLogSubtracts / logSubtractCount, unsafeLogSubtracts, logSubtractCount));
        }
    }

    /** Computes pi = \prod_i \mu_i(0). */
    private double getProductOfAllFalseMessages(FgNode parent, Messages[] msgs, boolean logDomain) {
        // Precompute the product of all the "false" messages.
        // pi = \prod_i \mu_i(0)
        // Here we store log pi.
        double pi = 0.0;
        for (FgEdge inEdge : parent.getInEdges()) {
            DenseFactor inMsg = msgs[inEdge.getId()].message;
            if (logDomain) {
                pi += inMsg.getValue(LinkVar.FALSE);
            } else {
                pi += FastMath.log(inMsg.getValue(LinkVar.FALSE));
            }
        }
        return pi;
    }

    /** Sets the outgoing messages. */
    private void setOutMsgs(Messages[] msgs, boolean logDomain, FgEdge outEdge, LinkVar link,
            double outMsgTrue, double outMsgFalse) {
        
        // Set the outgoing messages.
        DenseFactor outMsg = msgs[outEdge.getId()].newMessage;
        outMsg.setValue(LinkVar.FALSE, outMsgFalse);
        outMsg.setValue(LinkVar.TRUE, outMsgTrue);
                
        if (log.isTraceEnabled()) {
            log.trace(String.format("outMsgTrue: %s = %.2f", link.getName(), outMsg.getValue(LinkVar.TRUE)));
            log.trace(String.format("outMsgFalse: %s = %.2f", link.getName(), outMsg.getValue(LinkVar.FALSE)));
        }
        
        // Convert log messages to messages for output.
        if (!logDomain) {
            outMsg.convertLogToReal();
        }
        
        //assert !Double.isInfinite(outMsg.getValue(0)) && !Double.isInfinite(outMsg.getValue(1));
        assert !outMsg.containsBadValues(logDomain) : "message = " + outMsg;
    }

    @Override
    public double getExpectedLogBelief(FgNode parent, Messages[] msgs, boolean logDomain) {
        if (n == 0) {
            return 0.0;
        }
        assert parent.getFactor() == this;
        double[] root = new double[n];
        double[][] child = new double[n][n];
        getLogOddsRatios(parent, msgs, logDomain, root, child);
        double logPi = getProductOfAllFalseMessages(parent, msgs, logDomain);

        SemiringExt s = new LogPosNegSemiring();
        Pair<FirstOrderDepParseHypergraph, Scores> pair = HyperDepParser.insideAlgorithmEntropyFoe(root, child, s);
        FirstOrderDepParseHypergraph graph = pair.get1();
        Scores scores = pair.get2();
        
        int rt = graph.getRoot().getId();        
        double Z = scores.beta[rt];
        double rbar = scores.betaFoe[rt];
        double pi = s.fromLogProb(logPi);
        double partition = s.times(pi, Z);
        double expectation = s.toLogProb(s.divide(pi, partition)) + s.toReal(s.divide(rbar, Z));
        // Equivalent code:
        //        double logZ = s.toLogProb(scores.beta[rt]);     
        //        double logRbar = s.toLogProb(scores.betaFoe[rt]);
        //        double logPartition = logPi + logZ;
        //        double expectation = logPi - logPartition + FastMath.exp(logRbar - logZ);

        // TODO: Keep these for debugging.
        // log.debug(String.format("Z=%f rbar=%f pi=%f E=%f", logZ, logRbar, logPi, expectation));
        // log.debug(String.format("Z=%f rbar=%f pi=%f E=%f", s.toReal(logZ), s.toReal(logRbar), FastMath.exp(logPi), expectation));
        if (Double.isNaN(expectation)) {
            log.warn("Expected log belief was NaN. Returning zero instead.");
            return 0.0;
        }
        return expectation;
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
    public double getUnormalizedScore(int configId) {
        VarConfig vc = vars.getVarConfig(configId);
        // TODO: This would be faster: int[] cfg = vars.getVarConfigAsArray(configId);
        return getUnormalizedScore(vc);
    }

    @Override
    public double getUnormalizedScore(VarConfig vc) {
        Semiring s = logDomain ? new LogSemiring() : new RealSemiring();  
        if (!hasOneParentPerToken(n, vc)) {
            log.warn("Tree has more than one arc to root.");
            return s.zero();
        }
        int[] parents = getParents(n, vc);
        if (!DepTree.isDepTree(parents, true)) {
            log.warn("Tree is not a valid dependency tree.");
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

    @Override
    public void backwardCreateMessages(FgNode parent, Messages[] msgs, Messages[] msgsAdj, boolean logDomain) {
        
    }

}
