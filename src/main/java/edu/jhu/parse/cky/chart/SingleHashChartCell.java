package edu.jhu.parse.cky.chart;

import edu.jhu.nlp.data.Sentence;
import edu.jhu.parse.cky.CnfGrammar;
import edu.jhu.parse.cky.Rule;
import edu.jhu.parse.cky.chart.Chart.BackPointer;
import edu.jhu.prim.Primitives;
import edu.jhu.prim.map.IntObjectHashMap;
import edu.jhu.prim.util.Prng;

/**
 * Cell that stores every entry in a single hash table.
 * 
 * @author mgormley
 * 
 */
public class SingleHashChartCell implements ChartCell {

    private static final double PROB_EQUALS_TOLERANCE = 1e-13;

    private static class Storage {        
        final double maxScore;
        final BackPointer bp;
        double maxJit;
        boolean maxHasJit;
        public Storage(double maxScore, BackPointer bp) {
            this.maxScore = maxScore;
            this.bp = bp;
            this.maxHasJit = false;
        }
        public Storage(double maxScore, BackPointer bp, double maxJit, boolean maxHasJit) {
            this.maxScore = maxScore;
            this.bp = bp;
            this.maxJit = maxJit;
            this.maxHasJit = maxHasJit;
        }        
    }
    
    private IntObjectHashMap<Storage> table;
    private int[] ntsArray;    
    private boolean isClosed;
    private final boolean breakTies;

    public SingleHashChartCell(CnfGrammar grammar, boolean breakTies) {
        table = new IntObjectHashMap<Storage>();
        isClosed = false;
        this.breakTies = breakTies;
    }

    public void reset(Sentence sentence) {
        table.clear();
        ntsArray = null;
        isClosed = false;
    }
    
    public final void updateCell(int nt, double score, int mid, Rule r) {
        assert(!isClosed);
        
        Storage entry = table.get(nt);
        
        if (breakTies) {
            if (entry == null) {
                entry = new Storage(Double.NEGATIVE_INFINITY, null, 0.0, false);
            }
            double jit = 0.0;
            boolean hasJit = false;
            
            // Compare sentenceProb and prob. If they are equal break the tie by 
            // comparing the jitter.
            int diff = Primitives.compare(score, entry.maxScore, PROB_EQUALS_TOLERANCE);
            if (diff == 0) {
                // Create the jitter for the current contender. 
                jit = Prng.nextDouble();
                hasJit = true;
                if (!entry.maxHasJit) {
                    // Lazily create the jitter for the current max.
                    entry.maxJit = Prng.nextDouble();
                    entry.maxHasJit = true;
                }
                diff = Double.compare(jit, entry.maxJit);
            }
            if(diff > 0) {
                table.put(nt, new Storage(score, new BackPointer(r, mid), jit, hasJit));
            }
        } else {
            final double curMax = (entry != null) ? entry.maxScore : Double.NEGATIVE_INFINITY; 
            if (score > curMax) {
                table.put(nt, new Storage(score, new BackPointer(r, mid)));
            }
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