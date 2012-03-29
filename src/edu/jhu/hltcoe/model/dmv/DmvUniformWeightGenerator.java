/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import java.util.Arrays;
import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class DmvUniformWeightGenerator implements DmvWeightGenerator {

    public DmvUniformWeightGenerator() {
    }
    
    @Override
    public double getStopWeight(Triple<Label, String, Boolean> triple) {
        return 0.5;
    }
    
    @Override
    public LabeledMultinomial<Label> getChooseMulti(Pair<Label, String> pair, List<Label> children) {
        double[] chooseMulti = new double[children.size()];
        Arrays.fill(chooseMulti, 1.0 / chooseMulti.length);
        return new LabeledMultinomial<Label>(children, chooseMulti);
    }
    
}