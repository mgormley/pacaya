package edu.jhu.hltcoe.parse.cky;

import java.util.Arrays;

import edu.jhu.hltcoe.parse.cky.Chart.BackPointer;
import gnu.trove.TIntArrayList;

/**
 * Cell that stores every possible entry explicitly. This is suitable for
 * grammars with a very small number of non-terminals (e.g. the DMV), where Hash
 * lookups would slow down the parsing.
 * 
 * The key speedup here is avoiding any hasing whatsoever. The nonterminals set
 * is not represented as a HashSet but instead as an IntArrayList where we use
 * the presence of a null in bps[nt] to indicate that a nonterminal with id nt
 * is not in the set.
 * 
 * In addition we "close" the cell after processing it, so that future calls to
 * getNts() will used a cached array of ints.
 * 
 * @author mgormley
 * 
 */
public class FullChartCell implements ChartCell {
    
    private final double[] maxScores;
    private final BackPointer[] bps;
    private final TIntArrayList nts;
    private int[] ntsArray;
    
    private boolean isClosed;

    public FullChartCell(CnfGrammar grammar) {
        maxScores = new double[grammar.getNumNonTerminals()];
        bps = new BackPointer[grammar.getNumNonTerminals()];
        nts = new TIntArrayList();

        isClosed = false;
        
        // Initialize scores to negative infinity.
        Arrays.fill(maxScores, Double.NEGATIVE_INFINITY);
    }

    public void reset() {
        Arrays.fill(maxScores, Double.NEGATIVE_INFINITY);
        Arrays.fill(bps, null);
        nts.clear();
        isClosed = false;
        ntsArray = null;
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        assert(!isClosed);
        int nt = r.getParent();
        if (bps[nt] == null) {
            // If the non-terminal hasn't been added yet, include it in the set of non terminals.
            nts.add(nt);
        }
        if (score > maxScores[nt]) {
            maxScores[nt] = score;
            bps[nt] = new BackPointer(r, mid);
        }
    }

    public final BackPointer getBp(int symbol) {
        return bps[symbol];
    }
    
    public final double getMaxScore(int symbol) {
        return maxScores[symbol];
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return nts.toNativeArray();
        }
    }

    private static class FullMaxScores implements MaxScoresSnapshot {

        private double[] maxScores;

        public FullMaxScores(double[] maxScores) {
            this.maxScores = maxScores;
        }

        @Override
        public double getMaxScore(int symbol) {
            return maxScores[symbol];
        }
        
    }
    
    public MaxScoresSnapshot getMaxScoresSnapshot() {
        return new FullMaxScores(maxScores);
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = nts.toNativeArray();
    }
    
}