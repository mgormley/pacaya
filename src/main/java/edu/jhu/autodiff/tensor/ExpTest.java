package edu.jhu.autodiff.tensor;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorUtils;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ExpTest {

    private Algebra s = new RealAlgebra();

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
