package edu.jhu.pacaya.autodiff.tensor;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class ElemLinearTest {

    private Algebra s = RealAlgebra.getInstance();

    @Test
    public void testSimple() {
        Tensor t1 = TensorUtils.getVectorFromValues(s, 2, 3, 5);
        Tensor t2 = TensorUtils.getVectorFromValues(s, 4, 6, 7);
        
        Tensor expOut = TensorUtils.getVectorFromValues(s, 
                2*.3 + 4*.7, 
                3*.3 + 6*.7, 
                5*.3 + 7*.7);
        Tensor expT1Adj = TensorUtils.getVectorFromValues(s, 2.2*.3, 2.2*.3, 2.2*.3);
        Tensor expT2Adj = TensorUtils.getVectorFromValues(s, 2.2*.7, 2.2*.7, 2.2*.7);
        
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ElemLinear(m1, m2, .3, .7);
            }
        };
        
        AbstractModuleTest.evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, 2.2);
    }

    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ElemLinear(m1, m2, 3, 7);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
