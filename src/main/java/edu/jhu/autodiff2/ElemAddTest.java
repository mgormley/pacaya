package edu.jhu.autodiff2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff2.ModuleTestUtils.ModuleVecFn;
import edu.jhu.util.collections.Lists;

public class ElemAddTest {

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(4, 6, 7);
        Identity id1 = new Identity(t1);
        Identity id2 = new Identity(t2);
        ElemAdd ea = new ElemAdd(id1, id2);

        Tensor out = ea.forward();
        assertEquals(2+4, out.getValue(0), 1e-13);
        assertEquals(3+6, out.getValue(1), 1e-13);
        assertEquals(5+7, out.getValue(2), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        assertEquals(2.2, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2, id1.getOutputAdj().getValue(2), 1e-13);
        
        assertEquals(2.2, id2.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2, id2.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2, id2.getOutputAdj().getValue(2), 1e-13);
    }

    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(4, 6, 7);
        Identity id1 = new Identity(t1);
        Identity id2 = new Identity(t2);
        ElemAdd ea = new ElemAdd(id1, id2);
        
        ModuleVecFn vecFn = new ModuleVecFn((List)Lists.getList(id1, id2), ea);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
