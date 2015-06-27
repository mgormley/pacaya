package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FactorGraph.FgNode;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;

public class RandomMpSchedule implements MpSchedule {

    private ArrayList<Object> order;
    
    public RandomMpSchedule(FactorGraph fg) {
        order = new ArrayList<Object>();
        for (FgNode node : fg.getNodes()) {
            if (node.isFactor() && node.getFactor() instanceof GlobalFactor) {
                order.add(node.getFactor());
            } else {
                order.addAll(node.getOutEdges());
            }
        }
    }
    
    @Override
    public List<Object> getOrder() {
        Collections.shuffle(order);
        return order;
    }

}
