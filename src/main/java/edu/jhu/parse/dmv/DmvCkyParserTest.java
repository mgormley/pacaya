package edu.jhu.parse.dmv;

import static edu.jhu.model.dmv.SimpleStaticDmvModel.TW_A;
import static edu.jhu.model.dmv.SimpleStaticDmvModel.TW_B;
import static edu.jhu.parse.IlpDepParserTest.getIlpParses;
import static org.junit.Assert.assertArrayEquals;

import java.util.HashMap;
import java.util.Map;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.Label;
import edu.jhu.data.SentenceCollection;
import edu.jhu.eval.DependencyParserEvaluator;
import edu.jhu.gridsearch.dmv.DmvObjective;
import edu.jhu.gridsearch.dmv.DmvObjective.DmvObjectivePrm;
import edu.jhu.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.model.Model;
import edu.jhu.model.dmv.DmvDepTreeGenerator;
import edu.jhu.model.dmv.DmvModel;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.model.dmv.SimpleStaticDmvModel;
import edu.jhu.model.dmv.UniformDmvModelFactory;
import edu.jhu.parse.IlpFormulation;
import edu.jhu.parse.cky.chart.Chart.ChartCellType;
import edu.jhu.parse.dep.ProjectiveDependencyParser;
import edu.jhu.parse.dmv.DmvCkyParser.DmvCkyParserPrm;
import edu.jhu.train.DmvTrainCorpus;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;
import edu.jhu.util.collections.Maps;
import edu.jhu.util.math.FastMath;


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
        dmvModel.assertLogNormalized(1e-13);
        return dmvModel;
    }
    
    @Test
    public void testTwoPosTagCase() {
        DmvModel model = getTwoPosTagInstance();
        SentenceCollection sentences = new SentenceCollection(model.getTagAlphabet());
        sentences.addSentenceFromString("a/A b/B");
        double expectedParseWeight = FastMath.log(0.0078125); // 0.5^7
        
        // dynamic programming parsing
        DepTreebank dpTrees = getDpParses(model, sentences, expectedParseWeight);
        // flow projective parsing (only for comparison)
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        
        for (int i=0; i<flowTrees.size(); i++) {
            assertArrayEquals(flowTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }

    @Test
    public void testLongerTwoPosTagCase() {
        DmvModel model = getTwoPosTagInstance();
        SentenceCollection sentences = new SentenceCollection(model.getTagAlphabet());
        sentences.addSentenceFromString("a/A b/B a/A");
        double expectedParseWeight = FastMath.log(Math.pow(0.5, 10)); // 0.5^7
        
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

    @Test
    public void testTieBreakingOnUniformModel() {
        Prng.seed(System.currentTimeMillis());
        
        SentenceCollection sentences = new SentenceCollection();
        //sentences.addSentenceFromString("the cat ate the hat with the mouse");
        sentences.addSentenceFromString("the cat ate");
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        DmvModel model = modelFactory.getInstance(sentences.getLabelAlphabet());
        
        // Compute the expected log-likelihood as follows:
        //
        // 0.5^(n*2 stops, n-1 continues) * (1/3)^(n arcs)
        //
        // Note: we have n-1 continues since the root always only generates a single child.
        int n = sentences.get(0).size();
        int m = model.root.length;
        double expectedParseWeight = FastMath.log(Math.pow(0.5, n * 2 + n-1) * Math.pow(1./m, n));

        System.out.println("Expected log likelihood: " + expectedParseWeight);
        
        // Parse num restarts times and print out the results.
        int numRestarts = 5000;
        Map<ParentsArray,Integer> counter = new HashMap<ParentsArray,Integer>();
        for (int i=0; i<numRestarts; i++) {
            DepTreebank dpTrees = getDpParses(model, sentences, expectedParseWeight);
            ParentsArray pa = new ParentsArray(dpTrees.get(0).getParents());
            System.out.println(Arrays.toString(pa.parents));
            Maps.increment(counter, pa, 1);
        }
        for (ParentsArray pa : counter.keySet()) {
            System.out.printf("%5d %s\n", counter.get(pa), pa);
        }
    }
    

    /**
     * Output: WITH ChartCellType.FULL_BREAK_TIES
     * Average seconds per sentence: 0.09532673267326733
     * Total time: 9130.0
     * Sentences per second: 10.952902519167578
     * 
     * WITH ChartCellType.FULL
     * Average seconds per sentence: 0.06728712871287129 
     * Total time: 6338.0
     * Sentences per second: 15.77784790154623
     * 
     * WITH ChartCellType.SINGLE_HASH_BREAK_TIES  <------ Length 30 sentences.
     * Average seconds per sentence: 0.03812871287128713
     * Total time: 3223.0
     * Sentences per second: 31.02699348433137
     * 
     * WITH ChartCellType.SINGLE_HASH_BREAK_TIES <------ Length 50 sentences.
     * Average seconds per sentence: 0.22203960396039604
     * Total time: 21814.0
     * Sentences per second: 4.584211973961676
     */
    @Test
    public void testSpeed() {
        int trials = 100;
        int n = 30;
        String sent = "";
        for (int i=0; i<n; i++) {
            sent += " " + i;
        }

        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString(sent); //"NNP , CD NNS JJ , MD VB DT NN IN DT NNP , CD NNS JJ , MD VB DT NN IN DT");
        DmvModelFactory modelFactory = new UniformDmvModelFactory(); //new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        DmvCkyParserPrm prm = new DmvCkyParserPrm();
        prm.ckyPrm.cellType = ChartCellType.SINGLE_HASH_BREAK_TIES;
        DmvCkyParser parser = new DmvCkyParser(prm);
        parser.getViterbiParse(sentences, model);

        Timer timer = new Timer();
        timer.start();
        for (int t=0; t<trials; t++) {
            parser.getViterbiParse(sentences, model);
        }
        timer.stop();
        System.out.println("Total time: " + timer.totMs());
        int numSents = trials;
        System.out.println("Sentences per second: " + numSents / timer.totSec());
    }
    
    public static class ParentsArray {
        private int[] parents;
        public ParentsArray(int[] parents) {
            if (parents == null) {
                throw new IllegalStateException();
            }
            this.parents = parents;
        }
        @Override
        public int hashCode() {
            return java.util.Arrays.hashCode(parents);
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ParentsArray other = (ParentsArray) obj;
            if (!java.util.Arrays.equals(parents, other.parents))
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "PA: " + Arrays.toString(parents);
        }
    }
    
    public static DmvTrainCorpus getDefaultSemiSupervisedSyntheticCorpus(DmvModel dmvModel) {
        return getSemiSupervisedSyntheticCorpus(dmvModel, 0.9);
    }
    
    public static DmvTrainCorpus getSemiSupervisedSyntheticCorpus(DmvModel dmvModel, double propSupervised) {
        DmvDepTreeGenerator generator = new DmvDepTreeGenerator(dmvModel, Prng.nextInt(1000000));
        DepTreebank treebank = generator.getTreebank(100);
        DmvTrainCorpus trainCorpus = new DmvTrainCorpus(treebank, propSupervised);
        System.out.println("Fully supervised logProb: " + (new DmvObjective(new DmvObjectivePrm(), new IndexedDmvModel(trainCorpus))).computeTrueObjective(dmvModel, treebank));
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
