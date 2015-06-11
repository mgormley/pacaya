package edu.jhu.pacaya.gm.train;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Ignore;
import org.junit.Test;

import edu.jhu.hlt.optimize.MalletLBFGS;
import edu.jhu.hlt.optimize.MalletLBFGS.MalletLBFGSPrm;
import edu.jhu.hlt.optimize.function.Function;
import edu.jhu.pacaya.gm.data.FgExampleList;
import edu.jhu.pacaya.gm.data.FgExampleMemoryStore;
import edu.jhu.pacaya.gm.data.LFgExample;
import edu.jhu.pacaya.gm.data.LabeledFgExample;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.pacaya.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.pacaya.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.pacaya.gm.inf.FgInferencerFactory;
import edu.jhu.pacaya.gm.model.ExplicitFactor;
import edu.jhu.pacaya.gm.model.FactorGraph;
import edu.jhu.pacaya.gm.model.FgModel;
import edu.jhu.pacaya.gm.model.Var;
import edu.jhu.pacaya.gm.model.Var.VarType;
import edu.jhu.pacaya.gm.model.VarConfig;
import edu.jhu.pacaya.gm.model.VarSet;
import edu.jhu.pacaya.util.semiring.Algebra;
import edu.jhu.pacaya.util.semiring.LogSemiring;
import edu.jhu.pacaya.util.semiring.RealAlgebra;

public class CrfObjectiveTest {

    @Ignore("not implemented")
    @Test
    public void testGetLogLikelihood() {
        // TODO: Test the getValue method returns what we expect.
        //FactorGraph fg = FactorGraphTest.getLinearChainGraph();
    }
    
	@Test
	public void testLogLikelihoodBelowZeroBPLogDomain() {	// belief propagation
		BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
		bpPrm.s = LogSemiring.SINGLETON;
		checkLogLikelihoodBelowZero(bpPrm);
	}
	
	@Test
	public void testLogLikelihoodBelowZeroBPProbDomain() {	// belief propagation
		BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
		bpPrm.s = RealAlgebra.SINGLETON;
		checkLogLikelihoodBelowZero(bpPrm);
	}
	
	@Test
	public void testLogLikelihoodBelowZeroBF() {	// brute force
		checkLogLikelihoodBelowZero(new BruteForceInferencerPrm(RealAlgebra.SINGLETON));
		checkLogLikelihoodBelowZero(new BruteForceInferencerPrm(LogSemiring.SINGLETON));
	}
		
	/**
	 * log probabilities should be less than 0...
	 * make a chain of binary variables with one factor one each.
	 * more complicated models are not needed, just want to check if
	 * LL comes out <=0.
	 */
	public static void checkLogLikelihoodBelowZero(FgInferencerFactory infFactory) {
		
		System.out.println("[logLikelihoodBelowZero] starting...");
		FactorGraph fg = new FactorGraph();
		List<String> xNames = new ArrayList<String>() {{ add("hot"); add("cold"); }};
		List<Var> x = new ArrayList<Var>();
		int chainLen = 5;
		for(int i=0; i<chainLen; i++) {
			
			// variable
			Var xi = new Var(VarType.PREDICTED, xNames.size(), "x"+i, xNames);
			fg.addVar(xi);
			x.add(xi);
			
			// factor
			ExplicitFactor f = new ExplicitFactor(new VarSet(xi));
			f.fill(Math.sqrt(i + 1));
			f.setValue(0,  1d);
			//assertEquals(1d, f.getSum(), 1e-8);
			fg.addFactor(f);
		}
		
		assertTrue(fg.getEdges().size() > 0);
		
		// find out what the log-likelihood is
		CrfTrainer.CrfTrainerPrm trainerPrm = new CrfTrainer.CrfTrainerPrm();
		trainerPrm.optimizer = new MalletLBFGS(new MalletLBFGSPrm());
		trainerPrm.infFactory = infFactory;
		
		FgExampleMemoryStore exs = new FgExampleMemoryStore();
		
		// first, create a few instances of this factor graph
		Random rand = new Random();
		for(int i=0; i<10; i++) {
			VarConfig gold = new VarConfig();
			for(int j=0; j<chainLen; j++)
				gold.put(x.get(j), rand.nextInt(xNames.size()));
			LFgExample e = new LabeledFgExample(fg, gold);
			exs.add(e);
		}
		
		FgModel model = new FgModel(1);	// model is not important, have only Explicit/DenseFactors
		Function objective = getCrfObj(model, exs, infFactory);
		double objVal = objective.getValue(model.getParams());
		System.out.println("objVal = " + objVal);
		assertTrue(objVal < 0d);
		System.out.println("[logLikelihoodBelowZero] done");
	}
	
    public static AvgBatchObjective getCrfObj(FgModel model, FgExampleList data, FgInferencerFactory infFactory) {
        CrfObjective exObj = new CrfObjective(data, infFactory);
        return new AvgBatchObjective(exObj, model, 1);
    }

    public static FgInferencerFactory getInfFactory(Algebra s) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.s = s;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;        
        return bpPrm;
    }

}
