package edu.jhu.parse;

import java.util.ArrayList;

import edu.jhu.util.tuple.Pair;

public class IdentityDeltaGenerator implements DeltaGenerator {

    public static class Delta extends Pair<String, Double> {
     
        public Delta(String deltaId, double newWeight) {
            super(deltaId, newWeight);
        }

        public String getId() {
            return get1();
        }
        
        public double getWeight() {
            return get2();
        }
        
    }
    public static class DeltaList extends ArrayList<Delta> {

        private static final long serialVersionUID = 1L;

        public void add(String deltaId, double newWeight) {
            add(new Delta(deltaId, newWeight));
        }
        
    }
    
    public static final String IDENTITY_DELTA_ID = "identity";

    public IdentityDeltaGenerator() {
        // do nothing
    }
    
    public DeltaList getDeltas(double weight) {
        DeltaList deltas = new DeltaList();
        deltas.add(IDENTITY_DELTA_ID, weight);
        return deltas;
    }

}
