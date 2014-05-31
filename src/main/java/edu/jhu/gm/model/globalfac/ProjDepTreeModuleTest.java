package edu.jhu.gm.model.globalfac;

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

    @Test
    public void testGradByFiniteDiffs() {
        Algebra s = new RealAlgebra();
        Tensor t1 = new Tensor(3,3);
        TensorIdentity id1 = new TensorIdentity(t1);
        Tensor t2 = new Tensor(3,3);
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

}
