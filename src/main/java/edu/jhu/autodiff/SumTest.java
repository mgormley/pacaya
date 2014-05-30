package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;

public class SumTest {


    @Test
    public void testForward() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Sum s = new Sum(id1);
        assertEquals(2.+3.+5., s.forward().getValue(0), 1e-13);
        assertEquals(2.+3.+5., s.getOutput().getValue(0), 1e-13);
        // Set the adjoint of the sum to be 1.
        s.getOutputAdj().add(2.2);
        s.backward();
        assertEquals(2.2, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2, id1.getOutputAdj().getValue(2), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Sum s = new Sum(id1);
        
        TensorVecFn vecFn = new TensorVecFn(id1, s);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
