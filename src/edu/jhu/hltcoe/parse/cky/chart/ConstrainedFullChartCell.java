package edu.jhu.hltcoe.parse.cky.chart;

import java.util.Arrays;

import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.DmvRule;
import edu.jhu.hltcoe.parse.cky.DmvRule.DmvRuleType;
import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.parse.cky.chart.Chart.ParseType;
import edu.jhu.hltcoe.util.Utilities;
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
public final class ConstrainedFullChartCell implements ChartCell {

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
    private final TIntArrayList nts;
    /**
     * When the chart cell is closed, this is a copy of the elements in <code>nts</code>.
     */
    private int[] ntsArray;
    /**
     * Whether this chart cell has been closed. When closed, the chart cell will
     * throw an exception if an attempt to update it is made.
     */
    private boolean isClosed;
    /** The start the span dominated by this chart cell. */
    private final int start;
    /** The end the span dominated by this chart cell. */
    private final int end;
    /** Indicates whether a parent/child arc is valid. Indexed by child position, parent position. */
    private boolean[][] validParent;
    
    public ConstrainedFullChartCell(int start, int end, boolean[][] validParent, CnfGrammar grammar, ParseType parseType) {
        this.start = start;
        this.end = end;
        this.validParent = validParent;
        scores = new double[grammar.getNumNonTerminals()];
        bps = new BackPointer[grammar.getNumNonTerminals()];
        nts = new TIntArrayList();

        isClosed = false;
        
        // Initialize scores to negative infinity.
        Arrays.fill(scores, Double.NEGATIVE_INFINITY);
        
        if (parseType == ParseType.INSIDE){
            computeInside = true;
        } else {
            computeInside = false;
        }
    }

    public void reset() {
//        Arrays.fill(scores, Double.NEGATIVE_INFINITY);
//        Arrays.fill(bps, null);
//        nts.clear();
//        isClosed = false;
//        ntsArray = null;
        throw new RuntimeException("Caching of constrained chart cells is not supported.");
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        assert(!isClosed);
        final DmvRule dmvRule = (DmvRule)r;
        if (dmvRule.getType() == DmvRuleType.STRUCTURAL) {
            // This is only true for structural rules.
            int leftHead = start / 2;
            int rightHead = end / 2;
            // Determine which is the head and child.
            boolean isLeftHead = dmvRule.isLeftHead();
            int head = isLeftHead ? leftHead : rightHead;
            int child = isLeftHead ? rightHead : leftHead;
            
            if (validParent[child] != null && validParent[child].length > 0) {
                // Check that the constraints allow this arc.
                if (!validParent[child][head]) {
                    return;
                }
            }
        }
        
        int nt = r.getParent();
        if (bps[nt] == null) {
            // If the non-terminal hasn't been added yet, include it in the set of non terminals.
            nts.add(nt);
        }
        if (computeInside) {
            // Compute the inside score.
            scores[nt] = Utilities.logAdd(scores[nt], score);
            // Add a dummy backpointer, so that the above non-null check still works.
            bps[nt] = new BackPointer(null, -1);
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
    }
    
}