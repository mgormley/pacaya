package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.autodiff.tensor.ConvertAlgebra;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

public class InsideOutsideDepParseTest {
   
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
    
    Algebra s = new RealAlgebra();

    @Test
    public void testSimpleReal() {
        helpForwardBackward(new RealAlgebra());        
    }

    @Test
    public void testSimpleLog() {
        helpForwardBackward(new LogSemiring());
    }
    
    @Test
    public void testSimpleLogPosNeg() {
        helpForwardBackward(new LogSignAlgebra());        
    }

    private void helpForwardBackward(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(ModuleTestUtils.getVector(s, 2, 3, 5, 7));
        TensorIdentity id1 = new TensorIdentity(t1);
        ConvertAlgebra<Tensor> idCo = new ConvertAlgebra<Tensor>(id1, tmpS);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(idCo);
        ConvertAlgebra<Tensor> eaCo = new ConvertAlgebra<Tensor>(ea, s);

        TopoOrder topo = new TopoOrder();
        topo.add(id1); 
        topo.add(idCo);
        topo.add(ea);
        topo.add(eaCo);
        
        Tensor out = topo.forward();
        System.out.println(out);
        assertEquals(expout, out.toString());
        assertEquals(6, out.getValue(0), 1e-13);
        assertTrue(out == topo.getOutput());

        // Set the adjoint of the sum to be 1.
        topo.getOutputAdj().fill(2.2);
        topo.backward();
        
        Tensor outAdj = id1.getOutputAdj();
        System.out.println(outAdj);
        assertEquals(expoutAdj, outAdj.toString());
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor t1 = new Tensor(s, 4,4);
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new InsideOutsideDepParse(m1);
            }
        };        
        AbstractModuleTest.evalTensor1ByFiniteDiffs(fact, new TensorIdentity(t1));
    }
    
}
