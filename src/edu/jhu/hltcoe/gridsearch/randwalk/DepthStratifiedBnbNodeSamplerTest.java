package edu.jhu.hltcoe.gridsearch.randwalk;

import java.io.File;

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
import edu.jhu.hltcoe.gridsearch.randwalk.DepthStratifiedBnbNodeSampler.DepthStratifiedBnbSamplerPrm;
import edu.jhu.hltcoe.train.DmvTrainCorpus;


public class DepthStratifiedBnbNodeSamplerTest {

    @Before
    public void setUp() {
        //Prng.seed(1234567890);
    }
    
    @Test
    public void testRegretSampling() {
        DmvRltRelaxPrm rrPrm = new DmvRltRelaxPrm();
        // -- temp --
        rrPrm.tempDir = new File(".");
        rrPrm.rltPrm.nameRltVarsAndCons = true;
        // --  --
        
        DepthStratifiedBnbSamplerPrm prm = new DepthStratifiedBnbSamplerPrm();
        prm.maxDepth = 10;
        prm.maxSamples = 3;
        BnBDmvTrainerPrm bnbtPrm = new BnBDmvTrainerPrm();
        bnbtPrm.bnbSolverFactory = prm;
        bnbtPrm.brancher = BnBDmvTrainerTest.getDefaultBrancher();
        bnbtPrm.relaxFactory = rrPrm;
        BnBDmvTrainer trainer = new BnBDmvTrainer(bnbtPrm);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
    @Test
    public void testRandomSampling() {
        DepthStratifiedBnbSamplerPrm prm = new DepthStratifiedBnbSamplerPrm();
        prm.maxDepth = 10;
        prm.maxSamples = 3;
        BnBDmvTrainerPrm bnbtPrm = new BnBDmvTrainerPrm();
        bnbtPrm.bnbSolverFactory = prm;
        bnbtPrm.brancher = getRandomBrancher();
        bnbtPrm.relaxFactory = new DmvRltRelaxPrm();
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
