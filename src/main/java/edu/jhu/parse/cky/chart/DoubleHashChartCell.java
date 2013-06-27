package edu.jhu.parse.cky.chart;

import edu.jhu.data.Sentence;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart.BackPointer;
import edu.jhu.util.collections.PIntObjectHashMap;
import edu.jhu.util.collections.PIntDoubleHashMap;

/**
 * Cell that stores every entries in two hash tables.
 * 
 * @author mgormley
 * 
 */
public class DoubleHashChartCell implements ChartCell {

    private PIntDoubleHashMap scores;
    private PIntObjectHashMap<BackPointer> bps;
    private int[] ntsArray;    
    private boolean isClosed;

    public DoubleHashChartCell(CnfGrammar grammar) {
        scores = new PIntDoubleHashMap(Double.NEGATIVE_INFINITY);
        bps = new PIntObjectHashMap<BackPointer>();

        isClosed = false;
    }

    public void reset(Sentence sentence) {
        scores.clear();
        bps.clear();
        ntsArray = null;
        isClosed = false;
    }
    
    public final void updateCell(int nt, double score, int mid, Rule r) {
        assert(!isClosed);
        
        if (score > scores.get(nt)) {
            scores.put(nt, score);
            bps.put(nt, new BackPointer(r, mid));
        }
    }

    public final BackPointer getBp(int symbol) {
        return bps.get(symbol);
    }
    
    public final double getScore(int symbol) {
        return scores.get(symbol);
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return bps.keys();
        }
    }

    private static class DoubleHashScores implements ScoresSnapshot {

        private PIntDoubleHashMap scores;

        public DoubleHashScores(PIntDoubleHashMap scores) {
            this.scores = scores;
        }

        @Override
        public double getScore(int symbol) {
            return scores.get(symbol);
        }
        
    }
    
    public ScoresSnapshot getScoresSnapshot() {
        return new DoubleHashScores(new PIntDoubleHashMap(scores));
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = bps.keys();
    }
    
}