package edu.jhu.pacaya.gm.eval;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.junit.Test;

import edu.jhu.pacaya.gm.inf.BruteForceInferencer;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.gm.model.VarTensor;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class MseMarginalEvaluatorTest {

    static Var v1 = getVar(0, 2);
    static Var v2 = getVar(1, 2);
    static Var v3 = getVar(2, 3);
    
    private static class MockFgInferencer extends BruteForceInferencer {

        public MockFgInferencer() {
            super(null, false);
        }
        
        public VarTensor getMarginals(Var var) {
            VarTensor marg = new VarTensor(RealAlgebra.REAL_ALGEBRA, new VarSet(var));
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

    public static Var getVar(int id, int numStates) {
        ArrayList<String> stateNames = new ArrayList<String>();
        for (int i=0; i<numStates; i++) {
            stateNames.add("state" + i);
        }
        return new Var(VarType.PREDICTED, numStates, "var"+id, stateNames);
    }
}
