package edu.jhu.pacaya.autodiff.tensor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.TopoOrder;
import edu.jhu.pacaya.autodiff.ModuleTestUtils.ModuleFn;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.vector.IntDoubleDenseVector;

public class ConvertAlgebraTest {
    
    public static List<Algebra> algebras3 = QLists.getList(RealAlgebra.getInstance(), LogSemiring.getInstance(), LogSignAlgebra.getInstance());
    public static List<Algebra> algebras2 = QLists.getList(RealAlgebra.getInstance(), LogSignAlgebra.getInstance());

    @Test
    public void testForwardAndBackward() {
        for (Algebra inS : algebras3) {
            for (Algebra outS : algebras3) {
                Tensor t1 = TensorUtils.getVectorFromReals(inS, 2, 3, 5);
                Identity<Tensor> id1 = new Identity<Tensor>(t1);
                ConvertAlgebra<Tensor> ea = new ConvertAlgebra<Tensor>(id1, outS);

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
                Tensor t1 = TensorUtils.getVectorFromValues(inS, inS.fromReal(2), inS.fromReal(3), inS.fromReal(5));
                Identity<Tensor> id1 = new Identity<Tensor>(t1);
                ConvertAlgebra<Tensor> ea = new ConvertAlgebra<Tensor>(id1, outS);
                ConvertAlgebra<Tensor> ea2 = new ConvertAlgebra<Tensor>(ea, inS);
                
                TopoOrder<Tensor> topo = new TopoOrder<Tensor>(QLists.getList(id1), ea2);

                int numParams = ModuleFn.getOutputSize(topo.getInputs());
                IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
                ModuleTestUtils.assertGradientCorrectByFd(topo, x, 1e-5, 1e-8);                
            }
        }
    }

}
