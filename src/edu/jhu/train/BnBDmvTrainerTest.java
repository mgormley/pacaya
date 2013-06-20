package edu.jhu.hltcoe.train;

import junit.framework.Assert;


import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.LazyBnbSolverPrm;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.SearchStatus;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.RegretVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.DmvDwRelaxPrm;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.DmvRelaxationFactory;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.train.BnBDmvTrainer.BnBDmvTrainerPrm;
import edu.jhu.hltcoe.util.Prng;


public class BnBDmvTrainerTest {

    static {
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testTwo() {
        double epsilon = 0.1;
        DmvDwRelaxPrm prm = new DmvDwRelaxPrm();
        prm.maxCutRounds = 100;
        BnBDmvTrainer trainer = getDefaultBnb(epsilon, prm);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        sentences.addSentenceFromString("the hat");
        trainer.init(new DmvTrainCorpus(sentences));
        SearchStatus status = trainer.train();
        Assert.assertEquals(SearchStatus.OPTIMAL_SOLUTION_FOUND, status);
    }
    
    //DISABLED: @Test
    public void testOne() {
        double epsilon = 0.5;
        DmvDwRelaxPrm prm = new DmvDwRelaxPrm();
        prm.maxCutRounds = 100;
        BnBDmvTrainer trainer = getDefaultBnb(epsilon, prm);

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
    //DISABLED: @Test
    public void testSynthetic() {
        double epsilon = 0.9;
        DmvDwRelaxPrm prm = new DmvDwRelaxPrm();
        prm.maxDwIterations = 3;
        BnBDmvTrainer trainer = getDefaultBnb(epsilon, prm);

        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(10);
        System.out.println(treebank);
        System.out.println(dmvModel);
        SentenceCollection sentences = treebank.getSentences();
        
        trainer.train(new DmvTrainCorpus(sentences));
    }

    private BnBDmvTrainer getDefaultBnb(double epsilon, DmvRelaxationFactory relaxFactory) {
        LazyBnbSolverPrm bnbPrm = new LazyBnbSolverPrm();
        bnbPrm.epsilon = epsilon;
        bnbPrm.timeoutSeconds = 5;
        BnBDmvTrainerPrm prm = new BnBDmvTrainerPrm();
        prm.bnbSolverFactory = bnbPrm;
        prm.relaxFactory = relaxFactory;
        return new BnBDmvTrainer(prm);
    }
    
    public static CptBoundsDeltaFactory getDefaultBrancher() {
        VariableSelector varSelector = new RegretVariableSelector();
        VariableSplitter varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_PROB);
        return new BasicCptBoundsDeltaFactory(varSelector, varSplitter);
    }
    
}
