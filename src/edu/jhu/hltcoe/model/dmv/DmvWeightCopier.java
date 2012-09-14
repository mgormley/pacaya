/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.dmv.DmvModel.ChooseRhs;
import edu.jhu.hltcoe.model.dmv.DmvModel.StopRhs;

public class DmvWeightCopier implements DmvWeightGenerator {

    private DmvModel dmv;
    
    public DmvWeightCopier(DmvModel dmv) {
        this.dmv = dmv;
    }

    @Override
    public LabeledMultinomial<Label> getChooseMulti(ChooseRhs pair, List<Label> children) {
        // Do NOT normalize the multinomial, this is only copying.
        return new LabeledMultinomial<Label>(dmv.getChooseWeights(pair));
    }

    @Override
    public double getStopWeight(StopRhs triple) {
        return dmv.getStopWeights().get(triple);
    }
    
}