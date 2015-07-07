package edu.jhu.pacaya.gm.train;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.AbstractModuleTest;
import edu.jhu.pacaya.autodiff.AbstractModuleTest.TwoToOneFactory;
import edu.jhu.pacaya.autodiff.Identity;
import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.autodiff.ModuleTestUtils;
import edu.jhu.pacaya.autodiff.Tensor;
import edu.jhu.pacaya.gm.inf.Beliefs;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.globalfac.LinkVar;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactorTest;
import edu.jhu.pacaya.gm.model.globalfac.ProjDepTreeFactorTest.FgAndLinks;
import edu.jhu.pacaya.gm.train.DepParseDecodeLoss.DepParseDecodeLossFactory;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class DepParseDecodeLossTest {

    private Algebra s = RealAlgebra.getInstance();

    @Test
    public void testTemperatureLinearScale() {
        // Linear scaling.
        DepParseDecodeLossFactory fac = new DepParseDecodeLossFactory();
        fac.annealMse = false;
        fac.startTemp = 100;
        fac.endTemp = 0.1;
        fac.useLogScale = false;
        int maxIter = 9;
        for (int i=0; i<=maxIter; i++) {
            System.out.printf("i=%d temp=%f\n", i, fac.getTemperature(i, maxIter));
        }
        assertEquals(fac.startTemp, fac.getTemperature(0, maxIter), 1e-13);
        assertEquals(44.5, fac.getTemperature(5, maxIter), 1e-13);
        assertEquals(fac.endTemp, fac.getTemperature(maxIter, maxIter), 1e-13);
    }
    
    @Test
    public void testTemperatureLogScale() {
        // Linear scaling.
        DepParseDecodeLossFactory fac = new DepParseDecodeLossFactory();
        fac.annealMse = false;
        fac.startTemp = 100;
        fac.endTemp = 0.1;
        fac.useLogScale = true;
        int maxIter = 9;
        for (int i=0; i<=maxIter; i++) {
            System.out.printf("i=%d temp=%f\n", i, fac.getTemperature(i, maxIter));
        }
        assertEquals(fac.startTemp, fac.getTemperature(0, maxIter), 1e-13);
        assertEquals(10, fac.getTemperature(3, maxIter), 1e-13);
        assertEquals(1, fac.getTemperature(6, maxIter), 1e-13);
        assertEquals(fac.endTemp, fac.getTemperature(maxIter, maxIter), 1e-13);
    }
    
    @Test
    public void testDecodeLossWith3WordGlobalFactor() {
        // Get factor graph.
        FgAndLinks fgl = ProjDepTreeFactorTest.getFgl();
        FactorGraph fg = fgl.fg;
        LinkVar[] rootVars = fgl.rootVars;
        LinkVar[][] childVars = fgl.childVars;
                
        // Get variable configuration:
        final VarConfig goldConfig = new VarConfig();
        goldConfig.put(rootVars[0], 0);
        goldConfig.put(rootVars[1], 1);
        goldConfig.put(rootVars[2], 0);
        goldConfig.put(childVars[0][1], 0);
        goldConfig.put(childVars[0][2], 0);
        goldConfig.put(childVars[1][0], 1);
        goldConfig.put(childVars[1][2], 1);
        goldConfig.put(childVars[2][0], 0);
        goldConfig.put(childVars[2][1], 0);
               
        Beliefs b = new Beliefs(s);
        b.varBeliefs = new VarTensor[3*3];
        b.facBeliefs = new VarTensor[0];
        for (int v=0; v<b.varBeliefs.length; v++) {
            b.varBeliefs[v] = new VarTensor(s, new VarSet(fg.getVar(v)), 0.5);
        }
        
        Identity<Beliefs> id1 = new Identity<Beliefs>(b);
        Identity<Tensor> temp = new Identity<Tensor>(Tensor.getScalarTensor(s, 3));
        DepParseDecodeLoss dl = new DepParseDecodeLoss(id1, goldConfig, temp);
        
        ModuleTestUtils.assertGradientCorrectByFd(dl, 1e-8, 1e-5);      
        
        // testGradByFiniteDiffsAllSemirings
        TwoToOneFactory<Beliefs,Tensor,Tensor> fact = new TwoToOneFactory<Beliefs,Tensor,Tensor>() {
            public Module<Tensor> getModule(Module<Beliefs> m1, Module<Tensor> m2) {
                return new DepParseDecodeLoss(m1, goldConfig, m2);
            }
        };        
        AbstractModuleTest.evalTwoToOneByFiniteDiffsAbs(fact, id1, temp);
    }    
    
}
