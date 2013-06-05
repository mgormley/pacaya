package edu.jhu.hltcoe.parse.cky.chart;

import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.MaxScoresSnapshot;
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

    private OpenIntDoubleHashMapWithDefault maxScores;
    private IntObjectHashMap<BackPointer> bps;
    private int[] ntsArray;    
    private boolean isClosed;

    public DoubleHashChartCell(CnfGrammar grammar) {
        maxScores = new OpenIntDoubleHashMapWithDefault(Double.NEGATIVE_INFINITY);
        bps = new IntObjectHashMap<BackPointer>();

        isClosed = false;
    }

    public void reset() {
        maxScores.clear();
        bps.clear();
        ntsArray = null;
        isClosed = false;
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        assert(!isClosed);
        int nt = r.getParent();
        
        if (score > maxScores.get(nt)) {
            maxScores.put(nt, score);
            bps.put(nt, new BackPointer(r, mid));
        }
    }

    public final BackPointer getBp(int symbol) {
        return bps.get(symbol);
    }
    
    public final double getMaxScore(int symbol) {
        return maxScores.get(symbol);
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return bps.keys();
        }
    }

    private static class FullMaxScores implements MaxScoresSnapshot {

        private OpenIntDoubleHashMapWithDefault maxScores;

        public FullMaxScores(OpenIntDoubleHashMapWithDefault maxScores) {
            this.maxScores = maxScores;
        }

        @Override
        public double getMaxScore(int symbol) {
            return maxScores.get(symbol);
        }
        
    }
    
    public MaxScoresSnapshot getMaxScoresSnapshot() {
        return new FullMaxScores((OpenIntDoubleHashMapWithDefault)maxScores.clone());
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = bps.keys();
    }
    
}