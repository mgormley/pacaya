package edu.jhu.gm.inf;

import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;

public class BfsMpScheduleTest {

    @Test
    public void testGetOrder() {
        FactorGraph fg = new FactorGraph();                

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);

        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1)); 
        ExplicitFactor emit2 = new ExplicitFactor(new VarSet(t2)); 
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1)); 
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2)); 
        
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        BfsMpSchedule schedule = new BfsMpSchedule(fg);
        
        System.out.println();
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        
        System.out.println(schedule.getOrder());
        
        // This is valid test, but not an ideal test setup that we depend on toString().
        Assert.assertEquals(
                "[FgEdge [id=4, Factor[t2] --> Var[t2]], FgEdge [id=13, Var[t2] --> Factor[t1,t2]], FgEdge [id=10, Factor[t1,t2] --> Var[t1]], FgEdge [id=2, Factor[t1] --> Var[t1]], FgEdge [id=9, Var[t1] --> Factor[t0,t1]], FgEdge [id=6, Factor[t0,t1] --> Var[t0]], FgEdge [id=1, Var[t0] --> Factor[t0]], FgEdge [id=0, Factor[t0] --> Var[t0]], FgEdge [id=7, Var[t0] --> Factor[t0,t1]], FgEdge [id=8, Factor[t0,t1] --> Var[t1]], FgEdge [id=3, Var[t1] --> Factor[t1]], FgEdge [id=11, Var[t1] --> Factor[t1,t2]], FgEdge [id=12, Factor[t1,t2] --> Var[t2]], FgEdge [id=5, Var[t2] --> Factor[t2]]]",
                schedule.getOrder().toString());
    }

    @Test
    public void testGetOrder2() {
        FactorGraph fg = new FactorGraph();                

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);

        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1)); 
        
        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1));
        
        // For this test, we add the transition factor first since we want the root to have multiple children.
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(tran0);
        

        ArrayList<FgEdge> order = new ArrayList<FgEdge>();
        BfsMpSchedule.addEdgesFromRoot(fg.getNode(tran0), order, fg);
        
        System.out.println();
        for (FgEdge edge : order) {
            System.out.println(edge.toString());
        }
        
        System.out.println(order);
        
        // This is valid test, but not an ideal test setup that we depend on toString().
        Assert.assertEquals(
                "[FgEdge [id=2, Factor[t1] --> Var[t1]], FgEdge [id=0, Factor[t0] --> Var[t0]], FgEdge [id=7, Var[t1] --> Factor[t0,t1]], FgEdge [id=5, Var[t0] --> Factor[t0,t1]], FgEdge [id=4, Factor[t0,t1] --> Var[t0]], FgEdge [id=6, Factor[t0,t1] --> Var[t1]], FgEdge [id=1, Var[t0] --> Factor[t0]], FgEdge [id=3, Var[t1] --> Factor[t1]]]",
                order.toString());
    }

}
