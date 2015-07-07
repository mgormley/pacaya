package edu.jhu.pacaya.autodiff.tensor;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class SumTest {

    private Algebra s = RealAlgebra.getInstance();
    
    @Test
    public void testForwardAndBackward() {
        Tensor t1 = TensorUtils.getVectorFromValues(s, 2, 3, 5);
        
        Tensor expOut = TensorUtils.getVectorFromValues(s, 2.+3.+5.);
        double adjFill = 2.2;
        Tensor expT1Adj = TensorUtils.getVectorFromValues(s, 2.2, 2.2, 2.2);
        
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
