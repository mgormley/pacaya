package edu.jhu.parse.ilp;


public class FactorDeltaGenerator extends IdentityDeltaGenerator implements DeltaGenerator {
    
    private double factor;
    private int numPerSide;

    public FactorDeltaGenerator(double factor, int numPerSide) {
        this.factor = factor;
        this.numPerSide = numPerSide;
    }
    
    public DeltaList getDeltas(double weight) {
        DeltaList deltas = super.getDeltas(weight);
        for (int i = -numPerSide; i <= numPerSide; i++) {
            if (i == 0) {
                // Don't generate the identity delta
                continue;
            }
            String deltaId = "mult" + Math.pow(factor, i);
            double newWeight = weight * Math.pow(factor, i);
            // Only keep valid probabilities
            if (newWeight <= 1.0 && newWeight >= 0.0) {
                deltas.add(deltaId, newWeight);
            }
        }
        return deltas;
    }

}
