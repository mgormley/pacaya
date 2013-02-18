package edu.jhu.hltcoe.train;

import junit.framework.Assert;


import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.parse.DeltaGenerator;
import edu.jhu.hltcoe.parse.FixedIntervalDeltaGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.IlpViterbiSentenceParser;
import edu.jhu.hltcoe.parse.ViterbiParser;
import edu.jhu.hltcoe.util.Prng;

public class DeltaViterbiTrainerTest {


    @Before
    public void setUp() {
        Prng.seed(12345678);
    }
    
    @Test
    public void testConvergence() {
        double lambda = 0.01;
        int iterations = 25;
        double convergenceRatio = 0.99999;

        IlpSolverFactory ilpSolverFactory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        ViterbiParser fastParser = new IlpViterbiSentenceParser(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory);
        DeltaGenerator deltaGen = new FixedIntervalDeltaGenerator(0.1, 3);
        ViterbiParser deltaParser = new IlpViterbiParserWithDeltas(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory,
                deltaGen);
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        DeltaViterbiTrainer trainer = new DeltaViterbiTrainer(deltaParser, fastParser, modelFactory, iterations,
                convergenceRatio, lambda);

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the man ate the pizza with a fork");
        sentences.addSentenceFromString("the man ate the pizza");
        sentences.addSentenceFromString("the man ate with a fork the pizza");
        sentences.addSentenceFromString("with a fork the man ate");
        trainer.train(new DmvTrainCorpus(sentences));

        Assert.assertEquals(3, trainer.getIterationsCompleted());
    }

}
