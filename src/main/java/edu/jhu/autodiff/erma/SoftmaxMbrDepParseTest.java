package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang.mutable.MutableDouble;
import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.prim.util.Lambda.FnIntDoubleToVoid;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class SoftmaxMbrDepParseTest {

    Algebra s = new RealAlgebra();
    
    String expoutAdj = "Factor [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  30.8000\n"
            + "    0    1  |  33.0000\n"
            + "    1    0  |  165.000\n"
            + "    1    1  |  169.400\n"
            + "]";
    
    @Test
    public void testForwardAndBackward() {
        double T = 1;
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(ModuleTestUtils.getVector(s, 2, 3, 5, 7));
        t1.log();
        t1.multiply(T);
        TensorIdentity id1 = new TensorIdentity(t1);
        SoftmaxMbrDepParse ea = new SoftmaxMbrDepParse(id1, T, s);

        Tensor out = ea.forward();
        System.out.println(out);
        // Forward yields the same result as the test in HyperalgoModuleTest.
        assertEquals(InsideOutsideDepParseTest.expout, out.toString());
        assertEquals(6, out.getValue(0), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        Tensor outAdj = id1.getOutputAdj();
        System.out.println(outAdj);
        // Backward yields a different result from the test in HyperalgoModuleTest because of the
        // exp(x/T).
        assertEquals(expoutAdj, outAdj.toString());              
    }
    
    @Test
    public void testGradByFiniteDiffs() {       
        Tensor t1 = new Tensor(s, 4,4);
        TensorIdentity id1 = new TensorIdentity(t1);
        int T = 2;
        SoftmaxMbrDepParse ea = new SoftmaxMbrDepParse(id1, T, s);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1), ea);
        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        final MutableDouble sum = new MutableDouble(0);
        x.iterate(new FnIntDoubleToVoid() {
            public void call(int idx, double val) {
                sum.add(val);
            }
        });
        x.scale(-1.0/sum.doubleValue());
        ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-8, 1e-5);
    }
    
}
