package edu.jhu.pacaya.gm.inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.jhu.pacaya.gm.model.Factor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.globalfac.GlobalFactor;
import edu.jhu.pacaya.gm.util.BipartiteGraph;

public class RandomMpSchedule implements MpSchedule {

    private ArrayList<Object> order;
    
    public RandomMpSchedule(FactorGraph fg) {
        BipartiteGraph<Var, Factor> bg = fg.getBipgraph();
        order = new ArrayList<Object>();
        for (int v=0; v<fg.getNumVars(); v++) {
            for (int nb=0; nb<bg.numNbsT1(v); nb++) {
                order.add(bg.edgeT1(v, nb));
            }
        }
        for (int f=0; f<fg.getNumFactors(); f++) {
            if (bg.getT2s().get(f) instanceof GlobalFactor) {
                order.add(bg.getT2s().get(f));
            } else {
                for (int nb=0; nb<bg.numNbsT2(f); nb++) {
                    order.add(bg.edgeT2(f, nb));
                }
            }
        }
    }
    
    @Override
    public List<Object> getOrder() {
        Collections.shuffle(order);
        return order;
    }

}
