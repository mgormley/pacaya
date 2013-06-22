package edu.jhu.gm;

import org.junit.Test;
import static org.junit.Assert.*;
import edu.jhu.gm.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.Var.VarType;


public class BeliefPropagationTest {

    @Test
    public void testBpOnTree() {
        FactorGraph fg = FactorGraphTest.getSimpleGraph();
        
        BeliefPropagationPrm prm = new BeliefPropagationPrm(fg);
        prm.maxIterations = 10;
        prm.timeoutSeconds = 2;
        //prm.updateOrder
        BeliefPropagation bp = new BeliefPropagation(prm);
        bp.run();
        
        assertEquals(2.69, bp.getLogPartition(), 1e-2);
    }

}
