package edu.jhu.autodiff.erma;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.ModuleTestUtils.BeliefsVecFn;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactorTest;
import edu.jhu.gm.model.globalfac.ProjDepTreeFactor.LinkVar;
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
        VarConfig goldConfig = new VarConfig();
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
        DepParseDecodeLoss dl = new DepParseDecodeLoss(id1, goldConfig, 3);
        
        BeliefsVecFn vecFn = new BeliefsVecFn(id1, dl);
        ModuleTestUtils.assertFdAndAdEqual(vecFn, 1e-10, 1e-8);        
    }
    
    
}
