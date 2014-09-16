package edu.jhu.parse.cky.chart;

import java.util.Arrays;

import edu.jhu.nlp.data.Sentence;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart.BackPointer;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.arrays.BoolArrays;
import edu.jhu.prim.arrays.DoubleArrays;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.util.Prng;

/**
 * This class is identical to <code>FullChartCell</code> except that it adds the
 * option of breaking ties using a cached random number (jitter) for each
 * nonterminal type.
 * 
 * @author mgormley
 * 
 */
public class FullTieBreakerChartCell implements ChartCell {
    
    private static final double PROB_EQUALS_TOLERANCE = 1e-13;
    
    private final double[] maxScores;
    private final BackPointer[] bps;
    private final IntArrayList nts;
    private int[] ntsArray;
    
    private boolean isClosed;

    private final boolean breakTies;
    private final double[] maxJit;
    private final boolean[] maxHasJit;
    
    public FullTieBreakerChartCell(CnfGrammar grammar, boolean breakTies) {
        maxScores = new double[grammar.getNumNonTerminals()];
        bps = new BackPointer[grammar.getNumNonTerminals()];
        nts = new IntArrayList();

        isClosed = false;
        
        // Initialize scores to negative infinity.
        DoubleArrays.fill(maxScores, Double.NEGATIVE_INFINITY);
        
        // Tie breaking fields.
        this.breakTies = breakTies;
        maxJit = new double[grammar.getNumNonTerminals()];
        // -- uses default initialization to false.
        maxHasJit = new boolean[grammar.getNumNonTerminals()];
    }
    
    public void reset(Sentence sentence) {
        DoubleArrays.fill(maxScores, Double.NEGATIVE_INFINITY);
        Arrays.fill(bps, null);
        nts.clear();
        isClosed = false;
        ntsArray = null;
        DoubleArrays.fill(maxJit, 0.0);
        BoolArrays.fill(maxHasJit, false);
    }
    
    public final void updateCell(int nt, double score, int mid, Rule r) {
        assert(!isClosed);
        if (bps[nt] == null) {
            // If the non-terminal hasn't been added yet, include it in the set of non terminals.
            nts.add(nt);
        }
        if (breakTies) {
            double jit = 0.0;
            boolean hasJit = false;
            
            // Compare sentenceProb and prob. If they are equal break the tie by 
            // comparing the jitter.
            int diff = Primitives.compare(score, maxScores[nt], PROB_EQUALS_TOLERANCE);
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