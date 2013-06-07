package edu.jhu.hltcoe.parse.cky.chart;

import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;
import gnu.trove.TIntArrayList;

/**
 * This class optionally breaks ties using a cached random number (jitter).
 * 
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
public class FullTieBreakerChartCell implements ChartCell {
    
    private static final double PROB_EQUALS_TOLERANCE = 1e-13;
    
    private final double[] maxScores;
    private final BackPointer[] bps;
    private final TIntArrayList nts;
    private int[] ntsArray;
    
    private boolean isClosed;

    private final boolean breakTies;
    private final double[] maxJit;
    private final boolean[] maxHasJit;
    
    public FullTieBreakerChartCell(CnfGrammar grammar, boolean breakTies) {
        maxScores = new double[grammar.getNumNonTerminals()];
        bps = new BackPointer[grammar.getNumNonTerminals()];
        nts = new TIntArrayList();

        isClosed = false;
        
        // Initialize scores to negative infinity.
        Utilities.fill(maxScores, Double.NEGATIVE_INFINITY);
        
        // Tie breaking fields.
        this.breakTies = breakTies;
        maxJit = new double[grammar.getNumNonTerminals()];
        // -- uses default initialization to false.
        maxHasJit = new boolean[grammar.getNumNonTerminals()];
    }
    
    public void reset() {
        Utilities.fill(maxScores, Double.NEGATIVE_INFINITY);
        Utilities.fill(bps, null);
        nts.clear();
        isClosed = false;
        ntsArray = null;
        Utilities.fill(maxJit, 0.0);
        Utilities.fill(maxHasJit, false);
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        assert(!isClosed);
        int nt = r.getParent();
        if (bps[nt] == null) {
            // If the non-terminal hasn't been added yet, include it in the set of non terminals.
            nts.add(nt);
        }
        if (breakTies) {
            double jit = 0.0;
            boolean hasJit = false;
            
            // Compare sentenceProb and prob. If they are equal break the tie by 
            // comparing the jitter.
            int diff = Utilities.compare(score, maxScores[nt], PROB_EQUALS_TOLERANCE);
            if (diff == 0) {
                // Create the jitter for the current contender. 
                jit = Prng.nextDouble();
                hasJit = true;
                if (!maxHasJit[nt]) {
                    // Lazily create the jitter for the current max.
                    maxJit[nt] = Prng.nextDouble();
                    maxHasJit[nt] = true;
                }
                diff = Double.compare(jit, maxJit[nt]);
            }
            if(diff > 0) {
                maxScores[nt] = score;
                bps[nt] = new BackPointer(r, mid);
                maxJit[nt] = jit;
                maxHasJit[nt] = hasJit;
            }
        } else {
            if (score > maxScores[nt]) {
                maxScores[nt] = score;
                bps[nt] = new BackPointer(r, mid);
            }
        }
    }

    public final BackPointer getBp(int symbol) {
        return bps[symbol];
    }
    
    public final double getScore(int symbol) {
        return maxScores[symbol];
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return nts.toNativeArray();
        }
    }
    
    public ScoresSnapshot getScoresSnapshot() {
        return new FullScores(maxScores);
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = nts.toNativeArray();
    }
    
}