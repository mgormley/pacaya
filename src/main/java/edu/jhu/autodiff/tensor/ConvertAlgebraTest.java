package edu.jhu.autodiff.tensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TopoOrder;
import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.LogSemiring;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ConvertAlgebraTest {

    private Algebra inS = new RealAlgebra();
    private Algebra outS = new RealAlgebra();
    public static List<Algebra> algebras3 = Lists.getList(new RealAlgebra(), new LogSemiring(), new LogSignAlgebra());
    public static List<Algebra> algebras2 = Lists.getList(new RealAlgebra(), new LogSignAlgebra());

    @Test
    public void testForwardAndBackward() {
        for (Algebra inS : algebras3) {
            for (Algebra outS : algebras3) {
                Tensor t1 = ModuleTestUtils.getVectorFromReals(inS, 2, 3, 5);
                TensorIdentity id1 = new TensorIdentity(t1);
                ConvertAlgebra ea = new ConvertAlgebra(id1, outS);

                Tensor out = ea.forward();
                assertEquals(2, outS.toReal(out.getValue(0)), 1e-13);
                assertEquals(3, outS.toReal(out.getValue(1)), 1e-13);
                assertEquals(5, outS.toReal(out.getValue(2)), 1e-13);
                assertTrue(out == ea.getOutput());

                // Set the adjoint of the sum to be 1.
                ea.getOutputAdj().fill(outS.fromReal(2.2));
                ea.backward();

                Tensor inAdj = id1.getOutputAdj();
                assertEquals(2.2, inS.toReal(inAdj.getValue(0)), 1e-13);
                assertEquals(2.2, inS.toReal(inAdj.getValue(1)), 1e-13);
                assertEquals(2.2, inS.toReal(inAdj.getValue(2)), 1e-13);
            }
        }
    }

    @Test
    public void testGradByFiniteDiffs() {
        for (Algebra inS : algebras2) {
            for (Algebra outS : algebras2) {
                Tensor t1 = ModuleTestUtils.getVector(inS, inS.fromReal(2), inS.fromReal(3), inS.fromReal(5));
                TopoOrder topo = new TopoOrder();
                TensorIdentity id1 = new TensorIdentity(t1);
                topo.add(id1);
                ConvertAlgebra ea = new ConvertAlgebra(id1, outS);
                topo.add(ea);
                ConvertAlgebra ea2 = new ConvertAlgebra(ea, inS);
                topo.add(ea2);
                
                TensorVecFn vecFn = new TensorVecFn((List) Lists.getList(id1), topo);
                IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(vecFn.getNumDimensions());
                ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-5, 1e-8);
            }
        }
    }

}
