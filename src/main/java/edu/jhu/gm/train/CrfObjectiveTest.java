package edu.jhu.gm.train;

import static edu.jhu.data.simple.SimpleAnnoSentenceCollection.getSingleton;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.gm.data.FgExample;
import edu.jhu.gm.data.FgExampleList;
import edu.jhu.gm.data.FgExampleMemoryStore;
import edu.jhu.gm.feat.FactorTemplateList;
import edu.jhu.gm.feat.ObsFeatureConjoiner;
import edu.jhu.gm.feat.ObsFeatureConjoiner.ObsFeatureConjoinerPrm;
import edu.jhu.gm.inf.BeliefPropagation;
import edu.jhu.gm.inf.BeliefPropagation.BeliefPropagationPrm;
import edu.jhu.gm.inf.BeliefPropagation.BpScheduleType;
import edu.jhu.gm.inf.BeliefPropagation.BpUpdateOrder;
import edu.jhu.gm.inf.BeliefPropagation.FgInferencerFactory;
import edu.jhu.gm.inf.BfsBpSchedule;
import edu.jhu.gm.inf.BruteForceInferencer.BruteForceInferencerPrm;
import edu.jhu.gm.inf.FgInferencer;
import edu.jhu.gm.model.DenseFactor;
import edu.jhu.gm.model.ExplicitFactor;
import edu.jhu.gm.model.Factor;
import edu.jhu.gm.model.FactorGraph;
import edu.jhu.gm.model.FactorGraph.FgEdge;
import edu.jhu.gm.model.FgModel;
import edu.jhu.gm.model.FgModelTest;
import edu.jhu.gm.model.Var;
import edu.jhu.gm.model.Var.VarType;
import edu.jhu.gm.model.VarConfig;
import edu.jhu.gm.model.VarSet;
import edu.jhu.gm.train.CrfObjective.CrfObjectivePrm;
import edu.jhu.prim.util.math.FastMath;
import edu.jhu.srl.CorpusStatistics;
import edu.jhu.srl.CorpusStatistics.CorpusStatisticsPrm;
import edu.jhu.srl.SrlFactorGraph.RoleStructure;
import edu.jhu.srl.SrlFgExamplesBuilder;
import edu.jhu.srl.SrlFgExamplesBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.util.collections.Lists;

public class CrfObjectiveTest {

	@Test
	public void logLikelihoodBelowZeroBPLogDomain() {	// belief propagation
		BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
		bpPrm.logDomain = true;
		logLikelihoodBelowZero(bpPrm);
	}
	
	@Test
	public void logLikelihoodBelowZeroBPProbDomain() {	// belief propagation
		BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
		bpPrm.logDomain = false;
		logLikelihoodBelowZero(bpPrm);
	}
	
	@Test
	public void logLikelihoodBelowZeroBF() {	// brute force
		logLikelihoodBelowZero(new BruteForceInferencerPrm(false));
		logLikelihoodBelowZero(new BruteForceInferencerPrm(true));
	}
	
	
	/**
	 * log probabilities should be less than 0...
	 * make a chain of binary variables with one factor one each.
	 * more complicated models are not needed, just want to show
	 * that LL comes out >0.
	 */
	public void logLikelihoodBelowZero(FgInferencerFactory infFactory) {
		
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
			double v = infFactory.isLogDomain()
					? Math.sqrt(i + 1)
					: 1d / Math.sqrt(i + 1);
			DenseFactor df = new DenseFactor(new VarSet(xi), v);
			df.setValue(0, infFactory.isLogDomain()
					? 1d
					: 1d - v);
			Factor f = new ExplicitFactor(df);
			if(!infFactory.isLogDomain())
				assertEquals(1d, df.getSum(), 1e-8);
			fg.addFactor(f);
		}
		
		assertTrue(fg.getEdges().size() > 0);
		
		// find out what the log-likelihood is
		CrfTrainer.CrfTrainerPrm trainerPrm = new CrfTrainer.CrfTrainerPrm();
		trainerPrm.infFactory = infFactory;
		
		FgExampleMemoryStore exs = new FgExampleMemoryStore();
		
		// first, create a few instances of this factor graph
		Random rand = new Random();
		for(int i=0; i<10; i++) {
			VarConfig gold = new VarConfig();
			for(int j=0; j<chainLen; j++)
				gold.put(x.get(j), rand.nextInt(xNames.size()));
			FgExample e = new FgExample(fg, gold);
			exs.add(e);
		}
		
