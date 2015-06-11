package edu.jhu.pacaya.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.pacaya.autodiff.ModuleTestUtils.ModuleFn;
import edu.jhu.pacaya.util.collections.Lists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSignAlgebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;
import edu.jhu.prim.vector.IntDoubleDenseVector;

public class ProjDepTreeModuleTest {

    Algebra s = RealAlgebra.REAL_ALGEBRA;
    String expout = "Tensor (RealAlgebra) [\n"
            + "    0    1    2  |  value\n"
            + "    0    0    0  |  0.144000\n"
            + "    0    0    1  |  0.144000\n"
            + "    0    1    0  |  0.144000\n"
            + "    0    1    1  |  0.144000\n"
            + "    1    0    0  |  0.0960000\n"
            + "    1    0    1  |  0.0960000\n"
            + "    1    1    0  |  0.0960000\n"
            + "    1    1    1  |  0.0960000\n"
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
        helpSimple(RealAlgebra.REAL_ALGEBRA);
    }
    
    @Test
    public void testSimpleLogPosNeg() {
        helpSimple(LogSignAlgebra.LOG_SIGN_ALGEBRA);
    }

    private void helpSimple(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 2,2);
        Tensor t2 = new Tensor(s, 2,2);
        t1.fill(0.6);
        t2.fill(0.4);
        
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
        Identity<Tensor> id2 = new Identity<Tensor>(t2);
        ProjDepTreeModule topo = new ProjDepTreeModule(id1, id2, tmpS);
        
        Tensor out = topo.forward();
        assertEquals(expout , out.toString());
        
        topo.getOutputAdj().fill(2.2);        
        topo.backward();
        assertEquals(expoutAdj1 , id1.getOutputAdj().toString());
        assertEquals(expoutAdj2 , id2.getOutputAdj().toString());
    }
    
    @Test
    public void testForwardAndBackward() {
        for (final Algebra tmpS : Lists.getList(RealAlgebra.REAL_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA)) {
            Tensor t1 = new Tensor(s, 2, 2);
            t1.set(0.5, 0, 0);
            t1.set(0.5, 0, 1);
            t1.set(0.5, 1, 0);
            t1.set(0.5, 1, 1);
            Tensor t2 = new Tensor(s, 2, 2);
            t2.set(0.5, 0, 0);
            t2.set(0.5, 0, 1);
            t2.set(0.5, 1, 0);
            t2.set(0.5, 1, 1);
            
            Tensor expOut = new Tensor(s, 2, 2, 2);
            expOut.set(0.125, 0, 0, 0);
            expOut.set(0.125, 0, 0, 1);
            expOut.set(0.125, 0, 1, 0);
            expOut.set(0.125, 0, 1, 1);
            expOut.set(0.125, 1, 0, 0);
            expOut.set(0.125, 1, 0, 1);
            expOut.set(0.125, 1, 1, 0);
            expOut.set(0.125, 1, 1, 1);
            
            Tensor adj = new Tensor(s, 2, 2, 2);
            adj.set(0.0, 0, 0, 0);
            adj.set(0.0, 0, 0, 1);
            adj.set(0.0, 0, 1, 0);
            adj.set(0.0, 0, 1, 1);
            adj.set(0.0, 1, 0, 0);
            adj.set(0.0, 1, 0, 1);
            adj.set(1.0, 1, 1, 0);
            adj.set(1.0, 1, 1, 1);
            
            Tensor expT1Adj = new Tensor(s, 2, 2);
            expT1Adj.set(0.0, 0, 0);
            expT1Adj.set(0.0, 0, 1);
            expT1Adj.set(0.25, 1, 0);
            expT1Adj.set(0.25, 1, 1);
            
            // TODO: Why is the false adjoint twice the true adjoint?
            Tensor expT2Adj = new Tensor(s, 2, 2);
            expT2Adj.set(0.5, 0, 0);
            expT2Adj.set(0.5, 0, 1);
            expT2Adj.set(0.0, 1, 0);
            expT2Adj.set(0.0, 1, 1);
            
            Tensor2Factory fact = new Tensor2Factory() {
                public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                    return new ProjDepTreeModule(m1, m2, tmpS);
                }
            };
            
            AbstractModuleTest.evalTensor2OneAlgebra(t1, expT1Adj, t2, expT2Adj, fact, expOut, adj, RealAlgebra.REAL_ALGEBRA);
            AbstractModuleTest.evalTensor2OneAlgebra(t1, expT1Adj, t2, expT2Adj, fact, expOut, adj, LogSignAlgebra.LOG_SIGN_ALGEBRA);
        }
    }
        
    @Test
    public void testGradByFiniteDiffsReal() {
        helpGradByFinDiff(RealAlgebra.REAL_ALGEBRA);
    }
    
    @Test
    public void testGradByFiniteDiffsLogPosNeg() {
        helpGradByFinDiff(LogSignAlgebra.LOG_SIGN_ALGEBRA);
    }

    private void helpGradByFinDiff(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 3,3);
        Identity<Tensor> id1 = new Identity<Tensor>(t1);
        Tensor t2 = new Tensor(s, 3,3);
        Identity<Tensor> id2 = new Identity<Tensor>(t2);
        ProjDepTreeModule topo = new ProjDepTreeModule(id1, id2, tmpS);
        
        // Fill the tensors with 1.0 so that initial forward pass doesn't throw an error on the zeros. 
        t1.fill(s.one());
        t2.fill(s.one());
        
        int numParams = ModuleFn.getOutputSize(topo.getInputs());
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        ModuleTestUtils.assertGradientCorrectByFd(topo, x, 1e-8, 1e-5);
    }

    @Test
    public void testGradWithZeroAdjointsInAndPruning() {
        for (Double adjVal : Lists.getList(0., 1.)) {
            for (double[] inVals : Lists.getList(new double[]{.5, .5}, new double[]{0, 1})) {
                System.out.println("inVals: " + Arrays.toString(inVals) + " adjVal: " + adjVal);
                Tensor tmTrueIn = new Tensor(s, 3,3);
                Identity<Tensor> id1 = new Identity<Tensor>(tmTrueIn);
                Tensor tmFalseIn = new Tensor(s, 3,3);
                Identity<Tensor> id2 = new Identity<Tensor>(tmFalseIn);
        
                tmTrueIn.fill(0.5);
                tmFalseIn.fill(0.5);
                tmTrueIn.set(inVals[0], 0, 1);
                tmFalseIn.set(inVals[1], 0, 1);
                
                ProjDepTreeModule topo = new ProjDepTreeModule(id1, id2, LogSignAlgebra.LOG_SIGN_ALGEBRA);
                
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
    
    // NOTE: This test seems to occasionally fail do to floating point precision.
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        // Loop over possible internal algebras.
        for (final Algebra tmpS : Lists.getList(RealAlgebra.REAL_ALGEBRA, LogSignAlgebra.LOG_SIGN_ALGEBRA)) {
            Tensor t1 = new Tensor(s, 3,3);
            Identity<Tensor> id1 = new Identity<Tensor>(t1);
            Tensor t2 = new Tensor(s, 3,3);
            Identity<Tensor> id2 = new Identity<Tensor>(t2);
            Tensor2Factory fact = new Tensor2Factory() {
                public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                    return new ProjDepTreeModule(m1, m2, tmpS);
                }
            };        
            
            // Fill the tensors with 1.0 so that initial forward pass doesn't throw an error on the zeros. 
            t1.fill(s.one());
            t2.fill(s.one());
            
            // NOTE: The input to ProjDepTreeModule must be non-negative, so we use the Abs variant of the test function.
            AbstractModuleTest.evalTensor2ByFiniteDiffsAbs(fact, id1, id2);
        }
    }
        
}
