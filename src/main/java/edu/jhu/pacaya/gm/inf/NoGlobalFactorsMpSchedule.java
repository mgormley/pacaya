package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.util.BipartiteGraph;

public class NoGlobalFactorsMpSchedule implements MpSchedule {

    private ArrayList<Object> order;
    
    public NoGlobalFactorsMpSchedule(FactorGraph fg) {
        BipartiteGraph<Var, Factor> bg = fg.getBipgraph();
        order = new ArrayList<Object>(bg.getNumEdges());
        for (int e=0; e<bg.getNumEdges(); e++) {
            order.add(e);
        }
    }
    
    @Override
    public List<Object> getOrder() {
        return order;
    }

}
