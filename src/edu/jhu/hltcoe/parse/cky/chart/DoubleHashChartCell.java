package edu.jhu.hltcoe.parse.cky.chart;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.util.map.IntObjectHashMap;
import edu.jhu.hltcoe.util.map.OpenIntDoubleHashMapWithDefault;

/**
 * Cell that stores every entries in two hash tables.
 * 
 * @author mgormley
 * 
 */
public class DoubleHashChartCell implements ChartCell {

    private OpenIntDoubleHashMapWithDefault scores;
    private IntObjectHashMap<BackPointer> bps;
    private int[] ntsArray;    
    private boolean isClosed;

    public DoubleHashChartCell(CnfGrammar grammar) {
        scores = new OpenIntDoubleHashMapWithDefault(Double.NEGATIVE_INFINITY);
        bps = new IntObjectHashMap<BackPointer>();

        isClosed = false;
    }

    public void reset(Sentence sentence) {
        scores.clear();
        bps.clear();
        ntsArray = null;
        isClosed = false;
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        assert(!isClosed);
        int nt = r.getParent();
        
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

        private OpenIntDoubleHashMapWithDefault scores;

        public DoubleHashScores(OpenIntDoubleHashMapWithDefault scores) {
            this.scores = scores;
        }

        @Override
        public double getScore(int symbol) {
            return scores.get(symbol);
        }
        
    }
    
    public ScoresSnapshot getScoresSnapshot() {
        return new DoubleHashScores((OpenIntDoubleHashMapWithDefault)scores.clone());
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = bps.keys();
    }
    
}