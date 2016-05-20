package edu.jhu.pacaya.gm.inf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.pacaya.gm.inf.BfsMpScheduleTest.MockGlobalFactor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarSet;


public class RandomMpScheduleTest {

    // Test that a global factor is correctly added as a single item in the schedule.
    @Test
    public void testGetOrder4() {
        FactorGraph fg = new FactorGraph();

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);

        // Global factor.
        MockGlobalFactor gf = new MockGlobalFactor(new VarSet(t0, t1, t2));
        fg.addFactor(gf);

        // Check that we get the expected order.
        BfsMpSchedule schedule = new BfsMpSchedule(fg);
        List<Object> order = schedule.getOrder();
        String orderStr = BfsMpScheduleTest.getOrderStr(order, fg);
        System.out.println(orderStr);
        assertEquals(4, order.size());
        assertTrue(order.contains(gf));
        assertTrue(order.contains(fg.getBipgraph().edgeT1(t0.getId(), 0)));
        assertTrue(order.contains(fg.getBipgraph().edgeT1(t1.getId(), 0)));
        assertTrue(order.contains(fg.getBipgraph().edgeT1(t2.getId(), 0)));
    }
    
}
