package edu.jhu.autodiff;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.ModuleVecFn;
import edu.jhu.prim.vector.IntDoubleDenseVector;

public class TopoOrderTest {

    @Test
    public void testSimple() {
        TopoOrder topo = new TopoOrder();
        
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
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
        
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        TensorIdentity id1 = new TensorIdentity(t1);
        
        Sum s = new Sum(id1);
        topo.add(s);
        
        ScalarAdd add = new ScalarAdd(id1, s, 0);
        topo.add(add);
        
        Exp exp = new Exp(add);
        topo.add(exp);
        
        ModuleVecFn vecFn = new ModuleVecFn(id1, topo);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-5, 1e-8);

        int numParams = vecFn.getNumDimensions();                
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        x.set(0, .2);
        x.set(1, .3);
        x.set(2, .5);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, x, 1e-5, 1e-8);
    }

}
