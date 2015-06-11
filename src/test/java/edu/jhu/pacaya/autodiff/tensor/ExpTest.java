package edu.jhu.pacaya.autodiff.tensor;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.util.math.FastMath;

public class ExpTest {

    private Algebra s = RealAlgebra.SINGLETON;

    @Test
    public void testSimple() {
        Tensor t1 = TensorUtils.getVectorFromValues(s, 2, 3, 5);
        
        Tensor expOut = TensorUtils.getVectorFromValues(s, FastMath.exp(2.), FastMath.exp(3.), FastMath.exp(5.));
        Tensor expT1Adj = TensorUtils.getVectorFromValues(s, 
                2.2 * FastMath.exp(2.),
                2.2 * FastMath.exp(3.),
                2.2 * FastMath.exp(5.));
        
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new Exp(m1);
            }
        };
        
        AbstractModuleTest.evalTensor1(t1, expT1Adj, fact, expOut, 2.2);
    }

    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new Exp(m1);
            }
        };        
        AbstractModuleTest.evalTensor1ByFiniteDiffs(fact);
    }
    
}
