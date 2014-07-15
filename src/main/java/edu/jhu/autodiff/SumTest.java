package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class SumTest {

    private Algebra s = new RealAlgebra();
    
    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        
        Tensor expOut = ModuleTestUtils.getVector(s, 2.+3.+5.);
        double adjFill = 2.2;
        Tensor expT1Adj = ModuleTestUtils.getVector(s, 2.2, 2.2, 2.2);
        
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new Sum(m1);
            }
        };
        
        AbstractModuleTest.evalTensor1(t1, expT1Adj, fact, expOut, adjFill);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new Sum(m1);
            }
        };        
        AbstractModuleTest.evalTensor1ByFiniteDiffs(fact);
    }
    
}
