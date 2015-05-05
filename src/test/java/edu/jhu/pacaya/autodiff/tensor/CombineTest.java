package edu.jhu.pacaya.autodiff.tensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorIdentity;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class CombineTest {

    private Algebra s = RealAlgebra.REAL_ALGEBRA;

    @Test
    public void testForwardAndBackward() {

        Tensor t1 = TensorUtils.getVectorFromValues(s, 1, 2, 3);
        Tensor t2 = TensorUtils.getVectorFromValues(s, 4, 5, 6);
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
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new Combine(m1, m2);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
