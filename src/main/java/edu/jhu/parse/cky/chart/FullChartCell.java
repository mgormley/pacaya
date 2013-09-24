package edu.jhu.parse.cky.chart;

import java.util.Arrays;

import edu.jhu.data.Sentence;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart.BackPointer;
import edu.jhu.parse.cky.chart.Chart.ParseType;
import edu.jhu.prim.list.IntArrayList;
import edu.jhu.util.Utilities;

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

    /** Whether to compute the inside score or the max score. */
    private final boolean computeInside;
    /** The inside score or the max score. Indexed by the nonterminal type. */
    private final double[] scores;
    /** 
     * The backpointer to the children corresponding to the max score. 
     * Indexed by the nonterminal type.
     */
    private final BackPointer[] bps;
    /**
     * When the chart cell is not closed, an array list containing the set of
     * nonterminals that have non-null backpointers. When the chart cell is
     * closed, the <code>ntsArray</code> should be used instead of this.
     */
    private final IntArrayList nts;
    /**
     * When the chart cell is closed, this is a copy of the elements in <code>nts</code>.
     */
    private int[] ntsArray;
    /**
     * Whether this chart cell has been closed. When closed, the chart cell will
     * throw an exception if an attempt to update it is made.
     */
    private boolean isClosed;

    private int i;
    private int j;
    
    public FullChartCell(int i, int j, CnfGrammar grammar, ParseType parseType) {
        this.i = i;
        this.j = j;
        scores = new double[grammar.getNumNonTerminals()];
        bps = new BackPointer[grammar.getNumNonTerminals()];
        nts = new IntArrayList();

        isClosed = false;
        
        // Initialize scores to negative infinity.
        Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        
        if (parseType == ParseType.INSIDE){
            computeInside = true;
        } else {
            computeInside = false;
        }
    }

    public void reset(Sentence sentence) {
        Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        Arrays.fill(bps, null);
        nts.clear();
        isClosed = false;
        ntsArray = null;
    }
    
    public void updateCell(int nt, double score, int mid, Rule r) {
        assert(!isClosed);
        if (bps[nt] == null) {
            // If the non-terminal hasn't been added yet, include it in the set of non terminals.
            nts.add(nt);
        }
        if (computeInside) {
            // Compute the inside score.
            scores[nt] = Utilities.logAdd(scores[nt], score);
            // Add a dummy backpointer, so that the above non-null check still works.
            bps[nt] = BackPointer.NON_NULL_BACKPOINTER;
        } else {
            // Compute the viterbi score.
            if (score > scores[nt]) {
                scores[nt] = score;
                bps[nt] = new BackPointer(r, mid);
            }
        }
    }

    public final BackPointer getBp(int symbol) {
        return bps[symbol];
    }
    
    public final double getScore(int symbol) {
        return scores[symbol];
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return nts.toNativeArray();
        }
    }

    public ScoresSnapshot getScoresSnapshot() {
        return new FullScores(scores);
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = nts.toNativeArray();
        //log.trace("FullChartCell: i, j, nts.size(): %d %d %d\n", i, j, nts.size());
    }
    
}