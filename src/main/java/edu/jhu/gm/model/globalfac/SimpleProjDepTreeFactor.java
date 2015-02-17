package edu.jhu.gm.model.globalfac;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.autodiff.erma.InsideOutsideDepParse;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.ConstituencyTreeFactor.SpanVar;
import edu.jhu.hypergraph.Hyperalgo.Scores;
import edu.jhu.hypergraph.depparse.O1DpHypergraph;
import edu.jhu.hypergraph.depparse.HyperDepParser;
import edu.jhu.nlp.data.DepTree;
import edu.jhu.nlp.data.WallDepTreeNode;
import edu.jhu.parse.dep.DepIoChart;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogSemiring;

/**
 * Global factor which constrains O(n^2) variables to form a projective
 * dependency tree, following Smith & Eisner (2008).
 * 
 * @author mgormley
 */
public class SimpleProjDepTreeFactor extends AbstractConstraintFactor implements GlobalFactor {

    private static final long serialVersionUID = 1L;
     
    private static final Logger log = LoggerFactory.getLogger(SimpleProjDepTreeFactor.class);
    
    private final VarSet vars;
    /** The sentence length. */
    private final int n;
    private LinkVar[] rootVars;
    private LinkVar[][] childVars;
    
    /**
     * Constructor.
     * @param n The length of the sentence.
     */
    public SimpleProjDepTreeFactor(int n, VarType type) {    
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
        Algebra s = inMsgs[0].getAlgebra();
        if (!s.equals(Algebras.REAL_ALGEBRA) && !s.equals(Algebras.LOG_SEMIRING)) {
            throw new IllegalStateException("OldProjDepTreeFactor only supports log and real semirings as input.");
        }
        
        // All internal computation is done in the logDomain
        // since (for example) pi the product of all incoming false messages
        // would overflow.
        EdgeScores es = getLogOddsRatios(inMsgs);
        double pi = getLogProductOfAllFalseMessages(inMsgs);

        // Compute the dependency tree marginals, summing over all projective
        // spanning trees via the inside-outside algorithm.
        DepIoChart chart = ProjectiveDependencyParser.insideOutsideSingleRoot(es.root, es.child);

        // partition = pi * \sum_{y \in Trees} \prod_{edge \in y} weight(edge) 
        // Here we store the log partition.
        double partition = pi + chart.getLogPartitionFunction();

        if (log.isTraceEnabled()) {
            log.trace(String.format("partition: %.2f", partition));
        }
        
        // Create the messages and stage them in the Messages containers.
        for (int i=0; i<inMsgs.length; i++) {
            VarTensor inMsg = inMsgs[i];
            VarTensor outMsg = outMsgs[i];
            LinkVar link = (LinkVar) inMsg.getVars().get(0);
            
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
                    if (!DoubleArrays.contains(es.root, Double.NEGATIVE_INFINITY, 1e-13) && !EdgeScores.childContains(es.child, Double.NEGATIVE_INFINITY, 1e-13)) {
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
            double inMsgTrue = s.toLogProb(inMsg.getValue(SpanVar.TRUE));
            double inMsgFalse = s.toLogProb(inMsg.getValue(SpanVar.FALSE));
            
            double outMsgTrue = beliefTrue - inMsgTrue;
            double outMsgFalse = beliefFalse - inMsgFalse;
            
            outMsgTrue = (inMsgTrue == Double.NEGATIVE_INFINITY) ? Double.NEGATIVE_INFINITY : outMsgTrue;
            outMsgFalse = (inMsgFalse == Double.NEGATIVE_INFINITY) ? Double.NEGATIVE_INFINITY : outMsgFalse;
            
            setOutMsgs(outMsg, link, outMsgTrue, outMsgFalse);
        }
    }
//        
//    private void fixEdge(FgNode parent, Messages[] msgs, boolean logDomain, boolean normalizeMessages, FgEdge outEdge) {
//        log.trace("Fixing edge: " + outEdge);
//
//        // Note on logDomain: all internal computation is done in the logDomain
//        // since (for example) pi the product of all incoming false messages
//        // would overflow.        
//        assert (this == parent.getFactor());        
//        double[] root = new double[n];
//        double[][] child = new double[n][n];
//        getLogOddsRatios(parent, msgs, logDomain, root, child);
//        double pi = getProductOfAllFalseMessages(parent, msgs, logDomain);
//
//        // Get the incoming messages.
//        FgEdge inEdge = outEdge.getOpposing();
//        DenseFactor inMsg = msgs[inEdge.getId()].message;
//        double inMsgTrue, inMsgFalse;
//        if (logDomain) {
//            inMsgTrue = inMsg.getValue(LinkVar.TRUE);
//            inMsgFalse = inMsg.getValue(LinkVar.FALSE);
//        } else {
//            inMsgTrue = FastMath.log(inMsg.getValue(LinkVar.TRUE));
//            inMsgFalse = FastMath.log(inMsg.getValue(LinkVar.FALSE));                
//        }
//        // Divide out the skipped edge ahead of time.
//        // This is equivalent to setting the odds ratio to 1.0.
//        LinkVar link = (LinkVar) outEdge.getVar();
//        if (link.parent == -1) {
//            root[link.child] = 0.0;
//        } else {
//            child[link.parent][link.child] = 0.0;
//        }
//        pi -= inMsgFalse;
//        
//        // Compute the dependency tree marginals, summing over all projective
//        // spanning trees via the inside-outside algorithm.
//        DepIoChart chart = ProjectiveDependencyParser.insideOutsideAlgorithm(root, child);
//
//        // partition = pi * \sum_{y \in Trees} \prod_{edge \in y} weight(edge) 
//        // Here we store the log partition.
//        double partition = pi + chart.getLogPartitionFunction();
//        if (log.isTraceEnabled()) {
//            log.trace(String.format("partition: %.2f", partition));
//        } 
//        
//        // Create the messages and stage them in the Messages containers.
//        // The beliefs are computed as follows.
//        double beliefTrue = pi + chart.getLogSumOfPotentials(link.getParent(), link.getChild());
//        double beliefFalse = safeLogSubtract(partition, beliefTrue);        
//        // Don't divide out.
//        double outMsgTrue = beliefTrue;
//        double outMsgFalse = beliefFalse;        
//        setOutMsgs(msgs, logDomain, normalizeMessages, outEdge, link, outMsgTrue, outMsgFalse);
//    }
    
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
        checkLinkWeights(es);
        return es;
    }
    
