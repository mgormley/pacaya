package edu.jhu.train;

import junit.framework.Assert;


import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.SentenceCollection;
import edu.jhu.ilp.IlpSolverFactory;
import edu.jhu.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.model.dmv.DmvMStep;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.parse.IlpFormulation;
import edu.jhu.parse.IlpDepSentenceParser;
import edu.jhu.parse.DepParser;
import edu.jhu.parse.dmv.DmvCkyParser;
import edu.jhu.train.DmvViterbiEMTrainer.DmvViterbiEMTrainerPrm;
import edu.jhu.util.Prng;


public class ViterbiTrainerTest {

    static {
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }
    
    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testLogLikelihood() {
        double lambda = 0.1;
        DepParser parser = new DmvCkyParser();
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        DmvViterbiEMTrainerPrm prm = new DmvViterbiEMTrainerPrm();
        prm.parser = parser;
        prm.modelFactory = modelFactory;
        prm.emPrm.iterations = 25;
        prm.emPrm.convergenceRatio = 0.99999;
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(prm);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));

        Assert.assertEquals(-24.952, trainer.getLogLikelihood(), 1e-3);
    }
    
    @Test
    public void testConvergence() {
        double lambda = 0.1;
        IlpSolverFactory ilpSolverFactory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        DepParser parser = new IlpDepSentenceParser(IlpFormulation.FLOW_NONPROJ, ilpSolverFactory);
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        DmvViterbiEMTrainerPrm prm = new DmvViterbiEMTrainerPrm();
        prm.parser = parser;
        prm.modelFactory = modelFactory;
        prm.emPrm.iterations = 25;
        prm.emPrm.convergenceRatio = 0.99999;
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(prm);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        trainer.train(new DmvTrainCorpus(sentences));
        
        Assert.assertEquals(2, trainer.getIterationsCompleted());
    }    

    @Test
    public void testRestarts() {
        DmvViterbiEMTrainer trainer = getDefaultCkyViterbiTrainer();
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        sentences.addSentenceFromString("the hat");
        trainer.train(new DmvTrainCorpus(sentences));
        
        System.out.println("logLikelihood: " + trainer.getLogLikelihood());
        Assert.assertEquals(-2.517, trainer.getLogLikelihood(), 1e-3);
    }

    public static DmvViterbiEMTrainer getDefaultCkyViterbiTrainer() {
        double lambda = 0.1;
        DepParser parser = new DmvCkyParser();
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        DmvViterbiEMTrainerPrm prm = new DmvViterbiEMTrainerPrm(5, 0.99999, 9, 5, lambda, null, parser, modelFactory);
        DmvViterbiEMTrainer trainer = new DmvViterbiEMTrainer(prm);
        return trainer;
    }
    
}
