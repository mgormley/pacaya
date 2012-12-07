package edu.jhu.hltcoe.gridsearch;

import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainer;
import edu.jhu.hltcoe.gridsearch.dmv.BnBDmvTrainerTest;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation.DmvRltRelaxPrm;
import edu.jhu.hltcoe.train.DmvTrainCorpus;


public class DepthStratifiedBnbNodeSamplerTest {

    @Test
    public void testSampling() {
        DepthStratifiedBnbNodeSampler sampler = new DepthStratifiedBnbNodeSampler(10, 5, null);
        DmvRltRelaxPrm prm = new DmvRltRelaxPrm();
        DmvRltRelaxation relax = new DmvRltRelaxation(prm);
        BnBDmvTrainer trainer = new BnBDmvTrainer(sampler, BnBDmvTrainerTest.getDefaultBrancher(), relax);

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
}
