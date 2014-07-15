package edu.jhu.autodiff;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ElemDivideTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testForwardAndBackward() {
        Tensor t1 = ModuleTestUtils.getVector(s, 2, 3, 5);
        Tensor t2 = ModuleTestUtils.getVector(s, 4, 6, 7);
        
        Tensor expOut = ModuleTestUtils.getVector(s, 
                2./4, 
                3./6, 
                5./7);
        double adjFill = 2.2;
        Tensor expT1Adj = ModuleTestUtils.getVector(s, 2.2/4, 2.2/6, 2.2/7);
        Tensor expT2Adj = ModuleTestUtils.getVector(s, 2.2*2/(-4*4), 2.2*3/(-6*6), 2.2*5/(-7*7));
        
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ElemDivide(m1, m2);
            }
        };
        
        AbstractModuleTest.evalTensor2(t1, expT1Adj, t2, expT2Adj, fact, expOut, adjFill);
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
        Tensor t1 = ModuleTestUtils.getVector(s, in1);
        Tensor t2 = ModuleTestUtils.getVector(s, in2);
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
    public void testGradByFiniteDiffsAllSemirings() {
        Tensor2Factory fact = new Tensor2Factory() {
            public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                return new ElemDivide(m1, m2);
            }
        };        
        AbstractModuleTest.evalTensor2ByFiniteDiffs(fact);
    }
    
}
