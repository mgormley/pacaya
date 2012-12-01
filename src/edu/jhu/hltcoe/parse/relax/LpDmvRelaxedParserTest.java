package edu.jhu.hltcoe.parse.relax;


import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProjector;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.lp.CplexPrm;
import edu.jhu.hltcoe.math.Vectors;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParserTest;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;

public class LpDmvRelaxedParserTest {

    static {
        BasicConfigurator.configure();
    }
    
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
        RelaxedDepTreebank trees2 = IlpViterbiParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        // This should have the same expected objective as the Zimpl version.
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
        Assert.assertArrayEquals(trees1.getFracRoots(), trees2.getFracRoots());
        Assert.assertArrayEquals(trees1.getFracChildren(), trees2.getFracChildren());
    }
    
    @Test
    public void testTinyProj3() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        double expectedObj = -7.802;

        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpViterbiParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
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
        
        // This should have the same expected objective as the Zimpl version.
        double expectedObj = -26.467;
        RelaxedDepTreebank trees1 = getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        RelaxedDepTreebank trees2 = IlpViterbiParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
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
        RelaxedDepTreebank trees2 = IlpViterbiParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, expectedObj);
        
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
        RelaxedDepTreebank trees2 = IlpViterbiParserTest.getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, expectedObj);
        
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
    public void testProjection() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("cat ate hat");
        sentences.addSentenceFromString("mouse cat ate");        
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());

        IlpFormulation formulation = IlpFormulation.FLOW_PROJ_LPRELAX;
        
        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(new CplexPrm(), formulation);
        parser.setTempDir(new File("."));
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);
        RelaxedDepTreebank trees = parser.getRelaxedParse(corpus, model);
        checkFractionalTrees(trees);
        
        DmvProjector projector = new DmvProjector(corpus);
        projector.getProjectedParses(trees);
    }
    
    public static RelaxedDepTreebank getLpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(new CplexPrm(), formulation);
        parser.setTempDir(new File("."));
        RelaxedDepTreebank trees = parser.getRelaxedParse(new DmvTrainCorpus(sentences), model);
        checkFractionalTrees(trees);
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }

    private static void checkFractionalTrees(RelaxedDepTreebank trees) {
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
