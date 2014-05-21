package edu.jhu.autodiff.erma;

import org.junit.Test;

import edu.jhu.autodiff.erma.ErmaBp.ErmaBpPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraphTest;
import edu.jhu.gm.model.FactorGraphTest.FgAndVars;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.util.dist.Gaussian;


public class ErmaBpBackwardTest {
	
	@Test
	public void testGradientMatchesFiniteDifferences() {
	    boolean logDomain = false;
        FgAndVars fgv = FactorGraphTest.getLinearChainFgWithVars(logDomain);
        FactorGraph fg = fgv.fg;
        
        VarConfig goldConfig = new VarConfig();
        goldConfig.put(fgv.w0, 0);
        goldConfig.put(fgv.w1, 1);
        goldConfig.put(fgv.w2, 0);
        goldConfig.put(fgv.t1, 1);
        goldConfig.put(fgv.t2, 1);
        
	    // Define the "model" as the explicit factor entries.
	    int numParams = 0;
	    for (Factor f : fg.getFactors()) {
	        numParams += f.getVars().calcNumConfigs();
	    }
	    // Randomly initialize the model.
	    double[] params = new double[numParams];
	    for (int i=0; i<params.length; i++) {
	        params[i] = Gaussian.nextDouble(0.0, 1.0);
	    }
	    
	    ErmaBpPrm prm = new ErmaBpPrm();
	    prm.updateOrder = BpUpdateOrder.SEQUENTIAL;
	    prm.schedule = BpScheduleType.TREE_LIKE;
	    prm.logDomain = logDomain;
	    ErmaBp bp = new ErmaBp(fg, prm);
	    bp.forward();
        
	    ExpectedRecall er = new ExpectedRecall(bp, goldConfig);
	    er.forward();
	    
	    double goal = er.getExpectedRecall();
	    
	    er.backward(1);
	    bp.backward(er.getVarBeliefsAdjs(), er.getFacBeliefsAdjs());
	    
	    DenseFactor[] potentialsAdj = bp.getPotentialsAdj();
	    for (DenseFactor adj : potentialsAdj) {
	        System.out.println(adj);
	    }
	}
    
}
