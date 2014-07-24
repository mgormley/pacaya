package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.autodiff.tensor.Exp;
import edu.jhu.autodiff.tensor.ScalarAdd;
import edu.jhu.autodiff.tensor.Sum;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class TopoOrderTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testSimple() {
        TopoOrder topo = new TopoOrder();
        
        Tensor t1 = TensorUtils.getVectorFromValues(s, 2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        
        Sum s = new Sum(id1);
        topo.add(s);
        
        ScalarAdd add = new ScalarAdd(id1, s, 0);
        topo.add(add);
      
        Tensor t = topo.forward();
        System.out.println(t);
        double sum = 2+3+5;
        assertEquals(2 + sum, t.getValue(0), 1e-13);
        assertEquals(3 + sum, t.getValue(1), 1e-13);
        assertEquals(5 + sum, t.getValue(2), 1e-13);
        
        topo.getOutputAdj().fill(2.2);
        
        topo.backward();
        Tensor xAdj = id1.getOutputAdj();
        System.out.println(xAdj);
        assertEquals(2.2 + 2.2*3, xAdj.getValue(0), 1e-13);
        assertEquals(2.2 + 2.2*3, xAdj.getValue(1), 1e-13);
        assertEquals(2.2 + 2.2*3, xAdj.getValue(2), 1e-13);
    }
    
    @Test
    public void testGradByFiniteDiffs() {
        TopoOrder topo = new TopoOrder();
        
        Tensor t1 = TensorUtils.getVectorFromValues(s, 2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        
        Sum s = new Sum(id1);
        topo.add(s);
        
        ScalarAdd add = new ScalarAdd(id1, s, 0);
        topo.add(add);
        
        Exp exp = new Exp(add);
        topo.add(exp);
        
        TensorVecFn vecFn = new TensorVecFn(id1, topo);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);

        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        x.set(0, .2);
        x.set(1, .3);
        x.set(2, .5);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-5, 1e-8);
    }

}
