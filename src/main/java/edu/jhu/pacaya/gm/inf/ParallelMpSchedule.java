package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.pacaya.gm.model.FactorGraph;

public class ParallelMpSchedule implements MpSchedule {

    private ArrayList<Object> order;
    
    public ParallelMpSchedule(FactorGraph fg) {        
        order = new ArrayList<Object>();
        // Add the entire list of edges/factors as an item.
        order.add((new RandomMpSchedule(fg)).getOrder());
    }
    
    @Override
    public List<Object> getOrder() {
        return order;
    }

}
