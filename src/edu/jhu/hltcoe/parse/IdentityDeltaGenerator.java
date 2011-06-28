package edu.jhu.hltcoe.parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Quadruple;
import edu.jhu.hltcoe.util.Triple;

public class IdentityDeltaGenerator implements DeltaGenerator {

    public static final String IDENTITY_DELTA_ID = "identity";
        
    @Override
    public Map<Quadruple<Label, String, Label, String>, Double> getCWDeltas(
            Map<Triple<Label, String, Label>, Double> chooseWeights) {
        Map<Quadruple<Label, String, Label, String>, Double> cwDeltas = new HashMap<Quadruple<Label, String, Label, String>, Double>();
        for (Entry<Triple<Label,String,Label>,Double> entry : chooseWeights.entrySet()) {
            Quadruple<Label, String, Label, String> key = getDeltaKey(entry.getKey(), IDENTITY_DELTA_ID);
            double weight = entry.getValue();
            cwDeltas.put(key, weight);
        }        
        return cwDeltas;
    }

    protected static Quadruple<Label, String, Label, String> getDeltaKey(Triple<Label, String, Label> oldKey, String deltaId) {
        Label parent = oldKey.get1();
        String lr = oldKey.get2();
        Label child = oldKey.get3();
        return new Quadruple<Label, String, Label, String>(parent,lr,child,deltaId);
    }

}
