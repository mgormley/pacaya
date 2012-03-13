/**
 * 
 */
package edu.jhu.hltcoe.model.dmv;

import java.util.List;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public interface DmvWeightGenerator {
    
    double getStopWeight(Triple<Label, String, Boolean> triple);

    LabeledMultinomial<Label> getChooseMulti(Pair<Label, String> pair, List<Label> children);
    
}