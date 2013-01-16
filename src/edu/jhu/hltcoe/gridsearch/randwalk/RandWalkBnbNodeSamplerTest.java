package edu.jhu.hltcoe.gridsearch.randwalk;

import java.io.File;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainerTest;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer.BnBDmvTrainerPrm;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation.DmvRltRelaxPrm;
import edu.jhu.hltcoe.gridsearch.randwalk.RandWalkBnbNodeSampler.CostEstimator;
import edu.jhu.hltcoe.gridsearch.randwalk.RandWalkBnbNodeSampler.RandWalkBnbSamplerPrm;
import edu.jhu.hltcoe.train.DmvTrainCorpus;


public class RandWalkBnbNodeSamplerTest {

    @Before
    public void setUp() {
        //Prng.seed(1234567890);
    }
    
    @Test
    public void testCostEstimatorTreeSize() {
        // Test tree size
        CostEstimator est = new CostEstimator();
        est.add(0, 2, 1);
        est.add(1, 2, 1);
        est.add(2, 2, 1);
        Assert.assertTrue(Double.isNaN(est.getMean()));
        est.doneWithSample();
        Assert.assertEquals(1+2+4, est.getMean(), 1e-13);
        Assert.assertTrue(Double.isNaN(est.getVariance()));
        est.add(0, 2, 1);
        est.add(1, 2, 1);
        est.add(2, 2, 1);
        est.add(3, 2, 1);
        est.doneWithSample();
        // mean = 11
        Assert.assertEquals((1+2+4 + 1+2+4+8)/2, est.getMean(), 1e-13);
        // variance = (7-11)^2 + (7-11)^2 / 1 = 32
        Assert.assertEquals(32, est.getVariance(), 1e-13);
        Assert.assertEquals(5.656, est.getStdDev(), 1e-3);
    }
    
    @Test
    public void testCostEstimatorTreeTime() {
        // Test tree size
        CostEstimator est = new CostEstimator();
        est.add(0, 2, 3);
        est.add(1, 2, 5);
        est.add(2, 2, 7);
        Assert.assertTrue(Double.isNaN(est.getMean()));
        est.doneWithSample();
        Assert.assertEquals(1*3+2*5+4*7, est.getMean(), 1e-13);
        Assert.assertTrue(Double.isNaN(est.getVariance()));
        est.add(0, 2, 3);
        est.add(1, 2, 5);
        est.add(2, 2, 7);
        est.add(3, 2, 11);
        est.doneWithSample();
        Assert.assertEquals((1*3+2*5+4*7 + 1*3+2*5+4*7+8*11)/2, est.getMean(), 1e-13);
        Assert.assertEquals(3872, est.getVariance(), 1e-13);
        Assert.assertEquals(62.225, est.getStdDev(), 1e-3);
    }
    
    @Test
    public void testRegretSampling() {
        RandWalkBnbSamplerPrm prm = new RandWalkBnbSamplerPrm();
        prm.maxSamples = 10;
        prm.bnbPrm.timeoutSeconds = 3;
        DmvRltRelaxPrm rrPrm = new DmvRltRelaxPrm();
        rrPrm.rootMaxCutRounds = 0;
        rrPrm.maxCutRounds = 0;
        
        // -- temp --
        rrPrm.tempDir = new File(".");
        rrPrm.rltPrm.nameRltVarsAndCons = true;
        // --  --

        BnBDmvTrainerPrm bnbtPrm = new BnBDmvTrainerPrm();
        bnbtPrm.bnbSolverFactory = prm;
        bnbtPrm.brancher = BnBDmvTrainerTest.getDefaultBrancher();
        bnbtPrm.relaxFactory = rrPrm;
        BnBDmvTrainer trainer = new BnBDmvTrainer(bnbtPrm);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate");
        sentences.addSentenceFromString("the mouse ate");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
    @Test
    public void testRandomSampling() {
        RandWalkBnbSamplerPrm prm = new RandWalkBnbSamplerPrm();
        prm.maxSamples = 10;
        prm.bnbPrm.timeoutSeconds = 3;
        DmvRltRelaxPrm rrPrm = new DmvRltRelaxPrm();
        rrPrm.rootMaxCutRounds = 0;
        rrPrm.maxCutRounds = 0;

        BnBDmvTrainerPrm bnbtPrm = new BnBDmvTrainerPrm();
        bnbtPrm.bnbSolverFactory = prm;
        bnbtPrm.brancher = getRandomBrancher();
        bnbtPrm.relaxFactory = rrPrm;
        BnBDmvTrainer trainer = new BnBDmvTrainer(bnbtPrm);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));
    }

    public static CptBoundsDeltaFactory getRandomBrancher() {
        VariableSelector varSelector = new RandomVariableSelector(true);
        VariableSplitter varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_PROB);
        return new BasicCptBoundsDeltaFactory(varSelector, varSplitter);
    }
    
}
