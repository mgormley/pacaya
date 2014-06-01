package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ProdTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testForward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Prod m = new Prod(id1);
        Tensor out = m.forward();
        assertEquals(2.*3.*5., out.getValue(0), 1e-13);
        assertTrue(out == m.getOutput());
        // Set the adjoint of the sum to be 1.
        m.getOutputAdj().add(2.2);
        m.backward();
        assertEquals(2.2*3*5, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2*2*5, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2*2*3, id1.getOutputAdj().getValue(2), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Prod m = new Prod(id1);
        
        TensorVecFn vecFn = new TensorVecFn(id1, m);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
