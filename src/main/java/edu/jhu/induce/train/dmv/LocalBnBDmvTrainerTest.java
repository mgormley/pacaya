package edu.jhu.induce.train.dmv;


import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.SentenceCollection;
import edu.jhu.globalopt.LazyBranchAndBoundSolver.LazyBnbSolverPrm;
import edu.jhu.induce.train.dmv.LocalBnBDmvTrainer.LocalBnBDmvTrainerPrm;
import edu.jhu.util.Prng;

public class LocalBnBDmvTrainerTest {

    static {
        // Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }

    @Test
    public void testTwo() {
        LazyBnbSolverPrm bnbPrm = new LazyBnbSolverPrm();
        bnbPrm.epsilon = 0.4;
        bnbPrm.timeoutSeconds = 2;
        
        LocalBnBDmvTrainerPrm prm = new LocalBnBDmvTrainerPrm();
        prm.numRestarts = 1;
        prm.offsetProb = 0.1;
        prm.probOfSkipCm = 0.1;
        prm.bnbSolverFactory = bnbPrm;
        prm.timeoutSeconds = 10;
        prm.viterbiTrainer = DmvViterbiEMTrainerTest.getDefaultCkyViterbiTrainer();
        LocalBnBDmvTrainer trainer = new LocalBnBDmvTrainer(prm);

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        sentences.addSentenceFromString("the hat");
        trainer.train(new DmvTrainCorpus(sentences));
    }

}
