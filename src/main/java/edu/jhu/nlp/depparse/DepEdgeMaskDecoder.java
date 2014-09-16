package edu.jhu.nlp.depparse;

import java.util.Arrays;

import org.apache.log4j.Logger;

import edu.jhu.gm.app.Decoder;
import edu.jhu.gm.data.UFgExample;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.parse.dep.EdgeScores;
import edu.jhu.prim.sort.IntDoubleSort;
import edu.jhu.prim.tuple.Pair;

/**
 * Decodes from the marginals of a factor graph for dependency parsing to a {@link DepEdgeMask}
 * indicating the edges that should be pruned from subsequent dependency parsers.
 * 
 * @author mgormley
 */
public class DepEdgeMaskDecoder implements Decoder<AnnoSentence, DepEdgeMask> {

    public static class DepEdgeMaskDecoderPrm {
        public double pruneMargProp = 0.0001;
        public int maxPrunedHeads = 10;
    }
    
    private DepEdgeMaskDecoderPrm prm;
    
    public DepEdgeMaskDecoder(DepEdgeMaskDecoderPrm prm) {
        this.prm = prm;
    }

    private static final Logger log = Logger.getLogger(DepEdgeMaskDecoder.class);

    /**
     * Prune to only the most likely K (e.g. 10) heads per token from the first-order model. Also
     * for each token, prune any heads for which the marginal probability is less than propMaxMarg
     * (e.g. 0.0001) times the maximum head marginal for that token.
     * 
     * This follows footnote 10 in Martins et al. (2013).
     */
    @Override
    public DepEdgeMask decode(FgInferencer inf, UFgExample ex, AnnoSentence sent) {
        FactorGraph fg = ex.getFgLatPred();
        int n = sent.size();
        Pair<EdgeScores, Integer> pair = DepParseDecoder.getEdgeScores(inf, fg, n);
        EdgeScores scores = pair.get1();
        int linkVarCount = pair.get2();
        
        if (linkVarCount > 0) {
            return getDepEdgeMask(scores, prm.pruneMargProp, prm.maxPrunedHeads);
        } else {
            return null;
        }
    }

    /** See {@link #decode(FgInferencer, UFgExample, AnnoSentence)}. */
    public static DepEdgeMask getDepEdgeMask(EdgeScores scores, double propMaxMarg, int maxPrunedHeads) {
        int n = scores.root.length;
        DepEdgeMask mask = new DepEdgeMask(n, true);
        pruneByCount(scores, maxPrunedHeads, mask);
        pruneByMaxMarginal(scores, propMaxMarg, mask);
        checkValidMask(n, mask);
        return mask;
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
