package edu.jhu.autodiff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff2.ModuleTestUtils.ModuleVecFn;

public class ProdTest {


    @Test
    public void testForward() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Prod s = new Prod(id1);
        Tensor out = s.forward();
        assertEquals(2.*3.*5., out.getValue(0), 1e-13);
        assertTrue(out == s.getOutput());
        // Set the adjoint of the sum to be 1.
        s.getOutputAdj().add(2.2);
        s.backward();
        assertEquals(2.2*3*5, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2*2*5, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2*2*3, id1.getOutputAdj().getValue(2), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Prod s = new Prod(id1);
        
        ModuleVecFn vecFn = new ModuleVecFn(id1, s);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
