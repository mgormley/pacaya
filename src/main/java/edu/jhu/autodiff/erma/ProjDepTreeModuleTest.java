package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ProjDepTreeModuleTest {

    Algebra s = new RealAlgebra();
    String expout = "Tensor (RealAlgebra) [\n"
            + "    0    1    2  |  value\n"
            + "    0    0    0  |  0.0960000\n"
            + "    0    0    1  |  0.0960000\n"
            + "    0    1    0  |  0.0960000\n"
            + "    0    1    1  |  0.0960000\n"
            + "    1    0    0  |  0.144000\n"
            + "    1    0    1  |  0.144000\n"
            + "    1    1    0  |  0.144000\n"
            + "    1    1    1  |  0.144000\n"
            + "]";
    
    String expoutAdj1 = "Tensor (RealAlgebra) [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  1.40800\n"
            + "    0    1  |  1.40800\n"
            + "    1    0  |  1.40800\n"
            + "    1    1  |  1.40800\n"
            + "]";
    
    String expoutAdj2 = "Tensor (RealAlgebra) [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  1.84800\n"
            + "    0    1  |  1.84800\n"
            + "    1    0  |  1.84800\n"
            + "    1    1  |  1.84800\n"
            + "]";    
    
    @Test
    public void testSimpleReal() {
        helpSimple(new RealAlgebra());
    }
    
    @Test
    public void testSimpleLogPosNeg() {
        helpSimple(new LogSignAlgebra());
    }

    private void helpSimple(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 2,2);
        Tensor t2 = new Tensor(s, 2,2);
        t1.fill(0.6);
        t2.fill(0.4);
        
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        ProjDepTreeModule topo = new ProjDepTreeModule(id1, id2, tmpS);
        
        Tensor out = topo.forward();
        assertEquals(expout , out.toString());
        
        topo.getOutputAdj().fill(2.2);        
        topo.backward();
        assertEquals(expoutAdj1 , id1.getOutputAdj().toString());
        assertEquals(expoutAdj2 , id2.getOutputAdj().toString());
    }
    
    @Test
    public void testGradByFiniteDiffsReal() {
        helpGradByFinDiff(new RealAlgebra());
    }
    
    @Test
    public void testGradByFiniteDiffsLogPosNeg() {
        helpGradByFinDiff(new LogSignAlgebra());
    }

    private void helpGradByFinDiff(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 3,3);
        TensorIdentity id1 = new TensorIdentity(t1);
        Tensor t2 = new Tensor(s, 3,3);
        TensorIdentity id2 = new TensorIdentity(t2);
        ProjDepTreeModule topo = new ProjDepTreeModule(id1, id2, tmpS);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1, id2), topo);
        int numParams = vecFn.getNumDimensions();
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-8, 1e-5);
    }

    @Test
    public void testGradWithZeroAdjointsInAndPruning() {
        for (Double adjVal : Lists.getList(0., 1.)) {
            for (double[] inVals : Lists.getList(new double[]{.5, .5}, new double[]{0, 1})) {
                System.out.println("inVals: " + Arrays.toString(inVals) + " adjVal: " + adjVal);
                Tensor tmTrueIn = new Tensor(s, 3,3);
                TensorIdentity id1 = new TensorIdentity(tmTrueIn);
                Tensor tmFalseIn = new Tensor(s, 3,3);
                TensorIdentity id2 = new TensorIdentity(tmFalseIn);
        
                tmTrueIn.fill(0.5);
                tmFalseIn.fill(0.5);
                tmTrueIn.set(inVals[0], 0, 1);
                tmFalseIn.set(inVals[1], 0, 1);
                
                ProjDepTreeModule topo = new ProjDepTreeModule(id1, id2, new LogSignAlgebra());
                
                topo.forward();
                System.out.println(topo.getOutput());
                assertTrue(!topo.getOutput().containsNaN());
                
                topo.getOutputAdj().fill(1.0);
                topo.getOutputAdj().set(adjVal, 0, 0, 1);
                topo.getOutputAdj().set(adjVal, 1, 0, 1);
                topo.backward();  
                System.out.println(id1.getOutputAdj());
                System.out.println(id2.getOutputAdj());
                assertTrue(!id1.getOutputAdj().containsNaN());
                assertTrue(!id2.getOutputAdj().containsNaN());
            }
        }
    }
    
}