    private void checkLinkWeights(EdgeScores es) {
        
        // Keep track of the minimum and maximum odds ratios, in order to detect
        // possible numerical precision issues.
        double minOddsRatio = Double.POSITIVE_INFINITY;
        double maxOddsRatio = Double.NEGATIVE_INFINITY;
        
        for (int i=0; i<es.root.length; i++) {
            double oddsRatio = es.root[i];
            // Check min/max.
            if (oddsRatio < minOddsRatio && oddsRatio != Double.NEGATIVE_INFINITY) {
                // Don't count *negative* infinities when logging extreme odds ratios.
                minOddsRatio = oddsRatio;
            }
            if (oddsRatio > maxOddsRatio) {
                maxOddsRatio = oddsRatio;
            }
        }
        for (int i=0; i<es.child.length; i++) {
            for (int j=0; j<es.child[i].length; j++) {
                if (i == j) {
                    continue;
                }
                double oddsRatio = es.child[i][j];
                // Check min/max.
                if (oddsRatio < minOddsRatio && oddsRatio != Double.NEGATIVE_INFINITY) {
                    // Don't count *negative* infinities when logging extreme odds ratios.
                    minOddsRatio = oddsRatio;
                }
                if (oddsRatio > maxOddsRatio) {
                    maxOddsRatio = oddsRatio;
                }
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

    /** Sets the outgoing messages. */
    private void setOutMsgs(VarTensor outMsg, LinkVar link, double outMsgTrue, double outMsgFalse) {

        // Set the outgoing messages.
        Algebra s = outMsg.getAlgebra();
        outMsg.setValue(SpanVar.FALSE, s.fromLogProb(outMsgFalse));
        outMsg.setValue(SpanVar.TRUE, s.fromLogProb(outMsgTrue));

        if (log.isTraceEnabled()) {
            log.trace(String.format("outMsgTrue: %s = %.2f", link.getName(), outMsg.getValue(LinkVar.TRUE)));
            log.trace(String.format("outMsgFalse: %s = %.2f", link.getName(), outMsg.getValue(LinkVar.FALSE)));
        }

        assert !outMsg.containsBadValues() : "message = " + outMsg;
    }

    @Override
    public double getExpectedLogBelief(VarTensor[] inMsgs) {
        if (n == 0) {
            return 0.0;
        }
        EdgeScores ratios = getLogOddsRatios(inMsgs);
        double logPi = getLogProductOfAllFalseMessages(inMsgs);

        Algebra s = Algebras.LOG_SIGN_ALGEBRA;
        Pair<O1DpHypergraph, Scores> pair = HyperDepParser.insideEntropyFoe(ratios.root, ratios.child, s, InsideOutsideDepParse.singleRoot);
        O1DpHypergraph graph = pair.get1();
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
            return new SimpleProjDepTreeFactor(0, VarType.OBSERVED);
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
            log.warn("Tree has more than one arc to root.");
            return s.zero();
        }
        int[] parents = getParents(n, vc);
        if (!DepTree.isDepTree(parents, true, InsideOutsideDepParse.singleRoot)) {
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

}
