package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class SumTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testForward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Sum sum = new Sum(id1);
        assertEquals(2.+3.+5., sum.forward().getValue(0), 1e-13);
        assertEquals(2.+3.+5., sum.getOutput().getValue(0), 1e-13);
        // Set the adjoint of the sum to be 1.
        sum.getOutputAdj().add(2.2);
        sum.backward();
        assertEquals(2.2, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2, id1.getOutputAdj().getValue(2), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new Sum(m1);
            }
        };        
        AbstractModuleTest.evalTensor1ByFiniteDiffs(fact);
    }
    
}
