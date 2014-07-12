package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ScalarFillTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(s, 4, 6, 7);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        ScalarFill ea = new ScalarFill(id1, id2, 1);

        Tensor out = ea.forward();
        assertEquals(6, out.getValue(0), 1e-13);
        assertEquals(6, out.getValue(1), 1e-13);
        assertEquals(6, out.getValue(2), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        assertEquals(0, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(0, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(0, id1.getOutputAdj().getValue(2), 1e-13);
        
        assertEquals(0, id2.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2*3, id2.getOutputAdj().getValue(1), 1e-13);
        assertEquals(0, id2.getOutputAdj().getValue(2), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ScalarFill(m1, m2, 1);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
