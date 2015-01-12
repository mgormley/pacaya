package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ExpectedRecallTest {

    private VarConfig goldConfig;
    private BeliefsIdentity id1;
    private Algebra s = new RealAlgebra();

    @Before
    public void setUp() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        t0.setId(0);
        t1.setId(1);
        
        goldConfig = new VarConfig();
        goldConfig.put(t1, 1);

        Beliefs b = new Beliefs(s);
        b.varBeliefs = new VarTensor[2];
        b.facBeliefs = new VarTensor[0];
        b.varBeliefs[0] = new VarTensor(s, new VarSet(t0), 0.5);
        b.varBeliefs[1] = new VarTensor(s, new VarSet(t1), 0.5);
        
        id1 = new BeliefsIdentity(b);
    }
    
    @Test
    public void testSimple() {
        ExpectedRecall s = new ExpectedRecall(id1, goldConfig);
        
        Tensor out = s.forward();
        assertEquals(-0.5, out.getValue(0), 1e-13);
        
        s.getOutputAdj().setValue(0, 1);
        s.backward();
        Beliefs inAdj = id1.getOutputAdj();
        assertEquals(0, inAdj.varBeliefs[0].getValue(0), 1e-13);
        assertEquals(0, inAdj.varBeliefs[0].getValue(1), 1e-13);
        assertEquals(0, inAdj.varBeliefs[1].getValue(0), 1e-13);
        assertEquals(-1, inAdj.varBeliefs[1].getValue(1), 1e-13);        
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        OneToOneFactory<Beliefs,Tensor> fact = new OneToOneFactory<Beliefs,Tensor>() {
            public Module<Tensor> getModule(Module<Beliefs> m1) {
                return new ExpectedRecall(m1, goldConfig);
            }
        };        
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact, id1);
    }
    
}
