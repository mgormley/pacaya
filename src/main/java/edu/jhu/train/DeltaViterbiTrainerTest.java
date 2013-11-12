package edu.jhu.train;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.SentenceCollection;
import edu.jhu.ilp.IlpSolverFactory;
import edu.jhu.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.parse.DeltaGenerator;
import edu.jhu.parse.DepParser;
import edu.jhu.parse.FixedIntervalDeltaGenerator;
import edu.jhu.parse.IlpDepParserWithDeltas;
import edu.jhu.parse.IlpDepSentenceParser;
import edu.jhu.parse.IlpFormulation;
import edu.jhu.util.Prng;

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
        DepParser fastParser = new IlpDepSentenceParser(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory);
        DeltaGenerator deltaGen = new FixedIntervalDeltaGenerator(0.1, 3);
        DepParser deltaParser = new IlpDepParserWithDeltas(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory,
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
