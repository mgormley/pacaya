package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.autodiff.ScalarAdd;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class InsideOutsideDepParseTest {

    Algebra s = new RealAlgebra();

    public static String expout = "Factor [\n"
            + "    0    1    2  |  value\n"
            + "    0    0    0  |  6.000000\n"
            + "    0    0    1  |  3.000000\n"
            + "    0    1    0  |  5.000000\n"
            + "    0    1    1  |  35.000000\n"
            + "    1    0    0  |  1.000000\n"
            + "    1    0    1  |  2.000000\n"
            + "    1    1    0  |  7.000000\n"
            + "    1    1    1  |  1.000000\n"
            + "    2    0    0  |  41.000000\n"
            + "    2    0    1  |  0.000000\n"
            + "    2    1    0  |  0.000000\n"
            + "    2    1    1  |  0.000000\n"
            + "]";
    
    public static String expoutAdj = "Factor [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  15.400000\n"
            + "    0    1  |  11.000000\n"
            + "    1    0  |  33.000000\n"
            + "    1    1  |  24.200000\n"
            + "]";
    
    @Test
    public void testForwardAndBackward() {
        Tensor t1 = new Tensor(2,2);
        t1.setValuesOnly(ModuleTestUtils.getVector(2, 3, 5, 7));
        TensorIdentity id1 = new TensorIdentity(t1);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(id1, s);

        Tensor out = ea.forward();
        System.out.println(out);
        assertEquals(expout, out.toString());
        assertEquals(6, out.getValue(0), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        Tensor outAdj = id1.getOutputAdj();
        System.out.println(outAdj);
        assertEquals(expoutAdj, outAdj.toString());        
    }
    
    @Test
    public void testGradByFiniteDiffs() {       
        Tensor t1 = new Tensor(4,4);
        TensorIdentity id1 = new TensorIdentity(t1);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(id1, s);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1), ea);
        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-5, 1e-8);
    }

}
