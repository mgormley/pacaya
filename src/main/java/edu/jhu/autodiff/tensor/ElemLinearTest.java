package edu.jhu.autodiff.tensor;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ElemLinearTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testSimple() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(s, 4, 6, 7);
        
        Tensor expOut = ModuleTestUtils.getVector(s, 
                2*.3 + 4*.7, 
                3*.3 + 6*.7, 
                5*.3 + 7*.7);
        Tensor expT1Adj = ModuleTestUtils.getVector(s, 2.2*.3, 2.2*.3, 2.2*.3);
        Tensor expT2Adj = ModuleTestUtils.getVector(s, 2.2*.7, 2.2*.7, 2.2*.7);
        
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
