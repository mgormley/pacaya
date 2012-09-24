package edu.jhu.hltcoe.parse;

import static edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel.TW_A;
import static edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel.TW_B;
import static edu.jhu.hltcoe.parse.IlpViterbiParserTest.getIlpParses;
import static org.junit.Assert.assertArrayEquals;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Pair;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;


public class DmvCkyParserTest {

    static {
        BasicConfigurator.configure();
    }
    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(0.1));
        Set<Label> vocab = new HashSet<Label>();
        vocab.add(WallDepTreeNode.WALL_LABEL);
        vocab.add(TW_A);
        vocab.add(TW_B);
        DmvModel dmvModel = (DmvModel) modelFactory.getInstance(vocab);

        dmvModel.setAllChooseWeights(0.5);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "l", TW_A, 0.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "l", TW_B, 1.0);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", TW_A, 0.5);
        dmvModel.putChooseWeight(WallDepTreeNode.WALL_LABEL, "r", TW_B, 0.5);
//        dmvModel.putChooseWeight(TW_A, "l", TW_A, 0.4);
//        dmvModel.putChooseWeight(TW_A, "l", TW_B, 0.6);
        dmvModel.putChooseWeight(TW_A, "r", TW_A, 1.0);
        dmvModel.putChooseWeight(TW_A, "r", TW_B, 0.0); // dummy param
//        dmvModel.putChooseWeight(TW_B, "l", TW_A, 0.5);
//        dmvModel.putChooseWeight(TW_B, "l", TW_B, 0.5);
//        dmvModel.putChooseWeight(TW_B, "r", TW_A, 0.0);
//        dmvModel.putChooseWeight(TW_B, "r", TW_B, 1.0);
        
        dmvModel.setAllStopWeights(0.5);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "l", true, 1.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "l", false, 1.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "r", true, 0.0);
        dmvModel.putStopWeight(WallDepTreeNode.WALL_LABEL, "r", false, 1.0);
//        dmvModel.putStopWeight(TW_A, "l", true, 0.6);
//        dmvModel.putStopWeight(TW_A, "r", true, 0.6); 
//        dmvModel.putStopWeight(TW_B, "l", true, 0.6); 
//        dmvModel.putStopWeight(TW_B, "r", true, 0.6);
        
        return dmvModel;
    }
    
    @Test
    public void testTwoPosTagCase() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("a/A b/B");
        Model model = getTwoPosTagInstance();
        double expectedParseWeight = Utilities.log(0.0078125); // 0.5^7
        
        // dynamic programming parsing
        DepTreebank dpTrees = getDpParses(model, sentences, expectedParseWeight);
        // flow projective parsing (only for comparison)
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        
        for (int i=0; i<flowTrees.size(); i++) {
            assertArrayEquals(flowTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testProjParses() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences.getVocab());
        double expectedParseWeight = -33.0063;

        // dynamic programming parsing
        DepTreebank dpTrees = getDpParses(model, sentences, expectedParseWeight);
        // flow projective parsing (only for comparison)
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        
        for (int i=0; i<flowTrees.size(); i++) {
            assertArrayEquals(flowTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }

    @Test
    public void testFirstSentenceFromWsj() {
        SentenceCollection sentences = new SentenceCollection();
        // Below is the full sentence, but the DP_PROJ is too slow to parse it. Instead we use
        // just a part of it.
        //sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD .");
        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT");
        DmvModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences.getVocab());
        double expectedParseWeight = -50.3989;
        
        // dynamic programming parsing
        DepTreebank dpTrees = getDpParses(model, sentences, expectedParseWeight);
        // flow projective parsing (only for comparison)
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        
        for (int i=0; i<flowTrees.size(); i++) {
            assertArrayEquals(flowTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testSemiSupervisedOnSynthetic() {
        DmvModel dmvModel = SimpleStaticDmvModel.getThreePosTagInstance();
        DmvTrainCorpus trainCorpus = getSyntheticCorpus(dmvModel); 
        
        double expectedParseWeight = -17.704;
        getDpParses(dmvModel, trainCorpus, expectedParseWeight);
    }

    public static DmvTrainCorpus getSyntheticCorpus(DmvModel dmvModel) {
        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(10);
        DmvTrainCorpus trainCorpus = new DmvTrainCorpus(treebank, 0.5);
        return trainCorpus;
    }
    
    private static DepTreebank getDpParses(Model model, SentenceCollection sentences, double expectedParseWeight) {
        DmvCkyParser parser = new DmvCkyParser();
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        System.out.println("prob: " + Utilities.exp(parser.getLastParseWeight()));
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }    
    
    private static DepTreebank getDpParses(Model model, DmvTrainCorpus corpus, double expectedParseWeight) {
        DmvCkyParser parser = new DmvCkyParser();
        DepTreebank trees = parser.getViterbiParse(corpus, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        System.out.println("prob: " + Utilities.exp(parser.getLastParseWeight()));
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        
        // Check that the supervised trees are the same as the original.
        for (int s=0; s<corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                Assert.assertEquals(corpus.getTree(s), trees.get(s));
            }
        }
        
        return trees;
    }
    
}
