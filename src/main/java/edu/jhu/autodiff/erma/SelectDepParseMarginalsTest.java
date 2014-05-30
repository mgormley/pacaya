package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.Tensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
import edu.jhu.util.collections.Lists;

public class SelectDepParseMarginalsTest {

    @Test
    public void testSimple() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        LinkVar l0 = new LinkVar(VarType.PREDICTED, "l0", -1, 1);
        LinkVar l1 = new LinkVar(VarType.PREDICTED, "l1", 1, 0);
        t0.setId(0);
        t1.setId(1);
        l0.setId(2);
        l1.setId(3);
        
        Beliefs b = new Beliefs();
        b.varBeliefs = new VarTensor[4];
        b.facBeliefs = new VarTensor[0];
        b.varBeliefs[0] = new VarTensor(new VarSet(t0), 0.5);
        b.varBeliefs[1] = new VarTensor(new VarSet(t1), 0.5);
        b.varBeliefs[2] = new VarTensor(new VarSet(l0), 0.5);
        b.varBeliefs[3] = new VarTensor(new VarSet(l1), 0.5);
        
        BeliefsIdentity id1 = new BeliefsIdentity(b);
        SelectDepParseMarginals s = new SelectDepParseMarginals(id1, 2);
        
        Tensor out = s.forward();
        assertEquals(0.0, out.get(0,0), 1e-13); // -1, 0
        assertEquals(0.0, out.get(0,1), 1e-13);
        assertEquals(0.5, out.get(1,0), 1e-13);
        assertEquals(0.5, out.get(1,1), 1e-13); // -1, 1
        
        s.getOutputAdj().fill(1.0);
        s.backward();
        assertEquals(0, id1.getOutputAdj().varBeliefs[0].getValue(0), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[0].getValue(1), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[1].getValue(0), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[1].getValue(1), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[2].getValue(0), 1e-13);
        assertEquals(1, id1.getOutputAdj().varBeliefs[2].getValue(1), 1e-13);
        assertEquals(0, id1.getOutputAdj().varBeliefs[3].getValue(0), 1e-13);
        assertEquals(1, id1.getOutputAdj().varBeliefs[3].getValue(1), 1e-13);
    }

}
