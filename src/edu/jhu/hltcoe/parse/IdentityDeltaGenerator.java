package edu.jhu.hltcoe.parse;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModel.ChooseRhs;
import edu.jhu.hltcoe.util.Quadruple;

public class IdentityDeltaGenerator implements DeltaGenerator {

    public static final String IDENTITY_DELTA_ID = "identity";

    @Override
    public Map<Quadruple<Label, String, Label, String>, Double> getCWDeltas(DmvModel dmvModel) {
        Map<Quadruple<Label, String, Label, String>, Double> cwDeltas = new HashMap<Quadruple<Label, String, Label, String>, Double>();
        for (Entry<ChooseRhs, LabeledMultinomial<Label>> entry : dmvModel.getChooseWeights().entrySet()) {
            for (Entry<Label, Double> subEntry : entry.getValue().entrySet()) {
                Quadruple<Label, String, Label, String> key = getDeltaKey(entry.getKey(), subEntry.getKey(),
                        IDENTITY_DELTA_ID);
                double weight = subEntry.getValue();
                cwDeltas.put(key, weight);
            }
        }
        return cwDeltas;
    }

    protected Quadruple<Label, String, Label, String> getDeltaKey(ChooseRhs pair, Label child, String deltaId) {
        Label parent = pair.get1();
        String lr = pair.get2();
        return new Quadruple<Label, String, Label, String>(parent, lr, child, deltaId);
    }

}
