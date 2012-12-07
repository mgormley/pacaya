package edu.jhu.hltcoe.gridsearch;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.DepthStratifiedBnbNodeSampler.DepthStratifiedBnbSamplerPrm;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainerTest;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation.DmvRltRelaxPrm;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;


public class DepthStratifiedBnbNodeSamplerTest {

    @Before
    public void setUp() {
        DmvProblemNode.clearActiveNode();
        //Prng.seed(1234567890);
    }
    
    @Test
    public void testRegretSampling() {
        DepthStratifiedBnbSamplerPrm prm = new DepthStratifiedBnbSamplerPrm();
        prm.maxDepth = 10;
        DepthStratifiedBnbNodeSampler sampler = new DepthStratifiedBnbNodeSampler(prm, 3, null);
        DmvRltRelaxPrm rrPrm = new DmvRltRelaxPrm();
        DmvRltRelaxation relax = new DmvRltRelaxation(rrPrm);
        BnBDmvTrainer trainer = new BnBDmvTrainer(sampler, BnBDmvTrainerTest.getDefaultBrancher(), relax);

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
    @Test
    public void testRandomSampling() {
        DepthStratifiedBnbSamplerPrm prm = new DepthStratifiedBnbSamplerPrm();
        prm.maxDepth = 10;
        DepthStratifiedBnbNodeSampler sampler = new DepthStratifiedBnbNodeSampler(prm, 3, null);
        DmvRltRelaxPrm rrPrm = new DmvRltRelaxPrm();
        DmvRltRelaxation relax = new DmvRltRelaxation(rrPrm);
        BnBDmvTrainer trainer = new BnBDmvTrainer(sampler, getRandomBrancher(), relax);

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
