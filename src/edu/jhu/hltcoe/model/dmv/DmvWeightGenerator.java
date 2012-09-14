/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.dmv.DmvModel.ChooseRhs;
import edu.jhu.hltcoe.model.dmv.DmvModel.StopRhs;

public interface DmvWeightGenerator {
    
    double getStopWeight(StopRhs triple);

    LabeledMultinomial<Label> getChooseMulti(ChooseRhs pair, List<Label> children);
    
}