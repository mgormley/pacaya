package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.util.collections.Lists;

public class CombineTest {

    @Test
    public void testForwardAndBackward() {

        Tensor t1 = ModuleTestUtils.getVector(1, 2, 3);
        Tensor t2 = ModuleTestUtils.getVector(4, 5, 6);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        Combine ea = new Combine(id1, id2);
        
        Tensor out = ea.forward();
        for (int i=0; i<2; i++) {
            for (int k=0; k<3; k++) {
                assertEquals(i*3 + k*1 + 1, out.get(i,k), 1e-13);            
            }
        }
        assertTrue(out == ea.getOutput());

        // Set the adjoint.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                if (i==0) {
                    assertEquals(2.2, id1.getOutputAdj().get(j), 1e-13);
                } else {
                    assertEquals(2.2, id2.getOutputAdj().get(j), 1e-13);
                }
            }
        }
    }

    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(4, 6, 7);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        Combine ea = new Combine(id1, id2);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1, id2), ea);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
