package edu.jhu.hltcoe.model.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class DmvSupervisedWeightGenerator implements DmvWeightGenerator {

    private DmvModel model;

    public DmvSupervisedWeightGenerator(DepTreebank trainTreebank) {
        DmvMStep mStep = new DmvMStep(0.0);
        model = (DmvModel)mStep.getModel(trainTreebank);
    }
    
    @Override
    public LabeledMultinomial<Label> getChooseMulti(Pair<Label, String> pair, List<Label> children) {
        return model.getChooseWeights(pair);
    }

    @Override
    public double getStopWeight(Triple<Label, String, Boolean> triple) {
        return model.getStopWeight(triple);
    }

}
