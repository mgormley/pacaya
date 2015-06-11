package edu.jhu.pacaya.autodiff.tensor;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class ScalarAddTest {

    private Algebra s = RealAlgebra.SINGLETON;

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = TensorUtils.getVectorFromValues(s, 2, 3, 5);
        Tensor t2 = TensorUtils.getVectorFromValues(s, 4, 6, 7);
        
        Tensor expOut = TensorUtils.getVectorFromValues(s, 
                2 + 6, 
                3 + 6, 
                5 + 6);
        double adjFill = 2.2;
        Tensor expT1Adj = TensorUtils.getVectorFromValues(s, 2.2, 2.2, 2.2);
        Tensor expT2Adj = TensorUtils.getVectorFromValues(s, 0, 2.2*3, 0);
        
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ScalarAdd(m1, m2, 1);
            }
        };
        
        AbstractModuleTest.evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ScalarAdd(m1, m2, 1);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
