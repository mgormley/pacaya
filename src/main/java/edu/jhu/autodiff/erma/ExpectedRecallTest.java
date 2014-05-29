package edu.jhu.autodiff.erma;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.ModuleTestUtils.ModuleVecFn;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.collections.Lists;

public class ExpectedRecallTest {

    @Test
    public void testSimple() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        t0.setId(0);
        t1.setId(1);
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(t1, 1);

        Beliefs b = new Beliefs();
        b.varBeliefs = new VarTensor[2];
        b.facBeliefs = new VarTensor[0];
        b.varBeliefs[0] = new VarTensor(new VarSet(t0), 0.5);
        b.varBeliefs[1] = new VarTensor(new VarSet(t1), 0.5);
        
        BeliefsIdentity id1 = new BeliefsIdentity(b);
        ExpectedRecall s = new ExpectedRecall(id1, goldConfig);
        
        Tensor out = s.forward();
        assertEquals(0.5, out.getValue(0), 1e-13);
        
        s.getOutputAdj().setValue(0, 1);
        s.backward();
        assertEquals(0, id1.getOutputAdj().varBeliefs[0].getValue(0), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[0].getValue(1), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[1].getValue(0), 1e-13);
        assertEquals(1, id1.getOutputAdj().varBeliefs[1].getValue(1), 1e-13);        
    }
    
}
