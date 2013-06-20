package edu.jhu.hltcoe.parse;


public class FixedIntervalDeltaGenerator extends IdentityDeltaGenerator implements DeltaGenerator {

    private double interval;
    private int numPerSide;
    
    public FixedIntervalDeltaGenerator(double interval, int numPerSide) {
        this.interval = interval;
        this.numPerSide = numPerSide;
    }
    
    public DeltaList getDeltas(double weight) {
        DeltaList deltas = super.getDeltas(weight);
        for (int i = -numPerSide; i<=numPerSide; i++) {
            if (i == 0) {
                // Don't generate the identity delta
                continue;
            }
            String deltaId = "add" + interval * i;
            double newWeight = weight + interval * i;
            // Only keep valid probabilities
            if (newWeight <= 1.0 && newWeight >= 0.0) {
                deltas.add(deltaId, newWeight);
            }
        }
        return deltas;
    }
    
}
