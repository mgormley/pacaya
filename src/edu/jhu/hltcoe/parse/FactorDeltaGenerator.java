package edu.jhu.hltcoe.parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Quadruple;
import edu.jhu.hltcoe.util.Triple;

public class FactorDeltaGenerator implements DeltaGenerator {

    private double factor;
    private int numPerSide;
    
    public FactorDeltaGenerator(double factor, int numPerSide) {
        this.factor = factor;
        this.numPerSide = numPerSide;
    }

    @Override
    public Map<Quadruple<Label, String, Label, String>, Double> getCWDeltas(
            Map<Triple<Label, String, Label>, Double> chooseWeights) {
        Map<Quadruple<Label, String, Label, String>, Double> cwDeltas = new HashMap<Quadruple<Label, String, Label, String>, Double>();
        for (Entry<Triple<Label,String,Label>,Double> entry : chooseWeights.entrySet()) {
            Label parent = entry.getKey().get1();
            String lr = entry.getKey().get2();
            Label child = entry.getKey().get3();
            double weight = entry.getValue();
            
            for (int i = -numPerSide; i<=numPerSide; i++) {
                String deltaId = "mult" + Math.pow(factor, i);
                Quadruple<Label, String, Label, String> key = new Quadruple<Label, String, Label, String>(parent,lr,child,deltaId);
                double newWeight = weight * Math.pow(factor, i);
                // Only keep valid probabilities
                if (newWeight <= 1.0 && newWeight >= 0.0) {
                    cwDeltas.put(key, newWeight);
                }
            }
        }        
        return cwDeltas;
    }

}