		FgModel model = new FgModel(1);	// model is not important, have only Explicit/DenseFactors
		CrfObjective objective = new CrfObjective(trainerPrm.crfObjPrm, model, exs, trainerPrm.infFactory);
		double objVal = objective.getValue();
		System.out.println("objVal = " + objVal);
		assertTrue(objVal < 0d);
		System.out.println("[logLikelihoodBelowZero] done");
	}
    
	
    @Test
    public void testSrlLogLikelihood() throws Exception {
        List<CoNLL09Token> tokens = new ArrayList<CoNLL09Token>();
        //tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", new ArrayList<String>()));
        //tokens.add(new CoNLL09Token(id, form, lemma, plemma, pos, ppos, feat, pfeat, head, phead, deprel, pdeprel, fillpred, pred, apreds));
//        tokens.add(new CoNLL09Token(1, "the", "_", "_", "Det", "_", getList("feat"), getList("feat") , 2, 2, "det", "_", false, "_", getList("_")));
        tokens.add(new CoNLL09Token(2, "dog", "_", "_", "N", "_", Lists.getList("feat"), Lists.getList("feat") , 2, 2, "subj", "_", false, "_", Lists.getList("arg0")));
        tokens.add(new CoNLL09Token(3, "ate", "_", "_", "V", "_", Lists.getList("feat"), Lists.getList("feat") , 0, 0, "v", "_", true, "ate.1", Lists.getList("_")));
        //tokens.add(new CoNLL09Token(4, "food", "_", "_", "N", "_", getList("feat"), getList("feat") , 2, 2, "obj", "_", false, "_", getList("arg1")));
        CoNLL09Sentence sent = new CoNLL09Sentence(tokens);
                
        System.out.println("Done reading.");
        CorpusStatisticsPrm csPrm = new CorpusStatisticsPrm();
        CorpusStatistics cs = new CorpusStatistics(csPrm);
        SimpleAnnoSentenceCollection sents = getSingleton(sent.toSimpleAnnoSentence(csPrm.useGoldSyntax));
        cs.init(sents);
        
        FactorTemplateList fts = new FactorTemplateList();
        SrlFgExampleBuilderPrm prm = new SrlFgExampleBuilderPrm();
        prm.fgPrm.makeUnknownPredRolesLatent = false;
        prm.fgPrm.roleStructure = RoleStructure.PREDS_GIVEN;
        prm.fgPrm.useProjDepTreeFactor = true;
        prm.srlFePrm.fePrm.biasOnly = true;
        
        ObsFeatureConjoiner ofc = new ObsFeatureConjoiner(new ObsFeatureConjoinerPrm(), fts);
        SrlFgExamplesBuilder builder = new SrlFgExamplesBuilder(prm, ofc, cs);
        FgExampleList data = builder.getData(sents);
        ofc.init(data);
        
        System.out.println("Num features: " + fts.getNumObsFeats());
        FgModel model = new FgModel(ofc.getNumParams());

        boolean logDomain = false;
        FgInferencerFactory infFactory = getInfFactory(logDomain);        
        FgExample ex = data.get(0);
        
        FgInferencer infLat = infFactory.getInferencer(ex.getFgLat());
        FactorGraph fgLat = ex.updateFgLat(model, infLat.isLogDomain());
        infLat.run();        
        assertEquals(2, infLat.getPartition(), 2);
        // Check that the partition function is computed identically for each variable.
        for (Var v : fgLat.getVars()) {
            double partition = ((BeliefPropagation)infLat).getPartitionFunctionAtVarNode(fgLat.getNode(v));
            assertEquals(2, logDomain ? FastMath.exp(partition) : partition, 1e-3);
        }
        
        System.out.println("-------- Running LatPred Inference-----------");
        
        FgInferencer infLatPred = infFactory.getInferencer(ex.getFgLatPred());
        FactorGraph fgLatPred = ex.updateFgLatPred(model, infLatPred.isLogDomain());
        infLatPred.run();        
        // 2 trees, and 3 different roles (including argUNK)
        assertEquals(2*3, infLatPred.getPartition(), 2);         

        // Print schedule:
        BfsBpSchedule schedule = new BfsBpSchedule(fgLatPred);        
        System.out.println();
        for (FgEdge edge : schedule.getOrder()) {
            System.out.println(edge.toString());
        }
        System.out.println();
        // Print factors
        for (Factor f : fgLatPred.getFactors()) {
            System.out.println(f);
        }
        // Check that the partition function is computed identically for each variable.
        for (Var v : fgLatPred.getVars()) {
            double partition = ((BeliefPropagation)infLatPred).getPartitionFunctionAtVarNode(fgLatPred.getNode(v));
            System.out.format("Var=%s partition=%.4f\n", v.toString(), partition);
            assertEquals(2*3, logDomain ? FastMath.exp(partition) : partition, 1e-3);
        }
        
        CrfObjective obj = new CrfObjective(new CrfObjectivePrm(), model, data, infFactory);
        obj.setPoint(FgModelTest.getParams(model));
        double ll = obj.getValue();        
        assertEquals(2./6., FastMath.exp(ll), 1e-13);
    }

    public static FgInferencerFactory getInfFactory(boolean logDomain) {
        BeliefPropagationPrm bpPrm = new BeliefPropagationPrm();
        bpPrm.logDomain = logDomain;
        bpPrm.schedule = BpScheduleType.TREE_LIKE;
        bpPrm.updateOrder = BpUpdateOrder.SEQUENTIAL;
        bpPrm.normalizeMessages = false;
        bpPrm.maxIterations = 1;        
        return bpPrm;
    }
    
}
