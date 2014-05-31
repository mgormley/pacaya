package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils.TensorVecFn;
import edu.jhu.util.collections.Lists;

public class ElemDivideTest {

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(4, 6, 7);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        ElemDivide ea = new ElemDivide(id1, id2);

        Tensor out = ea.forward();
        assertEquals(2./4, out.getValue(0), 1e-13);
        assertEquals(3./6, out.getValue(1), 1e-13);
        assertEquals(5./7, out.getValue(2), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        assertEquals(2.2/4, id1.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2/6, id1.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2/7, id1.getOutputAdj().getValue(2), 1e-13);
        
        assertEquals(2.2*2/(-4*4), id2.getOutputAdj().getValue(0), 1e-13);
        assertEquals(2.2*3/(-6*6), id2.getOutputAdj().getValue(1), 1e-13);
        assertEquals(2.2*5/(-7*7), id2.getOutputAdj().getValue(2), 1e-13);
    }

    @Test
    public void testDivisionByZeroWithNonzeroAdjoint() {
        double in1 = 1;
        double in2 = 0;
        double outAdj = 2.2;
        double expOut = Double.POSITIVE_INFINITY;
        double expOutAdj1 = Double.POSITIVE_INFINITY;
        double expOutAdj2 = Double.NEGATIVE_INFINITY;        
        testDivide(in1, in2, outAdj, expOut, expOutAdj1, expOutAdj2);
    }
    
    @Test
    public void testDivisionByZeroWithZeroAdjoint() {
        double in1 = 1;
        double in2 = 0;
        double outAdj = 0;
        double expOut = Double.POSITIVE_INFINITY;
        double expOutAdj1 = 0;
        double expOutAdj2 = 0;
        testDivide(in1, in2, outAdj, expOut, expOutAdj1, expOutAdj2);        
    }
    

    @Test
    public void testZeroByZeroWithZeroAdjoint() {
        double in1 = 0;
        double in2 = 0;
        double outAdj = 0;
        double expOut = Double.NaN;
        double expOutAdj1 = 0;
        double expOutAdj2 = 0;
        testDivide(in1, in2, outAdj, expOut, expOutAdj1, expOutAdj2);        
    }

    private void testDivide(double in1, double in2, double outAdj, double expOut, double expOutAdj1, double expOutAdj2) {
        Tensor t1 = ModuleTestUtils.getVector(in1);
        Tensor t2 = ModuleTestUtils.getVector(in2);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        ElemDivide ea = new ElemDivide(id1, id2);

        Tensor out = ea.forward();
        assertEquals(expOut, out.getValue(0), 1e-13);
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(outAdj);
        ea.backward();
        
        assertEquals(expOutAdj1, id1.getOutputAdj().getValue(0), 1e-13);        
        assertEquals(expOutAdj2, id2.getOutputAdj().getValue(0), 1e-13);
    }

    @Test
    public void testGradByFiniteDiffs() {
        Tensor t1 = ModuleTestUtils.getVector(2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(4, 6, 7);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity id2 = new TensorIdentity(t2);
        ElemDivide ea = new ElemDivide(id1, id2);
        
        TensorVecFn vecFn = new TensorVecFn((List)Lists.getList(id1, id2), ea);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-10, 1e-5);
    }
    
}
