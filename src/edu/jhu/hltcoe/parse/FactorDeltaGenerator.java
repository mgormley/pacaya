package edu.jhu.hltcoe.parse;

import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.DmvModel;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Quadruple;

public class FactorDeltaGenerator extends IdentityDeltaGenerator implements DeltaGenerator {

    private double factor;
    private int numPerSide;

    public FactorDeltaGenerator(double factor, int numPerSide) {
        this.factor = factor;
        this.numPerSide = numPerSide;
    }

    @Override
    public Map<Quadruple<Label, String, Label, String>, Double> getCWDeltas(DmvModel dmvModel) {
        Map<Quadruple<Label, String, Label, String>, Double> cwDeltas = super.getCWDeltas(dmvModel);

        for (Entry<Pair<Label, String>, LabeledMultinomial<Label>> entry : dmvModel.getChooseWeights().entrySet()) {
            for (Entry<Label, Double> subEntry : entry.getValue().entrySet()) {
                double weight = subEntry.getValue();
                for (int i = -numPerSide; i <= numPerSide; i++) {
                    if (i == 0) {
                        // Don't generate the identity delta
                        continue;
                    }
                    String deltaId = "mult" + Math.pow(factor, i);
                    Quadruple<Label, String, Label, String> key = getDeltaKey(entry.getKey(), subEntry.getKey(), deltaId);
                    double newWeight = weight * Math.pow(factor, i);
                    // Only keep valid probabilities
                    if (newWeight <= 1.0 && newWeight >= 0.0) {
                        cwDeltas.put(key, newWeight);
                    }
                }
            }
        }
        return cwDeltas;
    }

}
