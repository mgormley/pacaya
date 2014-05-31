package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.util.collections.Lists;

public class SelectTest {

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.get3DTensor(2, 3, 5);
                
        TensorIdentity id1 = new TensorIdentity(t1);
        Select ea = new Select(id1, 1, 2);
        
        Tensor out = ea.forward();
        for (int i=0; i<2; i++) {
            int j = 2; // selected idx
            for (int k=0; k<5; k++) {
                assertEquals(i*5*3 + j*5 + k*1, out.get(i,k), 1e-13);            
            }
        }
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        for (int i=0; i<2; i++) {
            for (int j=0; j<3; j++) {
                for (int k=0; k<5; k++) {
                    if (j==2) {
                        assertEquals(2.2, id1.getOutputAdj().get(i,j,k), 1e-13);
                    } else {
                        assertEquals(0.0, id1.getOutputAdj().get(i,j,k), 1e-13);
                    }
                }
            }
        }
    }

    @Test
    public void testGradByFiniteDiffs() {
        TensorIdentity id1 = new TensorIdentity(ModuleTestUtils.get3DTensor(2, 3, 5));
        Select ea = new Select(id1, 1, 2);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1), ea);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);
    }
    
}
