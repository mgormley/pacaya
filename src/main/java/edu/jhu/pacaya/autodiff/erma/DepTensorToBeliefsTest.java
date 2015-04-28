package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorIdentity;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class DepTensorToBeliefsTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testSimple() {
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(TensorUtils.getVectorFromValues(s, .2, .3, .5, .7));
        TensorIdentity id1 = new TensorIdentity(t1);

        BeliefsIdentity inf = DepTensorFromBeliefsTest.getBeliefsModule();
        DepTensorToBeliefs mod = new DepTensorToBeliefs(id1, inf);
        
        Beliefs out = mod.forward();

        assertEquals(null, out.varBeliefs[0]);
        assertEquals(null, out.varBeliefs[1]);

        assertEquals(.3, out.varBeliefs[2].getValue(0), 1e-13);
        assertEquals(.7, out.varBeliefs[2].getValue(1), 1e-13);
        assertEquals(.5, out.varBeliefs[3].getValue(0), 1e-13);
        assertEquals(.5, out.varBeliefs[3].getValue(1), 1e-13);
        
        Beliefs outAdj = mod.getOutputAdj();
        outAdj.fill(0.0);
        outAdj.varBeliefs[2].set(2.2, 1);
        outAdj.varBeliefs[3].set(2.2, 0);
        mod.backward();
        Tensor inAdj = id1.getOutputAdj();
        assertEquals(0.0, inAdj.get(0,0), 1e-13); // -1, 0
        assertEquals(0.0, inAdj.get(0,1), 1e-13); //  0, 1
        assertEquals(-2.2, inAdj.get(1,0), 1e-13); //  1, 0
        assertEquals(2.2, inAdj.get(1,1), 1e-13); // -1, 1        
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        // Inputs
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(TensorUtils.getVectorFromValues(s, .2, .3, .5, .7));
        TensorIdentity id1 = new TensorIdentity(t1);
        final BeliefsIdentity inf = DepTensorFromBeliefsTest.getBeliefsModule();

        OneToOneFactory<Tensor,Beliefs> fact = new OneToOneFactory<Tensor,Beliefs>() {
            public Module<Beliefs> getModule(Module<Tensor> m1) {
                return new DepTensorToBeliefs(m1, inf);
            }
        };        
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact, id1);
    }

}
