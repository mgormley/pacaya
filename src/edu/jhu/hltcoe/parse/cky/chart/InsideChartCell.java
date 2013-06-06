package edu.jhu.hltcoe.parse.cky.chart;

import java.util.Arrays;

import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.util.Utilities;
import gnu.trove.TIntArrayList;

/**
 * Cell that stores every possible entry explicitly, just as in FullChartCell.
 * The difference is that this version treats the scores as inside
 * probabilities. Thus, they are updated by sum instead of max.
 * 
 * @author mgormley
 * 
 */
public class InsideChartCell implements ChartCell {
    
    private final double[] insideScores;
    private final BackPointer[] bps;
    private final TIntArrayList nts;
    private int[] ntsArray;
    
    private boolean isClosed;

    public InsideChartCell(CnfGrammar grammar) {
        insideScores = new double[grammar.getNumNonTerminals()];
        bps = new BackPointer[grammar.getNumNonTerminals()];
        nts = new TIntArrayList();

        isClosed = false;
        
        // Initialize scores to negative infinity.
        Arrays.fill(insideScores, Double.NEGATIVE_INFINITY);
    }

    public void reset() {
        Arrays.fill(insideScores, Double.NEGATIVE_INFINITY);
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
        insideScores[nt] = Utilities.logAdd(insideScores[nt], score);
        bps[nt] = new BackPointer(r, mid);
    }

    public final BackPointer getBp(int symbol) {
        return bps[symbol];
    }
    
    public final double getScore(int symbol) {
        return insideScores[symbol];
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return nts.toNativeArray();
        }
    }
    
    public ScoresSnapshot getScoresSnapshot() {
        return new FullScores(insideScores);
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = nts.toNativeArray();
    }
    
}