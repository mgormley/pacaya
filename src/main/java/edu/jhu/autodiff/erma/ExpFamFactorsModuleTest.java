package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.autodiff.AbstractModuleTest;
import edu.jhu.autodiff.AbstractModuleTest.OneToOneFactory;
import edu.jhu.autodiff.Module;
import edu.jhu.autodiff.ModuleTestUtils;
import edu.jhu.autodiff.Tensor;
import edu.jhu.autodiff.TensorIdentity;
import edu.jhu.autodiff.TensorUtils;
import edu.jhu.gm.feat.FeatureVector;
import edu.jhu.gm.model.ExplicitExpFamFactor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarTensor;
import edu.jhu.util.collections.Lists;
import edu.jhu.util.semiring.Algebra;
import edu.jhu.util.semiring.RealAlgebra;

public class ExpFamFactorsModuleTest {

    boolean logDomain = false;
    Algebra s = new RealAlgebra();
        
    private final FgModel model;
    private final FactorGraph fg;
    
    public ExpFamFactorsModuleTest() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        ExplicitExpFamFactor emit1 = new ExplicitExpFamFactor(new VarSet(t0));
        for (int c=0; c<emit1.size(); c++) {
            FeatureVector features = new FeatureVector();
            features.set(c, c+1);
            emit1.setFeatures(c, features);
        }
        fg = new FactorGraph();
        fg.addFactor(emit1);
        model = new FgModel(emit1.size());
    }
    
    @Test
    public void testSimple() {
        model.fill(0.0);
        model.getParams().set(0, 2);
        model.getParams().set(1, 3);
        FgModelIdentity id1 = new FgModelIdentity(model);
        
        ExpFamFactorsModule effm = new ExpFamFactorsModule(id1, fg, s);
        effm.forward();
        VarTensor[] y = effm.getOutput().f;
        System.out.println(Arrays.deepToString(y));
        assertEquals(Math.exp(2*1), y[0].getValue(0), 1e-1);
        assertEquals(Math.exp(3*2), y[0].getValue(1), 1e-1);
        
        VarTensor[] yAdj = effm.getOutputAdj().f;
        for (int a=0; a<yAdj.length; a++) {
            yAdj[a].fill(5);
        }
        
        effm.backward();
        FgModel grad = id1.getOutputAdj().getModel();
        System.out.println(grad);
        assertEquals(5*Math.exp(2*1)*1, grad.getParams().get(0), 1e-1);
        assertEquals(5*Math.exp(3*2)*2, grad.getParams().get(1), 1e-1);        
    }
    
    @Test
    public void testGradByFiniteDiffs() {
        // This tests ONLY the real semiring, since that is the only supported semiring.
        model.fill(0.0);
        FgModelIdentity id1 = new FgModelIdentity(model);        
        ExpFamFactorsModule effm = new ExpFamFactorsModule(id1, fg, s);
        ModuleTestUtils.assertFdAndAdEqual(effm, 1e-5, 1e-8);
    }

}
