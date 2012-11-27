package edu.jhu.hltcoe.gridsearch.dmv;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.BfsComparator;
import edu.jhu.hltcoe.gridsearch.DmvLazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver;
import edu.jhu.hltcoe.gridsearch.PqNodeOrderer;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.RegretVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.LpSumToOneBuilder.CutCountComputer;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;


public class BnBDmvTrainerTest {

    static {
        BasicConfigurator.configure();
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
        DmvProblemNode.clearActiveNode();
    }
    
    @Test
    public void testTwo() {
        double epsilon = 0.4;
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(null, 100, new CutCountComputer());
        BnBDmvTrainer trainer = getDefaultBnb(epsilon, dwRelax);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        sentences.addSentenceFromString("the hat");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
    //@Test
    public void testOne() {
        double epsilon = 0.5;
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(null, 100, new CutCountComputer());
        BnBDmvTrainer trainer = getDefaultBnb(epsilon, dwRelax);

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(new DmvTrainCorpus(sentences));
    }
    
    //@Test
    public void testSynthetic() {
        double epsilon = 0.9;
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(null, 1, new CutCountComputer());
        dwRelax.setMaxDwIterations(3);
        BnBDmvTrainer trainer = getDefaultBnb(epsilon, dwRelax);

        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(10);
        System.out.println(treebank);
        System.out.println(dmvModel);
        SentenceCollection sentences = treebank.getSentences();
        
        trainer.train(new DmvTrainCorpus(sentences));
    }

    private BnBDmvTrainer getDefaultBnb(double epsilon, DmvDantzigWolfeRelaxation dwRelax) {
        LazyBranchAndBoundSolver bnbSolver = new DmvLazyBranchAndBoundSolver(epsilon, new PqNodeOrderer(new BfsComparator()), 5, null);
        BnBDmvTrainer trainer = new BnBDmvTrainer(bnbSolver, getDefaultBrancher(), dwRelax);
        return trainer;
    }
    
    public static CptBoundsDeltaFactory getDefaultBrancher() {
        VariableSelector varSelector = new RegretVariableSelector();
        VariableSplitter varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_PROB);
        return new BasicCptBoundsDeltaFactory(varSelector, varSplitter);
    }
    
}
