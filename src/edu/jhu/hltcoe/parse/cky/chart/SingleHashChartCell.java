package edu.jhu.hltcoe.parse.cky.chart;

import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.parse.cky.CnfGrammar;
import edu.jhu.hltcoe.parse.cky.Rule;
import edu.jhu.hltcoe.parse.cky.chart.Chart.BackPointer;
import edu.jhu.hltcoe.util.map.IntObjectHashMap;

/**
 * Cell that stores every entry in a single hash table.
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

    public void reset(Sentence sentence) {
        table.clear();
        ntsArray = null;
        isClosed = false;
    }
    
    public final void updateCell(int nt, double score, int mid, Rule r) {
        assert(!isClosed);
        
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
    
    public final double getScore(int symbol) {
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

    private static class SingleHashScores implements ScoresSnapshot {

        private IntObjectHashMap<Storage> table;

        public SingleHashScores(IntObjectHashMap<Storage> table) {
            this.table = table;
        }

        @Override
        public double getScore(int symbol) {
            return table.get(symbol).maxScore;
        }
        
    }
    
    public ScoresSnapshot getScoresSnapshot() {
        return new SingleHashScores(new IntObjectHashMap<Storage>(table));
    }

    @Override
    public void close() {
        isClosed = true;
        ntsArray = table.keys();
    }
    
}