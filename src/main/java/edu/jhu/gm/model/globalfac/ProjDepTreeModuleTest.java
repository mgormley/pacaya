package edu.jhu.gm.model.globalfac;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.Combine;
import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ProjDepTreeModuleTest {

    Algebra s = new RealAlgebra();

    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = new Tensor(s, 3,3);
        TensorIdentity id1 = new TensorIdentity(t1);
        Tensor t2 = new Tensor(s, 3,3);
        TensorIdentity id2 = new TensorIdentity(t2);

        TopoOrder topo = new TopoOrder();
        ProjDepTreeModule ea = new ProjDepTreeModule(id1, id2, s);
        Combine comb = new Combine(ea);
        topo.add(ea);
        topo.add(comb);
        
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
                
                TopoOrder topo = new TopoOrder();
                ProjDepTreeModule ea = new ProjDepTreeModule(id1, id2, s);
                Combine comb = new Combine(ea);
                topo.add(ea);
                topo.add(comb);
                
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
