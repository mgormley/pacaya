package edu.jhu.gm.eval;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import edu.jhu.gm.inf.BruteForceInferencer;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.model.VarSetTest;

public class MseMarginalEvaluatorTest {

    static Var v1 = VarSetTest.getVar(0, 2);
    static Var v2 = VarSetTest.getVar(1, 2);
    static Var v3 = VarSetTest.getVar(2, 3);
    
    private static class MockFgInferencer extends BruteForceInferencer {

        public MockFgInferencer() {
            super(null, false);
        }
        
        public DenseFactor getMarginals(Var var) {
            DenseFactor marg = new DenseFactor(new VarSet(var));
            if (var == v1) {
                marg.setValue(0, 0.4);
                marg.setValue(1, 0.6);
            } else if (var == v2) {
                marg.setValue(0, 0.3);
                marg.setValue(1, 0.7);
            } else if (var == v3) {
                marg.setValue(0, 0.2);
                marg.setValue(1, 0.3);
                marg.setValue(2, 0.5);
            } else {
                throw new RuntimeException();
            }
            return marg;
        }
        
    }
    
    @Test
    public void testMse() {
        MseMarginalEvaluator mse = new MseMarginalEvaluator();
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(v1, 0);
        goldConfig.put(v2, 1);
        goldConfig.put(v3, 1);
        double val = mse.evaluate(goldConfig, new MockFgInferencer());
        double eval = Math.pow(1-.4, 2) + Math.pow(0-.6, 2) + Math.pow(0-.3, 2) + Math.pow(1-.7, 2)  
                + Math.pow(0-.2, 2) + Math.pow(1-.3, 2) + Math.pow(0-.5, 2);
        assertEquals(eval, val, 1e-13);
    }

}