package edu.jhu.hltcoe.parse.relax;


import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.Tag;
import edu.jhu.hltcoe.data.WallDepTreeNode;
import edu.jhu.hltcoe.gridsearch.dmv.BasicDmvProjector;
import edu.jhu.hltcoe.gridsearch.dmv.BasicDmvProjector.DmvProjectorPrm;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.gridsearch.dmv.ShinyEdges;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.model.dmv.UniformDmvModelFactory;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpDepParserTest;
import edu.jhu.hltcoe.parse.relax.LpDmvRelaxedParser.LpDmvRelaxedParserPrm;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Alphabet;
import edu.jhu.hltcoe.util.JUnitUtils;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.math.Vectors;

public class LpDmvRelaxedParserTest {

    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testTinyProj2() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        double expectedObj = -4.593;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpDepParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }

    @Test
    public void testTinyProj3Uniform() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate");
        DmvModelFactory modelFactory = new UniformDmvModelFactory();
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        double expectedObj = -8.841;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpDepParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
    }
    
    @Test
    public void testTinyProj3Random() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        double expectedObj = -7.802;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpDepParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }
    
    @Test
    public void testCplexLpParserNonProj() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedObj = -26.467;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpDepParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
        //expectedObj = -26.525; // TODO: this shouldn't be different!
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }    

    @Test
    public void testCplexLpParserProj() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedObj = -26.525;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpDepParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }
    
    @Test
    public void testCplexLpParserMultipleSentences() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("cat ate hat");
        sentences.addSentenceFromString("mouse cat ate");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedObj = -26.664;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpDepParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }
    
    @Test
    public void testFeatCountObjective() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("cat ate hat");
        sentences.addSentenceFromString("mouse cat ate");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedObj = -26.664;

        // Single commodity flow non-projective parsing LP Relaxation -- standard objective.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        // Single commodity flow non-projective parsing LP Relaxation -- feature count objective.
        RelaxedDepTreebank trees2 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX_FCOBJ, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }
    
    @Test
    public void testUniversalPosteriorConstraints() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the/NOUN man/NOUN ran/VERB");
        sentences.addSentenceFromString("cat/NOUN ate/VERB mouse/NOUN");
        sentences.addSentenceFromString("very/ADV red/ADJ sweaters/NOUN shine/VERB");        
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        DepTreebank projTrees;
        projTrees = getPostConsTrees(sentences, model, false, 1.0);
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1}, projTrees.get(0).getParents());
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1}, projTrees.get(1).getParents());
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1, 2}, projTrees.get(2).getParents());

        projTrees = getPostConsTrees(sentences, model, true, 0.5);
        // There are two non-shiny arcs here:
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 1}, projTrees.get(0).getParents());
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, projTrees.get(1).getParents());
        // There are three non-shiny arcs here:
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 3, 1}, projTrees.get(2).getParents());
        
        projTrees = getPostConsTrees(sentences, model, true, 0.7);
        JUnitUtils.assertArrayEquals(new int[]{2, 0, -1}, projTrees.get(0).getParents());
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, projTrees.get(1).getParents());
        // There are three non-shiny arcs here:
        JUnitUtils.assertArrayEquals(new int[]{-1, 0, 3, 1}, projTrees.get(2).getParents());

        projTrees = getPostConsTrees(sentences, model, true, 1.0);
        JUnitUtils.assertArrayEquals(new int[]{2, 0, -1}, projTrees.get(0).getParents());
        JUnitUtils.assertArrayEquals(new int[]{1, -1, 1}, projTrees.get(1).getParents());
        JUnitUtils.assertArrayEquals(new int[]{1, 2, 3, -1}, projTrees.get(2).getParents());
    }

    public static DepTreebank getPostConsTrees(SentenceCollection sentences, Model model, boolean univeralPostCons, double universalMinProp) {
        LpDmvRelaxedParserPrm prm = new LpDmvRelaxedParserPrm();
        prm.parsePrm.formulation = IlpFormulation.FLOW_PROJ_LPRELAX;
        prm.parsePrm.universalPostCons = univeralPostCons;
        prm.parsePrm.universalMinProp = universalMinProp;
        System.out.println(sentences.getLabelAlphabet());
        prm.parsePrm.shinyEdges = getUniversalSet(sentences.getLabelAlphabet());
        prm.tempDir = new File(".");

        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(prm);
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        RelaxedDepTreebank trees = parser.getRelaxedParse(corpus, model);
        checkFractionalTrees(trees);
        
        BasicDmvProjector projector = new BasicDmvProjector(new DmvProjectorPrm(), corpus);
        DepTreebank projTrees = projector.getProjectedParses(trees);
        for (DepTree tree : projTrees) {
            System.out.println(tree);
        }
        return projTrees;
    }
    

    public static ShinyEdges getUniversalSet(Alphabet<Label> alphabet) {
        ShinyEdges edges = new ShinyEdges(alphabet);
        
        Label root = WallDepTreeNode.WALL_LABEL;
        Label verb = new Tag("VERB");
        Label noun = new Tag("NOUN");
        Label adj = new Tag("ADJ");
        Label adv = new Tag("ADV");
        Label adp = new Tag("ADP");
        
        // Root
        edges.addShinyEdge(root, verb);
        // Verb
        edges.addShinyEdge(verb, noun);
        edges.addShinyEdge(verb, verb);
        // Noun
        edges.addShinyEdge(noun, adj);
        edges.addShinyEdge(noun, noun);
        // Adp
        edges.addShinyEdge(adp, noun);
        // Adj
        edges.addShinyEdge(adj, adv);
        
        return edges;
    }
    
    public static RelaxedDepTreebank getLpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        LpDmvRelaxedParserPrm prm = new LpDmvRelaxedParserPrm();
        prm.parsePrm.formulation = IlpFormulation.FLOW_PROJ_LPRELAX;
        prm.tempDir = new File(".");
        prm.parsePrm.inclExtraCons = false;
        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(prm);
        RelaxedDepTreebank trees = parser.getRelaxedParse(new DmvTrainCorpus(sentences), model);
        checkFractionalTrees(trees);
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }

    public static void checkFractionalTrees(RelaxedDepTreebank trees) {
        // Check the parents of the nodes.
        for (int s=0; s<trees.size(); s++) {
            double[] fracRoots = trees.getFracRoots()[s];
            double[][] fracChildren = trees.getFracChildren()[s];
            // Check that the wall has "one" child.
            Assert.assertEquals(1.0, Vectors.sum(fracRoots), 1e-13);
            // Check that each node has "one" parent.
            int sentLen = fracChildren.length;
            for (int c = 0; c<sentLen; c++) {
                double sum = fracRoots[c];
                for (int p = 0; p<sentLen; p++) {
                    sum += fracChildren[p][c];
                }
                Assert.assertEquals(1.0, sum, 1e-13);
            }
        }
    }
}
