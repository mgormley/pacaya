package edu.jhu.gm.inf;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.gm.model.ExpFamFactor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.IFgModel;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FactorGraph.FgNode;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.globalfac.AbstractGlobalFactor;
import edu.jhu.gm.model.globalfac.GlobalFactor;
import edu.jhu.gm.model.VarSet;

public class BfsMpScheduleTest {

    private String expectedOrderStr1 = 
            "FgEdge [id=4, Factor[t2] --> Var[t2]]\n"
            + "FgEdge [id=13, Var[t2] --> Factor[t1,t2]]\n"
            + "FgEdge [id=10, Factor[t1,t2] --> Var[t1]]\n"
            + "FgEdge [id=2, Factor[t1] --> Var[t1]]\n"
            + "FgEdge [id=9, Var[t1] --> Factor[t0,t1]]\n"
            + "FgEdge [id=6, Factor[t0,t1] --> Var[t0]]\n"
            + "FgEdge [id=1, Var[t0] --> Factor[t0]]\n"
            + "FgEdge [id=0, Factor[t0] --> Var[t0]]\n"
            + "FgEdge [id=7, Var[t0] --> Factor[t0,t1]]\n"
            + "FgEdge [id=8, Factor[t0,t1] --> Var[t1]]\n"
            + "FgEdge [id=3, Var[t1] --> Factor[t1]]\n"
            + "FgEdge [id=11, Var[t1] --> Factor[t1,t2]]\n"
            + "FgEdge [id=12, Factor[t1,t2] --> Var[t2]]\n"
            + "FgEdge [id=5, Var[t2] --> Factor[t2]]\n";
    
    @Test
    public void testGetOrder1() {
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
        
        // Check that we get the expected order.
        BfsMpSchedule schedule = new BfsMpSchedule(fg); 
        List<Object> order = schedule.getOrder();
        String orderStr = getOrderStr(order);        
        assertEquals(order.size(), fg.getNumEdges());
        assertEquals(expectedOrderStr1 , orderStr.toString());
    }

    private String getOrderStr(List<Object> order) {
        StringBuilder orderStr = new StringBuilder();
        for (Object item : order) {
            orderStr.append(item.toString());
            orderStr.append("\n");
        }
        System.out.println(orderStr);
        return orderStr.toString();
    }

    String expectedOrderStr2 = 
            "FgEdge [id=1, Var[t0] --> Factor[t0]]\n"
            + "FgEdge [id=0, Factor[t0] --> Var[t0]]\n"
            + "FgEdge [id=3, Var[t1] --> Factor[t1]]\n"
            + "FgEdge [id=2, Factor[t1] --> Var[t1]]\n";
            
    // This tests that we can properly get an order with two connected components.
    @Test
    public void testGetOrder2() {        
        FactorGraph fg = new FactorGraph();                

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);

        // Emission factors. 
        ExplicitFactor emit0 = new ExplicitFactor(new VarSet(t0)); 
        ExplicitFactor emit1 = new ExplicitFactor(new VarSet(t1)); 
        
        // For this test, we add the transition factor first since we want the root to have multiple children.
        fg.addFactor(emit0);
        fg.addFactor(emit1);
                
        // Check that we get the expected order.
        BfsMpSchedule schedule = new BfsMpSchedule(fg);
        List<Object> order = schedule.getOrder();
        String orderStr = getOrderStr(order);
        assertEquals(order.size(), fg.getNumEdges());
        assertEquals(expectedOrderStr2 , orderStr.toString());
    }
    
    String expectedOrderStr3 = 
            "FgEdge [id=7, Var[t2] --> Factor[t1,t2]]\n"
            + "FgEdge [id=11, Var[t2] --> Factor[t0,t2]]\n"
            + "FgEdge [id=4, Factor[t1,t2] --> Var[t1]]\n"
            + "FgEdge [id=8, Factor[t0,t2] --> Var[t0]]\n"
            + "FgEdge [id=3, Var[t1] --> Factor[t0,t1]]\n"
            + "FgEdge [id=1, Var[t0] --> Factor[t0,t1]]\n"
            + "FgEdge [id=0, Factor[t0,t1] --> Var[t0]]\n"
            + "FgEdge [id=2, Factor[t0,t1] --> Var[t1]]\n"
            + "FgEdge [id=9, Var[t0] --> Factor[t0,t2]]\n"
            + "FgEdge [id=5, Var[t1] --> Factor[t1,t2]]\n"
            + "FgEdge [id=10, Factor[t0,t2] --> Var[t2]]\n"
            + "FgEdge [id=6, Factor[t1,t2] --> Var[t2]]\n";

    // Test a loopy connected component.
    @Test
    public void testGetOrder3() {
        FactorGraph fg = new FactorGraph();

        // Create three tags.
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", null);
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", null);
        Var t2 = new Var(VarType.PREDICTED, 2, "t2", null);

        // Transition factors.
        ExplicitFactor tran0 = new ExplicitFactor(new VarSet(t0, t1));
        ExplicitFactor tran1 = new ExplicitFactor(new VarSet(t1, t2));
        ExplicitFactor tran2 = new ExplicitFactor(new VarSet(t2, t0));

        fg.addFactor(tran0);
        fg.addFactor(tran1);
        fg.addFactor(tran2);

        // Check that we get the expected order.
        BfsMpSchedule schedule = new BfsMpSchedule(fg);
        List<Object> order = schedule.getOrder();
        String orderStr = getOrderStr(order);
        assertEquals(order.size(), fg.getNumEdges());
        assertEquals(expectedOrderStr3, orderStr.toString());
    }
    
    String expectedOrderStr4 = 
            "FgEdge [id=5, Var[t2] --> Factor[t0,t1,t2]]\n"
            + "FgEdge [id=3, Var[t1] --> Factor[t0,t1,t2]]\n"
            + "FgEdge [id=1, Var[t0] --> Factor[t0,t1,t2]]\n"
            + "MockGlobalFactor\n";
    
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
        String orderStr = getOrderStr(order);
        assertTrue(order.size() < fg.getNumEdges());
        assertEquals(expectedOrderStr4, orderStr.toString());
    }
    
    private static class MockGlobalFactor extends AbstractGlobalFactor implements GlobalFactor {

        private static final long serialVersionUID = 1L;

        private VarSet vars;
        
        public MockGlobalFactor(VarSet vars) {
            this.vars = vars;
        }
        
        @Override
        public String toString() {
            return "MockGlobalFactor";
        }

        @Override
        public VarSet getVars() {            
            return vars;
        }

        @Override
        public Factor getClamped(VarConfig clmpVarConfig) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public double getLogUnormalizedScore(VarConfig goldConfig) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public double getLogUnormalizedScore(int goldConfig) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void createMessages(VarTensor[] inMsgs, VarTensor[] outMsgs) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public double getExpectedLogBelief(VarTensor[] inMsgs) {
            throw new RuntimeException("not implemented");
        }

        @Override
        public void backwardCreateMessages(VarTensor[] inMsgs, VarTensor[] outMsgsAdj, VarTensor[] inMsgsAdj) {
            throw new RuntimeException("not implemented");
        }

    }

}
