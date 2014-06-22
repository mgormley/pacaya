package edu.jhu.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;

public class RandomBpSchedule implements BpSchedule {

    private ArrayList<FgEdge> order;
    
    public RandomBpSchedule(FactorGraph fg) {
        order = new ArrayList<FgEdge>(fg.getEdges());
    }
    @Override
    public List<FgEdge> getOrder() {
        Collections.shuffle(order);
        return order;
    }

}
