package edu.jhu.hltcoe.parse.cky;

import edu.jhu.hltcoe.parse.cky.Chart.BackPointer;
import edu.jhu.hltcoe.util.map.IntObjectHashMap;
import edu.jhu.hltcoe.util.map.OpenIntDoubleHashMapWithDefault;

/**
 * Cell that stores every entries in a single hash table.
 * 
 * @author mgormley
 * 
 */
public class SingleHashChartCell implements ChartCell {
    
    private static class Storage {        
        final double maxScore;
        final BackPointer bp;
        public Storage(double maxScore, BackPointer bp) {
            this.maxScore = maxScore;
            this.bp = bp;
        }
    }
    
    private IntObjectHashMap<Storage> table;
    private int[] ntsArray;    
    private boolean isClosed;

    public SingleHashChartCell(CnfGrammar grammar) {
        table = new IntObjectHashMap<Storage>();
        isClosed = false;
    }
    
    public final void updateCell(int mid, Rule r, double score) {
        assert(!isClosed);
        int nt = r.getParent();
        
        Storage entry = table.get(nt);
        double curMax = (entry != null) ? entry.maxScore : Double.NEGATIVE_INFINITY; 
        if (score > curMax) {
            table.put(nt, new Storage(score, new BackPointer(r, mid)));
        }
    }

    public final BackPointer getBp(int symbol) {
        Storage entry = table.get(symbol);
        return (entry != null) ? entry.bp : null;
    }
    
    public final double getMaxScore(int symbol) {
        Storage entry = table.get(symbol);
        return (entry != null) ? entry.maxScore : Double.NEGATIVE_INFINITY;
    }
    
    public final int[] getNts() {
        if (isClosed) {
            return ntsArray;
        } else {
            return table.keys();
        }
    }

    private static class FullMaxScores implements MaxScoresSnapshot {

        private IntObjectHashMap<Storage> table;

        public FullMaxScores(IntObjectHashMap<Storage> table) {
            this.table = table;
        }

        @Override
        public double getMaxScore(int symbol) {
            return table.get(symbol).maxScore;
        }
        
    }
    
    public MaxScoresSnapshot getMaxScoresSnapshot() {
        return new FullMaxScores(new IntObjectHashMap<Storage>(table));
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = table.keys();
    }
    
}