package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.globalfac.LinkVar;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class DepTensorFromBeliefsTest {

    private static Algebra s = RealAlgebra.REAL_ALGEBRA;

    @Test
    public void testGuessNumWords() {
        BeliefsIdentity id1 = getBeliefsModule();
        Beliefs b = id1.getOutput();
        assertEquals(2, DepTensorFromBeliefs.guessNumWords(b));
    }
    
    @Test
    public void testSimple() {
        BeliefsIdentity id1 = getBeliefsModule();
        DepTensorFromBeliefs s = new DepTensorFromBeliefs(id1);
        
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

    public static BeliefsIdentity getBeliefsModule() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        Var t1 = new Var(VarType.PREDICTED, 2, "t1", Lists.getList("N", "V"));
        LinkVar l0 = new LinkVar(VarType.PREDICTED, "l0", -1, 1);
        LinkVar l1 = new LinkVar(VarType.PREDICTED, "l1", 1, 0);
        t0.setId(0);
        t1.setId(1);
        l0.setId(2);
        l1.setId(3);
        
        Beliefs b = new Beliefs(s);
        b.varBeliefs = new VarTensor[4];
        b.facBeliefs = new VarTensor[0];
        b.varBeliefs[0] = new VarTensor(s, new VarSet(t0), 0.5);
        b.varBeliefs[1] = new VarTensor(s, new VarSet(t1), 0.5);
        b.varBeliefs[2] = new VarTensor(s, new VarSet(l0), 0.5);
        b.varBeliefs[3] = new VarTensor(s, new VarSet(l1), 0.5);
        
        BeliefsIdentity id1 = new BeliefsIdentity(b);
        return id1;
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        BeliefsIdentity id1 = DepTensorFromBeliefsTest.getBeliefsModule();

        OneToOneFactory<Beliefs,Tensor> fact = new OneToOneFactory<Beliefs,Tensor>() {
            public Module<Tensor> getModule(Module<Beliefs> m1) {
                return new DepTensorFromBeliefs(m1);
            }
        };        
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact, id1);
    }

}
