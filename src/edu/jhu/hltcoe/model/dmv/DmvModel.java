package edu.jhu.hltcoe.model.dmv;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.math.LabeledMultinomial;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.util.ComparablePair;
import edu.jhu.hltcoe.util.Triple;

public class DmvModel implements Model {

    public static class StopRhs extends Triple<Label, String, Boolean> {

        public StopRhs(Label label, String lr, Boolean adjacent) {
            super(label, lr, adjacent);
        }
        
    }
    
    public static class ChooseRhs extends ComparablePair<Label, String> {

        public ChooseRhs(Label parent, String lr) {
            super(parent, lr);
        }
        
    }
    
    private Map<StopRhs, Double> swMap = new HashMap<StopRhs, Double>();
    private Map<ChooseRhs, LabeledMultinomial<Label>> cwMap = new HashMap<ChooseRhs, LabeledMultinomial<Label>>();

    public DmvModel() {
        
    }
    
    public Map<StopRhs, Double> getStopWeights() {
        return swMap;
    }

    public double getStopWeight(StopRhs triple) {
        return swMap.get(triple);
    }

    public double getStopWeight(Label label, String leftRight, boolean adjacent) {
        return swMap.get(new StopRhs(label, leftRight, adjacent));
    }

    public Map<ChooseRhs, LabeledMultinomial<Label>> getChooseWeights() {
        return cwMap;
    }

    public LabeledMultinomial<Label> getChooseWeights(Label label, String lr) {
        return getChooseWeights(new ChooseRhs(label, lr));
    }

    public LabeledMultinomial<Label> getChooseWeights(ChooseRhs pair) {
        return cwMap.get(pair);
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
    public void putStopWeight(StopRhs triple, double weight) {
        swMap.put(triple, weight);
    }

    public void putStopWeight(Label label, String leftRight, Boolean adjacent, double weight) {
        swMap.put(new StopRhs(label, leftRight, adjacent), weight);
    }

    /**
     * @param parent
     *            Label of the parent node
     * @param lr
     *            Describes whether the model is generating to the left or right
     *            and is \in {"l", "r"}
     * @param chooseMulti
     */
    public void setChooseWeights(Label parent, String lr, LabeledMultinomial<Label> chooseMulti) {
        cwMap.put(new ChooseRhs(parent, lr), chooseMulti);
    }
    
    public void putChooseWeight(Label parent, String leftRight, Label child, double d) {
        LabeledMultinomial<Label> multi = safeGetMultinomial(parent, leftRight);
        multi.put(child, d);
    }

    private LabeledMultinomial<Label> safeGetMultinomial(Label parent, String leftRight) {
        ChooseRhs pair = new ChooseRhs(parent, leftRight);
        LabeledMultinomial<Label> multinomial = cwMap.get(pair);
        if (multinomial == null) {
            multinomial = new LabeledMultinomial<Label>();
            cwMap.put(pair, multinomial);
        }
        return multinomial;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Stop-weight map:\n");
        sb.append(getMapInSortedOrder(swMap));
        
        sb.append("Choose-weight map:\n");
        // Convert to comparable pairs:
        ArrayList<ChooseRhs> cwList = new ArrayList<ChooseRhs>();
        for (ChooseRhs key : cwMap.keySet()) {
            cwList.add(new ChooseRhs(key.get1(), key.get2()));
        }
        Object[] cwArray = cwList.toArray();
        Arrays.sort(cwArray);
        for(Object cw : cwArray) {
            sb.append(cw);
            sb.append(" = {\n");
            LabeledMultinomial<Label> multi = cwMap.get(cw);
            for (Entry<Label, Double> entry : multi.entrySet()) {
                sb.append(String.format("%12s = %.3g,\n", entry.getKey(), entry.getValue()));
            }
            sb.append("}\n");
        }
        
        return sb.toString();
    }

    private static <X,Y> StringBuilder getMapInSortedOrder(Map<X,Y> map) {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> list = new ArrayList<String>();
        for (Entry<X,Y> entry : map.entrySet()) {
            list.add(entry.getKey() + " = " + entry.getValue());
        }
        Object[] array = list.toArray();
        Arrays.sort(array);
        for(Object sw : array) {
            sb.append(sw);
            sb.append("\n");
        }
        return sb;
    }
    

    public void setAllChooseWeights(double value) {
        for (Entry<ChooseRhs, LabeledMultinomial<Label>> entry : getChooseWeights().entrySet()) {
            for (Entry<Label,Double> subEntry : entry.getValue().entrySet()) {
                subEntry.setValue(value);                
            }
        }
    }

    public void setAllStopWeights(double value) {
        for (Entry<StopRhs, Double> entry : getStopWeights().entrySet()) {
            entry.setValue(value);
        }
    }

}
