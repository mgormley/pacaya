package edu.jhu.hltcoe.parse;

import static edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel.TW_A;
import static edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel.TW_B;
import static edu.jhu.hltcoe.parse.IlpViterbiParserTest.getIlpParses;
import static org.junit.Assert.assertArrayEquals;

import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;


import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.util.Alphabet;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.eval.DependencyParserEvaluator;
import edu.jhu.hltcoe.gridsearch.dmv.DmvObjective;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvDepTreeGenerator;
import edu.jhu.hltcoe.model.dmv.DmvModel;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.SimpleStaticDmvModel;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;


public class DmvCkyParserTest {

    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }

    public static DmvModel getTwoPosTagInstance() {
        DmvModelFactory modelFactory = new RandomDmvModelFactory(0.1);
        Alphabet<Label> alphabet = new Alphabet<Label>();
        alphabet.lookupIndex(TW_A);
        alphabet.lookupIndex(TW_B);
        DmvModel dmvModel = modelFactory.getInstance(alphabet);
        
        dmvModel.fill(0.5);
        dmvModel.putRootWeight(TW_A, 0.5);
        dmvModel.putRootWeight(TW_B, 0.5);
//        dmvModel.putChooseWeight(TW_A, "l", TW_A, 0.4);
//        dmvModel.putChooseWeight(TW_A, "l", TW_B, 0.6);
        dmvModel.putChildWeight(TW_A, "r", TW_A, 1.0);
        dmvModel.putChildWeight(TW_A, "r", TW_B, 0.0); // dummy param
//        dmvModel.putChooseWeight(TW_B, "l", TW_A, 0.5);
//        dmvModel.putChooseWeight(TW_B, "l", TW_B, 0.5);
//        dmvModel.putChooseWeight(TW_B, "r", TW_A, 0.0);
//        dmvModel.putChooseWeight(TW_B, "r", TW_B, 1.0);
        
//        dmvModel.putStopWeight(TW_A, "l", true, 0.6);
//        dmvModel.putStopWeight(TW_A, "r", true, 0.6); 
//        dmvModel.putStopWeight(TW_B, "l", true, 0.6); 
//        dmvModel.putStopWeight(TW_B, "r", true, 0.6);
        
        dmvModel.convertRealToLog();
        dmvModel.assertNormalized(1e-13);
        return dmvModel;
    }
    
    @Test
    public void testTwoPosTagCase() {
        DmvModel model = getTwoPosTagInstance();
        SentenceCollection sentences = new SentenceCollection(model.getTagAlphabet());
        sentences.addSentenceFromString("a/A b/B");
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
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedParseWeight = -27.274;

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
        sentences.addSentenceFromString("NNP , CD NNS JJ , MD VB DT NN IN DT");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedParseWeight = -49.376;
        
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
        // Generate trees from a gold model.
        DmvModel goldModel = SimpleStaticDmvModel.getThreePosTagInstance();
        System.out.println(goldModel.toString());

        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(goldModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(100);
        DependencyParserEvaluator eval = new DependencyParserEvaluator(new DmvCkyParser(), treebank, "test");

        // Parse with a random model.
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model dmvModel = modelFactory.getInstance(goldModel.getTagAlphabet());
        
        DmvTrainCorpus trainCorpus = new DmvTrainCorpus(treebank, 1.0);
        System.out.println(trainCorpus);
        double expectedParseWeight = -1574.426;
        DepTreebank parses = getDpParses(dmvModel, trainCorpus, expectedParseWeight);
        Assert.assertEquals(1.0, eval.evaluate(parses));
                        
        trainCorpus = new DmvTrainCorpus(treebank, 0.8);
        expectedParseWeight = -1538.890;
        parses = getDpParses(dmvModel, trainCorpus, expectedParseWeight);
        Assert.assertEquals(0.871, eval.evaluate(parses), 1e-3);

        trainCorpus = new DmvTrainCorpus(treebank, 0.4);
        expectedParseWeight = -1442.570;
        parses = getDpParses(dmvModel, trainCorpus, expectedParseWeight);
        Assert.assertEquals(0.511, eval.evaluate(parses), 1e-3);
    }

    public static DmvTrainCorpus getDefaultSemiSupervisedSyntheticCorpus(DmvModel dmvModel) {
        return getSemiSupervisedSyntheticCorpus(dmvModel, 0.9);
    }
    
    public static DmvTrainCorpus getSemiSupervisedSyntheticCorpus(DmvModel dmvModel, double propSupervised) {
        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(100);
        DmvTrainCorpus trainCorpus = new DmvTrainCorpus(treebank, propSupervised);
        System.out.println("Fully supervised logProb: " + (new DmvObjective(trainCorpus)).computeTrueObjective(dmvModel, treebank));
        return trainCorpus;
    }
    
    private static DepTreebank getDpParses(Model model, SentenceCollection sentences, double expectedParseWeight) {
        DmvCkyParser parser = new DmvCkyParser();
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }    
    
    private static DepTreebank getDpParses(Model model, DmvTrainCorpus corpus, double expectedParseWeight) {
        DmvCkyParser parser = new DmvCkyParser();
        DepTreebank trees = parser.getViterbiParse(corpus, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        System.out.println("logProb: " + parser.getLastParseWeight());
        
        // Check that the supervised trees are the same as the original.
        for (int s=0; s<corpus.size(); s++) {
            if (corpus.isLabeled(s)) {
                Assert.assertEquals(corpus.getTree(s), trees.get(s));
            }
        }

        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        
        return trees;
    }
    
}
