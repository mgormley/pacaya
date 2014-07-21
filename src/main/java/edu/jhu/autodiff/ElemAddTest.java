package edu.jhu.autodiff;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;

public class ElemAddTest {

    private Algebra s = Algebras.REAL_ALGEBRA;
    
    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(s, 4, 6, 7);
        
        Tensor expOut = ModuleTestUtils.getVector(s, 
                2 + 4, 
                3 + 6, 
                5 + 7);
        double adjFill = 2.2;
        Tensor expT1Adj = ModuleTestUtils.getVector(s, adjFill, adjFill, adjFill);
        Tensor expT2Adj = ModuleTestUtils.getVector(s, adjFill, adjFill, adjFill);
        
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ElemAdd(m1, m2);
            }
        };
        
        AbstractModuleTest.evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ElemAdd(m1, m2);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
