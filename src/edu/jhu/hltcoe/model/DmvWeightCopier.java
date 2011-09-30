/**
 * 
 */
package edu.jhu.hltcoe.model;

import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class DmvWeightCopier implements DmvWeightGenerator {

    private DmvModel dmv;
    
    public DmvWeightCopier(DmvModel dmv) {
        this.dmv = dmv;
    }

    @Override
    public LabeledMultinomial<Label> getChooseMulti(Pair<Label,String> pair, List<Label> children) {
        // Do NOT normalize the multinomial, this is only copying.
        return new LabeledMultinomial<Label>(dmv.getChooseWeights(pair));
    }

    @Override
    public double getStopWeight(Triple<Label, String, Boolean> triple) {
        return dmv.getStopWeights().get(triple);
    }
    
}