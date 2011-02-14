package edu.jhu.hltcoe.model;

import java.util.HashMap;
import java.util.Map;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Triple;

public class DmvModel implements Model {
    
    private Map<Triple<Label, String, Boolean>, Double> swMap = new HashMap<Triple<Label, String, Boolean>, Double>();
    private Map<Pair<Label, Label>, Double> cwMap = new HashMap<Pair<Label, Label>, Double>();
    
    public Map<Triple<Label, String, Boolean>, Double> getStopWeights() {
        return swMap;
    }

    public Map<Pair<Label, Label>, Double> getChooseWeights() {
        return cwMap;
    }

    public void putStopWeight(Triple<Label, String, Boolean> triple, double weight) {
        swMap.put(triple, weight);
    }

    public void putChooseWeight(Pair<Label, Label> pair, double d) {
        cwMap.put(pair, d);
    }

}
