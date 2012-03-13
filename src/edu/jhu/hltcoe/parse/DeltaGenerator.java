package edu.jhu.hltcoe.parse;

import java.util.Map;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.util.Quadruple;

public interface DeltaGenerator {

    Map<Quadruple<Label, String, Label, String>, Double> getCWDeltas(DmvModel dmvModel);

}
