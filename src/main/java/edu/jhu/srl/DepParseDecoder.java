package edu.jhu.srl;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.DepTree;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.prim.sort.IntDoubleSort;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.prim.util.math.FastMath;

public class DepParseDecoder {

    private static final Logger log = Logger.getLogger(DepParseDecoder.class);

    public static int[] getParents(List<VarTensor> margs, List<Var> vars, int n) {        
        // Build up the beliefs about the link variables (if present),
        // and compute the MBR dependency parse.
        Pair<EdgeScores, Integer> pair = getEdgeScores(margs, vars, n, false);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        
        if (linkVarCount > 0) {
            // Get MBR parse, by finding the argmax tree where we treat the
            // score of a tree as the sum of the edge scores.
            int[] parents = new int[n];
            Arrays.fill(parents, DepTree.EMPTY_POSITION);
            ProjectiveDependencyParser.parse(scores.root, scores.child, parents);
            return parents;
        } else {
            return null;
        }
    }

    private static Pair<EdgeScores, Integer> getEdgeScores(List<VarTensor> margs, List<Var> vars, int n, boolean logOdds) {
        int linkVarCount = 0;
        EdgeScores scores = new EdgeScores(n, Double.NEGATIVE_INFINITY);
        for (int varId = 0; varId < vars.size(); varId++) {
            Var var = vars.get(varId);
            VarTensor marg = margs.get(varId);
            if (var instanceof LinkVar && (var.getType() == VarType.LATENT || var.getType() == VarType.PREDICTED)) {
                LinkVar link = ((LinkVar)var);
                int c = link.getChild();
                int p = link.getParent();

                double belief;
                if (logOdds) {
                    // TODO: Using logOdds is the method of MBR decoding
                    // prescribed in Smith & Eisner (2008). However, this breaks the parser
                    // when the log-odds are positive infinity.
                    belief = FastMath.log(marg.getValue(LinkVar.TRUE) / marg.getValue(LinkVar.FALSE));
                } else {
                    belief = marg.getValue(LinkVar.TRUE);
                }
                if (p == -1) {
                    scores.root[c] = belief;
                } else {
                    scores.child[p][c] = belief;
                }
                linkVarCount++;
            }
        }
        return new Pair<EdgeScores, Integer>(scores, linkVarCount);
    }

    /**
     * Prune to only the most likely K = 10 heads per token from the first-order model. Also for
     * each token, prune any heads for which the marginal probability is less than propMaxMarg (e.g.
     * 0.0001) times the maximum head marginal for that token.
     * 
     * This follows footnote 10 in Martins et al. (2013).
     */
    public static DepEdgeMask getDepEdgeMask(List<VarTensor> margs, List<Var> vars, int n, double propMaxMarg,
            int maxPrunedHeads) {
        Pair<EdgeScores, Integer> pair = getEdgeScores(margs, vars, n, false);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        if (linkVarCount > 0) {
            DepEdgeMask mask = new DepEdgeMask(n, true);
            pruneByCount(scores, maxPrunedHeads, mask);
            pruneByMaxMarginal(scores, propMaxMarg, mask);
            checkValidMask(n, mask);
            return mask;
        } else {
            return null;
        }
    }

    /**
     * For each token, keep only the top K heads in terms of the marginal scores.
     * 
     * @param scores Marginals for each edge.
     * @param maxPrunedHeads The number of heads to keep, K. Set to Integer.MAX_VALUE for no
     *            pruning.
     * @param mask Output pruning mask.
     */
    private static void pruneByCount(EdgeScores scores, int maxPrunedHeads, DepEdgeMask mask) {        
        int n = scores.root.length;
        // For each token...
        for (int c = 0; c < n; c++) {
            // Keep only the top K.
            int[] heads = IntDoubleSort.getIntIndexArray(n);
            double[] margs = new double[n];
            for (int p = -1; p < n; p++) {
                if (p == c) { continue; }
                int idx = (p == -1) ? c : p;
                heads[idx] = p;
                margs[idx] = scores.getScore(p, c);
            }
            // Sort by the marginals increasing, keeping the heads in sync.
            IntDoubleSort.sortValuesAsc(margs, heads);
            // Prune the first (N - K).
            for (int i = 0; i < (n - maxPrunedHeads); i++) {
                mask.setIsKept(heads[i], c, false);
            }
        }
    }

    /**
     * For each token, prune any heads for which the marginal probability is less than propMaxMarg
     * (e.g. 0.0001) times the maximum head marginal for that token.
     * 
     * @param scores Marginals for each edge.
     * @param propMaxMarg The proportion of the max marginal below which edges will be pruned. Set
     *            to 0 for no pruning.
     * @param mask Output pruning mask.
     */
    private static void pruneByMaxMarginal(EdgeScores scores, double propMaxMarg, DepEdgeMask mask) {
        int n = scores.root.length;
        // Get the max head marginal for each token.
        double[] maxMargForTok = new double[n];
        Arrays.fill(maxMargForTok, Double.NEGATIVE_INFINITY);
        for (int c=0; c<n; c++) {
            for (int p=-1; p<n; p++) {
                if (p == c) { continue; }
                double marg = scores.getScore(p, c);
                if (marg > maxMargForTok[c]) {
                    maxMargForTok[c] = marg;                                
                }
            }
        }
        // For each token, prune any heads for which the marginal
        // probability is less than propMaxMarg (e.g. 0.0001) times the
        // maximum head marginal for that token.
        for (int p=-1; p<n; p++) {
            for (int c=0; c<n; c++) {
                if (p == c) { continue; }
                double marg = scores.getScore(p, c);
                // In probability domain: marg < propMaxMarg * maxMargForTok[c];
                if (log.isTraceEnabled()) {
                    log.trace(String.format("p=%d c=%d marg=%f maxMarg=%f thresh=%f", p, c, marg, maxMargForTok[c], propMaxMarg * maxMargForTok[c]));
                }
                if (marg < propMaxMarg * maxMargForTok[c]) {
                    mask.setIsKept(p, c, false);
                }
            }
        }
    }

    private static void checkValidMask(int n, DepEdgeMask mask) {
        // Check that each child has at least one parent. This should always be the case.
        for (int c=0; c<n; c++) {
            if (mask.getParentCount(c) == 0) {
                String msg = String.format("") + mask;
                throw new IllegalStateException(msg);
            }
        }
    }

}
