package edu.jhu.autodiff;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ScalarMultiplyTest {
    
    private Algebra s = new RealAlgebra();

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(s, 4, 6, 7);
        
        Tensor expOut = ModuleTestUtils.getVector(s, 
                2 * 6, 
                3 * 6, 
                5 * 6);
        double adjFill = 2.2;
        Tensor expT1Adj = ModuleTestUtils.getVector(s, 2.2*6, 2.2*6, 2.2*6);
        Tensor expT2Adj = ModuleTestUtils.getVector(s, 0, 2.2*2 + 2.2*3 + 2.2*5, 0);
        
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ScalarMultiply(m1, m2, 1);
            }
        };
        
        AbstractModuleTest.evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill);
    }

    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ScalarMultiply(m1, m2, 1);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
