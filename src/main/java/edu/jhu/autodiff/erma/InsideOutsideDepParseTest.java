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
    
    public static String expout = "Tensor (RealAlgebra) [\n"
            + "    0    1    2  |  value\n"
            + "    0    0    0  |  6.00000\n"
            + "    0    0    1  |  3.00000\n"
            + "    0    1    0  |  5.00000\n"
            + "    0    1    1  |  35.0000\n"
            + "    1    0    0  |  1.00000\n"
            + "    1    0    1  |  2.00000\n"
            + "    1    1    0  |  7.00000\n"
            + "    1    1    1  |  1.00000\n"
            + "    2    0    0  |  41.0000\n"
            + "    2    0    1  |  0.00000\n"
            + "    2    1    0  |  0.00000\n"
            + "    2    1    1  |  0.00000\n"
            + "]";
    
    public static String expoutAdj = "Tensor (RealAlgebra) [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  15.4000\n"
            + "    0    1  |  11.0000\n"
            + "    1    0  |  33.0000\n"
            + "    1    1  |  24.2000\n"
            + "]";
    
    @Test
    public void testForwardAndBackward() {
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(ModuleTestUtils.getVector(s, 2, 3, 5, 7));
        TensorIdentity id1 = new TensorIdentity(t1);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(id1);

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
        Tensor t1 = new Tensor(s, 4,4);
        TensorIdentity id1 = new TensorIdentity(t1);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(id1);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1), ea);
        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-5, 1e-8);
    }

}
