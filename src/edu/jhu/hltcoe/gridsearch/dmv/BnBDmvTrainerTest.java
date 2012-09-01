package edu.jhu.hltcoe.gridsearch.dmv;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvDantzigWolfeRelaxation.CutCountComputer;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
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
        BnBDmvTrainer trainer = new BnBDmvTrainer(epsilon, getDefaultBrancher(), dwRelax, 5);
        
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        sentences.addSentenceFromString("the hat");
        trainer.train(sentences);
    }
    
    //@Test
    public void testOne() {
        double epsilon = 0.5;
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(null, 100, new CutCountComputer());
        BnBDmvTrainer trainer = new BnBDmvTrainer(epsilon, getDefaultBrancher(), dwRelax, 5);
        //trainer.setTempDir(new File("."));

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the hat with the mouse ate by the cat");
        trainer.train(sentences);
    }
    
    //@Test
    public void testSynthetic() {
        double epsilon = 0.9;
        DmvDantzigWolfeRelaxation dwRelax = new DmvDantzigWolfeRelaxation(null, 1, new CutCountComputer());
        dwRelax.setMaxDwIterations(3);
        BnBDmvTrainer trainer = new BnBDmvTrainer(epsilon, getDefaultBrancher(), dwRelax, 5);
        //trainer.setTempDir(new File("."));

        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(10);
        System.out.println(treebank);
        System.out.println(dmvModel);
        SentenceCollection sentences = treebank.getSentences();
        
        trainer.train(sentences);
    }


    private DmvBoundsDeltaFactory getDefaultBrancher() {
        return new RegretDmvBoundsDeltaFactory();
    }
    
}
