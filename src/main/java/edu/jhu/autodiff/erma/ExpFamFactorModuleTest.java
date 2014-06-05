package edu.jhu.autodiff.erma;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;

import org.junit.Test;

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

public class ExpFamFactorModuleTest {

    boolean logDomain = false;
    Algebra s = new RealAlgebra();
    
    @Test
    public void testSimple() {
        Var t0 = new Var(VarType.PREDICTED, 2, "t0", Lists.getList("N", "V"));
        ExplicitExpFamFactor emit1 = new ExplicitExpFamFactor(new VarSet(t0));
        for (int c=0; c<emit1.size(); c++) {
            FeatureVector features = new FeatureVector();
            features.set(c, c+1);
            emit1.setFeatures(c, features);
        }
        FactorGraph fg = new FactorGraph();
        fg.addFactor(emit1);
        FgModel model = new FgModel(emit1.size());
        model.getParams().set(0, 2);
        model.getParams().set(1, 3);
        
        ExpFamFactorsModule effm = new ExpFamFactorsModule(fg, model, s);
        effm.forward();
        VarTensor[] y = effm.getOutput();
        System.out.println(Arrays.deepToString(y));
        assertEquals(Math.exp(2*1), y[0].getValue(0), 1e-1);
        assertEquals(Math.exp(3*2), y[0].getValue(1), 1e-1);
        
        VarTensor[] yAdj = effm.getOutputAdj();
        for (int a=0; a<yAdj.length; a++) {
            yAdj[a].fill(5);
        }
        
        effm.backward();
        FgModel grad = effm.getModelAdj();
        System.out.println(grad);
        assertEquals(5*Math.exp(2*1)*1, grad.getParams().get(0), 1e-1);
        assertEquals(5*Math.exp(3*2)*2, grad.getParams().get(1), 1e-1);        
    }

}
