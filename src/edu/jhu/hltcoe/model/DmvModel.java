package edu.jhu.hltcoe.model;

import java.util.HashMap;
import java.util.Map;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.util.Triple;

public class DmvModel implements Model {

    private Map<Triple<Label, String, Boolean>, Double> swMap = new HashMap<Triple<Label, String, Boolean>, Double>();
    private Map<Triple<Label, String, Label>, Double> cwMap = new HashMap<Triple<Label, String, Label>, Double>();

    public Map<Triple<Label, String, Boolean>, Double> getStopWeights() {
        return swMap;
    }

    public Map<Triple<Label, String, Label>, Double> getChooseWeights() {
        return cwMap;
    }

    /**
     * @param triple
     *            A Triple consisting of (parent, lr, adj), where parent is the
     *            label of the current node, lr describes whether the model is
     *            generating to the left or right and is \in {"l", "r"}, and adj
     *            is true iff generating an adjacent node.
     * @param weight
     *            The probability of stopping.
     */
    public void putStopWeight(Triple<Label, String, Boolean> triple, double weight) {
        swMap.put(triple, weight);
    }

    /**
     * @param triple
     *            A Triple consisting of (parent, lr, child). lr describes
     *            whether the model is generating to the left or right and is
     *            \in {"l", "r"}. parent and child are the labels of the nodes.
     * @param d
     *            The probability of generating child to the lr of parent.
     */
    public void putChooseWeight(Triple<Label, String, Label> triple, double d) {
        cwMap.put(triple, d);
    }

}
