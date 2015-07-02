package edu.jhu.pacaya.autodiff.vartensor;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.MVecArray;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.pacaya.autodiff.erma.Beliefs;
import edu.jhu.pacaya.autodiff.erma.ExpectedRecall;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests;
import edu.jhu.pacaya.gm.model.FactorGraphsForTests.FgAndVars;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class VTProdTest {

    private Module<MVecArray<VarTensor>> modIn;

    @Before
    public void setUp() {
        FgAndVars fgv = FactorGraphsForTests.getLinearChainFgWithVars();
        // Create input module.
        //Algebra s = RealAlgebra.REAL_ALGEBRA;
        VarTensor[] xsa = new VarTensor[3];
        xsa[0] = new VarTensor(fgv.emit0);
        xsa[1] = new VarTensor(fgv.emit1);
        xsa[2] = new VarTensor(fgv.tran0);
        MVecArray<VarTensor> xs = new MVecArray<>(xsa);
        for (int a=0; a<xs.dim(); a++) {
            xs.get(a).exp();
        }
        modIn = new Identity<MVecArray<VarTensor>>(xs);
    }
    
    @Test
    public void testForward() throws Exception {
        VTProd prod = new VTProd(modIn);
        VarTensor joint = prod.forward();
        System.out.println(joint);
        assertEquals(0.006, joint.get(0,0,0,0), 1e-13);
        assertEquals(0.028, joint.get(0,0,0,1), 1e-13);
        assertEquals(0.081, joint.get(0,0,1,0), 1e-13);
        assertEquals(0.315, joint.get(0,0,1,1), 1e-13);
        prod.getOutputAdj().fill(1.0);
        prod.backward();
        System.out.println(modIn.getOutputAdj());
        assertEquals(0.34, modIn.getOutputAdj().getValue(0), 1e-13);
        assertEquals(0.44, modIn.getOutputAdj().getValue(1), 1e-13);
        assertEquals(0.34, modIn.getOutputAdj().getValue(2), 1e-13);
        assertEquals(0.44, modIn.getOutputAdj().getValue(3), 1e-13);
    }

    @Test
    public void testGradByFiniteDiffsAllSemirings() {
        OneToOneFactory<MVecArray<VarTensor>,VarTensor> fact = new OneToOneFactory<MVecArray<VarTensor>,VarTensor>() {
            public Module<VarTensor> getModule(Module<MVecArray<VarTensor>> m1) {
                return new VTProd(m1);
            }
        };
        AbstractModuleTest.evalOneToOneByFiniteDiffsAbs(fact, modIn);
    }
    
}
