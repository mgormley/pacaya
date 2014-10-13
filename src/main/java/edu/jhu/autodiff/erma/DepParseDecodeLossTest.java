package edu.jhu.autodiff.erma;

import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.AbstractModuleTest.TwoToOneFactory;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.LinkVar;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactorTest;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactorTest.FgAndLinks;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class DepParseDecodeLossTest {

    private Algebra s = new RealAlgebra();

    @Test
    public void testDecodeLossWith3WordGlobalFactor() {
        // Get factor graph.
        FgAndLinks fgl = ProjDepTreeFactorTest.getFgl(false);
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
        
        BeliefsIdentity id1 = new BeliefsIdentity(b);
        TensorIdentity temp = new TensorIdentity(Tensor.getScalarTensor(s, 3));
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
