package edu.jhu.pacaya.gm.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.pacaya.autodiff.Module;
import edu.jhu.pacaya.gm.feat.FeatureVector;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.FeatureNames;
import edu.jhu.pacaya.util.collections.QLists;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;


public class ExplicitExpFamFactorTest {

    @Test
    public void testGetFactorModule() throws Exception {
        Algebra s = RealAlgebra.getInstance();
        VarTensor y = getOutputOfModule(s);
        assertEquals(s, y.getAlgebra());        
        assertEquals(Math.exp(1+2), y.get(0), 1e-1);
        assertEquals(Math.exp(2+3+4), y.get(1), 1e-1);
        assertEquals(Math.exp(1+2), y.get(2), 1e-1);
    }

    @Test
    public void testGetFactorModuleLog() throws Exception {
        Algebra s = LogSemiring.getInstance();
        VarTensor y = getOutputOfModule(s);
        assertEquals(s, y.getAlgebra());        
        assertEquals((1+2), y.get(0), 1e-1);
        assertEquals((2+3+4), y.get(1), 1e-1);
        assertEquals((1+2), y.get(2), 1e-1);
    }
    
    protected VarTensor getOutputOfModule(Algebra s) {
        // Model - 4 for a,b,c,d.
        FgModel model = new FgModel(4);
        model.fill(0.0);
        for (int i=0; i<model.getNumParams(); i++) {
            model.getParams().set(i, i+1);
        }
        FgModelIdentity id1 = new FgModelIdentity(model);
        // Features
        FeatureNames alphabet = new FeatureNames();
        FeatureVector f1 = new FeatureVector();
        f1.add(alphabet.lookupIndex("f-a"), 1.0);
        f1.add(alphabet.lookupIndex("f-b"), 1.0);
        FeatureVector f2 = new FeatureVector();
        f2.add(alphabet.lookupIndex("f-b"), 1.0);
        f2.add(alphabet.lookupIndex("f-c"), 1.0);
        f2.add(alphabet.lookupIndex("f-d"), 1.0);
        // Var
        VarSet vars = new VarSet(new Var(VarType.PREDICTED, 3, "y", QLists.getList("y-a", "y-b", "y-c")));

        ExplicitExpFamFactor f = new ExplicitExpFamFactor(vars);
        f.setFeatures(0, f1);
        f.setFeatures(1, f2);
        f.setFeatures(2, f1);
        
        Module<VarTensor> fm = (Module<VarTensor>) f.getFactorModule(id1, s);
        VarTensor y = fm.forward();
        System.out.println(fm.getOutput());
        return y;
    }
    
}
