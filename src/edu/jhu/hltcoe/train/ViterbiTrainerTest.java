package edu.jhu.hltcoe.train;

import junit.framework.Assert;

import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.dmv.DmvMStep;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParserTest;
import edu.jhu.hltcoe.parse.IlpViterbiSentenceParser;
import edu.jhu.hltcoe.parse.ViterbiParser;


public class ViterbiTrainerTest {

    @Test
    public void testConvergence() {
        double lambda = 0.1;
        int iterations = 25;
        IlpSolverFactory ilpSolverFactory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        ViterbiParser parser = new IlpViterbiSentenceParser(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory);
        DmvMStep mStep = new DmvMStep(lambda);
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        ViterbiTrainer trainer = new ViterbiTrainer(parser, mStep, modelFactory, iterations, 0.99999);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the hat with the mouse"));
        trainer.train(sentences);
        
        Assert.assertEquals(2, trainer.getIterationsCompleted());
    }
    
}
