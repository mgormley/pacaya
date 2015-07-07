package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor1Factory;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.TensorUtils;
import edu.jhu.pacaya.autodiff.TopoOrder;
import edu.jhu.pacaya.autodiff.tensor.ConvertAlgebra;
import edu.jhu.pacaya.parse.dep.EdgeScores;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

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
    
    Algebra s = RealAlgebra.getInstance();

    @Test
    public void testSimpleReal() {
        helpForwardBackward(RealAlgebra.getInstance());        
    }

    @Test
    public void testSimpleLog() {
        helpForwardBackward(LogSemiring.getInstance());
    }
    
    @Test
    public void testSimpleLogPosNeg() {
        helpForwardBackward(LogSignAlgebra.getInstance());        
    }

    private void helpForwardBackward(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(TensorUtils.getVectorFromValues(s, 2, 3, 5, 7));
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
        ConvertAlgebra<Tensor> idCo = new ConvertAlgebra<Tensor>(id1, tmpS);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(idCo);
        ConvertAlgebra<Tensor> eaCo = new ConvertAlgebra<Tensor>(ea, s);

        TopoOrder<Tensor> topo = new TopoOrder<Tensor>(QLists.getList(id1), eaCo);
        
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
    public void testCountTreesUsingPartition() {
        Tensor t1 = new Tensor(s, 4,4);
        t1.fill(1.0);
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(id1);
        Tensor y = ea.forward();        
        assertEquals(30.0, y.get(InsideOutsideDepParse.ROOT_IDX, 0, 0), 1e-13);
    }
    
    @Test
    public void testPartitionWithOnlyANonProjectiveTree() {
        EdgeScores es = new EdgeScores(4, 0.0);
        // Allow all edges from WALL
        es.setScore(-1, 0, 1.0);
        es.setScore(-1, 1, 1.0);
        es.setScore(-1, 2, 1.0);
        es.setScore(-1, 3, 1.0);
        // Allow two children of token 1 (this will be the root)
        es.setScore(1, 0, 1.0);
        es.setScore(1, 3, 1.0);
        // Allow non-projective child of token 0
        es.setScore(0, 2, 1.0);
        Tensor t1 = es.toTensor(s);
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
        InsideOutsideDepParse ea = new InsideOutsideDepParse(id1);
        Tensor y = ea.forward();        
        assertEquals(0.0, y.get(InsideOutsideDepParse.ROOT_IDX, 0, 0), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor t1 = new Tensor(s, 4,4);
        Tensor1Factory fact = new Tensor1Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1) {
                return new InsideOutsideDepParse(m1);
            }
        };        
        AbstractModuleTest.evalTensor1ByFiniteDiffs(fact, new Identity<Tensor>(t1));
    }
    
}
