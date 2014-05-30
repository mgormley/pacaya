package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.ModuleVecFn;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.collections.Lists;

public class ExpTest {

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Exp ea = new Exp(id1);

        Tensor out = ea.forward();
        assertEquals(FastMath.exp(2.), out.getValue(0), 1e-13);
        assertEquals(FastMath.exp(3.), out.getValue(1), 1e-13);
        assertEquals(FastMath.exp(5.), out.getValue(2), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        assertEquals(2.2 * FastMath.exp(2.), id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2 * FastMath.exp(3.), id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2 * FastMath.exp(5.), id1.getOutputAdj().getValue(2), 1e-13);
    }

    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        Exp ea = new Exp(id1);
        
        ModuleVecFn vecFn = new ModuleVecFn((List)Lists.getList(id1), ea);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
