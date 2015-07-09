package edu.jhu.pacaya.gm.model.globalfac;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class TakeLeftIfZeroTest {

    private Algebra s = RealAlgebra.getInstance();
    
    @Test
    public void testForwardAndBackward() {
        Tensor t1 = TensorUtils.getVectorFromValues(s, 1, 2, 3);
        Tensor t2 = TensorUtils.getVectorFromValues(s, 4, 5, 6);
        
        Tensor expOut = TensorUtils.getVectorFromValues(s, 
                1, 
                5, 
                6);
        double adjFill = 2.2;
        Tensor expT1Adj = TensorUtils.getVectorFromValues(s, adjFill, 0.0, 0.0);
        Tensor expT2Adj = TensorUtils.getVectorFromValues(s, 0.0, adjFill, adjFill);
        
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                final Tensor t3 = TensorUtils.getVectorFromReals(m1.getAlgebra(), 0, 1, 1e-13);
                return new TakeLeftIfZero(m1, m2, new Identity<Tensor>(t3));
            }
        };
        
        AbstractModuleTest.evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                final Tensor t3 = TensorUtils.getVectorFromReals(m1.getAlgebra(), 0, 1, 1e-13);
                return new TakeLeftIfZero(m1, m2, new Identity<Tensor>(t3));
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
