package edu.jhu.gm;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.gm.FactorGraph.FgEdge;
import edu.jhu.gm.Var.VarType;

public class BfsBpScheduleTest {

    @Test
    public void testGetOrder() {
        FactorGraph fg = new FactorGraph();                

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);

        // Emission factors. 
        Factor emit0 = new Factor(new VarSet(t0)); 
        Factor emit1 = new Factor(new VarSet(t1)); 
        Factor emit2 = new Factor(new VarSet(t2)); 
        
        // Transition factors.
        Factor tran0 = new Factor(new VarSet(t0, t1)); 
        Factor tran1 = new Factor(new VarSet(t1, t2)); 
        
        fg.addFactor(emit0);
        fg.addFactor(emit1);
        fg.addFactor(emit2);
        fg.addFactor(tran0);
        fg.addFactor(tran1);
        
        BfsBpSchedule schedule = new BfsBpSchedule(fg);
        
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        
        System.out.println(schedule.getOrder());
        
        // This is valid test, but not an ideal test setup that we depend on toString().
        Assert.assertEquals(
                "[FgEdge [id=2, marked=true, Factor[t1] --> Var[t1]], FgEdge [id=4, marked=true, Factor[t2] --> Var[t2]], FgEdge [id=13, marked=true, Var[t2] --> Factor[t1,t2]], FgEdge [id=10, marked=true, Factor[t1,t2] --> Var[t1]], FgEdge [id=9, marked=true, Var[t1] --> Factor[t0,t1]], FgEdge [id=6, marked=true, Factor[t0,t1] --> Var[t0]], FgEdge [id=1, marked=true, Var[t0] --> Factor[t0]], FgEdge [id=0, marked=true, Factor[t0] --> Var[t0]], FgEdge [id=7, marked=true, Var[t0] --> Factor[t0,t1]], FgEdge [id=8, marked=true, Factor[t0,t1] --> Var[t1]], FgEdge [id=3, marked=true, Var[t1] --> Factor[t1]], FgEdge [id=11, marked=true, Var[t1] --> Factor[t1,t2]], FgEdge [id=12, marked=true, Factor[t1,t2] --> Var[t2]], FgEdge [id=5, marked=true, Var[t2] --> Factor[t2]]]",
                schedule.getOrder().toString());
    }

}
