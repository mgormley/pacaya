package edu.jhu.pacaya.autodiff.tensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorIdentity;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class SelectTest {

    private Algebra s = RealAlgebra.REAL_ALGEBRA;

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = TensorUtils.get3DTensor(s, 2, 3, 5);
                
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
    public void testGradByFiniteDiffsAllSemirings() {
        TensorIdentity id1 = new TensorIdentity(TensorUtils.get3DTensor(s, 2, 3, 5));

        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new Select(m1, 1, 2);
            }
        };        
        
        AbstractModuleTest.evalTensor1ByFiniteDiffs(fact, id1);
    }
    
}
