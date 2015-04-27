package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.mutable.MutableDouble;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.AbstractModuleTest.Tensor2Factory;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.ModuleTestUtils.ModuleFn;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TensorUtils;
import edu.jhu.prim.util.Prng;
import edu.jhu.prim.util.Lambda.FnIntDoubleToVoid;
import edu.jhu.prim.vector.IntDoubleDenseVector;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.Algebras;
import edu.jhu.util.semiring.LogSignAlgebra;
import edu.jhu.util.semiring.RealAlgebra;

public class SoftmaxMbrDepParseTest {

    Algebra s = new RealAlgebra();
    
    String expout = "Tensor (RealAlgebra) [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  0.146341\n"
            + "    0    1  |  0.146341\n"
            + "    1    0  |  0.853659\n"
            + "    1    1  |  0.853659\n"
            + "]";
    
    String expoutAdj = "Tensor (RealAlgebra) [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  2.85950e-16\n"
            + "    0    1  |  2.85950e-16\n"
            + "    1    0  |  1.66804e-15\n"
            + "    1    1  |  1.66804e-15\n"
            + "]";
    
    @Before
    public void setUp() {
        Prng.seed(Prng.DEFAULT_SEED);
    }
    
    @Test
    public void testSimpleReal() {
        helpSimple(new LogSignAlgebra());              
        //helpSimple(new RealAlgebra());
    }
    
    @Test    
    public void testSimpleLogPosNeg() {    
        helpSimple(new LogSignAlgebra());              
    }

    private void helpSimple(Algebra tmpS) {
        double T = 2;
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(TensorUtils.getVectorFromValues(s, 2, 3, 5, 7));
        // Take the log and multiply by T so that forward yields the same result as the test 
        // in InsideOutsideDepParseTest.
        t1.log();
        t1.multiply(T);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity temp = new TensorIdentity(Tensor.getScalarTensor(s, T));
        SoftmaxMbrDepParse ea = new SoftmaxMbrDepParse(id1, temp, tmpS);

        Tensor out = ea.forward();
        //System.out.println(out);        
        ea.report();
        assertEquals(expout, out.toString());
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(2.2);
        ea.backward();
        
        Tensor outAdj = id1.getOutputAdj();
        System.out.println(outAdj);
        // Backward yields a different result from the test in InsideOutsideDepParse because of the
        // exp(x/T).
        assertEquals(expoutAdj, outAdj.toString());
    }
    
    String expoutAdj2 = "Tensor (RealAlgebra) [\n"
            + "    0    1  |  value\n"
            + "    0    0  |  0.443426\n"
            + "    0    1  |  0.443426\n"
            + "    1    0  |  -0.443426\n"
            + "    1    1  |  -0.443426\n"
            + "]";
    @Test    
    public void testSimpleLogPosNeg2() {    
        helpSimple2(new RealAlgebra());              
    }

    private void helpSimple2(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 2,2);
        t1.setValuesOnly(TensorUtils.getVectorFromReals(s, .2, .3, .5, .7));
        // Take the log and multiply by T so that forward yields the same result as the test 
        // in InsideOutsideDepParseTest.
        //t1.log();
        //t1.multiply(100);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity temp = new TensorIdentity(Tensor.getScalarTensor(s, 1));
        SoftmaxMbrDepParse ea = new SoftmaxMbrDepParse(id1, temp, tmpS);

        Tensor out = ea.forward();
        //System.out.println(out);        
        //assertEquals(expout, out.toString());
        assertTrue(out == ea.getOutput());

        // Set the adjoint of the sum to be 1.
        ea.getOutputAdj().fill(s.zero());
        ea.getOutputAdj().setValue(0, s.one());
        ea.getOutputAdj().setValue(1, s.one());
        ea.backward();

        ea.report();

        Tensor outAdj = id1.getOutputAdj();
        System.out.println(outAdj);

        // TODO: Note that in this case you can actually see how the exp seems to more evenly prefer
        // the gold parse edges.
        assertEquals(expoutAdj2, outAdj.toString());
    }
    
    @Test
    public void testGradByFiniteDiffsReal() {   
        helpGradByFiniteDiffs(new RealAlgebra());
    }
    
    @Test
    public void testGradByFiniteDiffsLogPosNeg() {
        helpGradByFiniteDiffs(new LogSignAlgebra());
    }

    private void helpGradByFiniteDiffs(Algebra tmpS) {
        Tensor t1 = new Tensor(s, 4,4);
        TensorIdentity id1 = new TensorIdentity(t1);
        TensorIdentity temp = new TensorIdentity(Tensor.getScalarTensor(s, 2));
        SoftmaxMbrDepParse ea = new SoftmaxMbrDepParse(id1, temp, tmpS);
        
        int numParams = ModuleFn.getOutputSize(ea.getInputs());
        IntDoubleDenseVector x = ModuleTestUtils.getAbsZeroOneGaussian(numParams);
        final MutableDouble sum = new MutableDouble(0);
        x.iterate(new FnIntDoubleToVoid() {
            public void call(int idx, double val) {
                sum.add(val);
            }
        });
        x.scale(-1.0/sum.doubleValue());
        ModuleTestUtils.assertGradientCorrectByFd(ea, x, 1e-8, 1e-5);
    }
    
    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        // Loop over possible internal algebras.
        for (final Algebra tmpS : Lists.getList(Algebras.REAL_ALGEBRA, Algebras.LOG_SIGN_ALGEBRA)) {
            Tensor t1 = new Tensor(s, 4,4);
            TensorIdentity id1 = new TensorIdentity(t1);
            TensorIdentity temp = new TensorIdentity(Tensor.getScalarTensor(s, 2));
            Tensor2Factory fact = new Tensor2Factory() {
                public Module<Tensor> getModule(Module<Tensor> m1, Module<Tensor> m2) {
                    return new SoftmaxMbrDepParse(m1, m2, tmpS);
                }
            };        
            // NOTE: The input to SoftmaxMbrDepParse must be non-negative, so we use the Abs variant of the test function.
            AbstractModuleTest.evalTensor2ByFiniteDiffsAbs(fact, id1, temp);
        }
    }
}
