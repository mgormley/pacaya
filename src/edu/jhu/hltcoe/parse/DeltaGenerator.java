package edu.jhu.hltcoe.parse;

import java.util.Map;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Quadruple;
import edu.jhu.hltcoe.util.Triple;

public interface DeltaGenerator {

    Map<Quadruple<Label, String, Label, String>, Double> getCWDeltas(
            Map<Triple<Label, String, Label>, Double> chooseWeights);

}
