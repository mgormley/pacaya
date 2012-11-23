package edu.jhu.hltcoe.parse.relax;


import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.dmv.RelaxedDepTreebank;
import edu.jhu.hltcoe.lp.CplexFactory;
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
    
    /*
     * ZIMPL with arc#0#0#2 == 1
Variable Name           Solution Value
arc#0#0#2                     1.000000
arc#0#2#1                     1.000000
arc#0#2#3                     1.000000
genAdj#0#2$l                  1.000000
genAdj#0#2$r                  0.333333
numNonAdj#0@2d                0.666667
stopAdj#0#1$l                 1.000000
stopAdj#0#1$r                 1.000000
stopAdj#0#2$r                 0.666667
stopAdj#0#3$l                 1.000000
stopAdj#0#3$r                 1.000000
stopNonAdj#@3c                1.000000
stopNonAdj#@3d                0.333333
flow#0#0#2                    3.000000
flow#0#2#1                    1.000000
flow#0#2#3                    1.000000
numToSide#0@1c                1.000000
numToSide#0@19                1.000000
numToSide#0@1d                1.000000
genAdj#0#0$r                  1.000000
stopAdj#0#0$l                 1.000000
stopNonAdj#@39                1.000000
All other variables matching '*' are 0.

    CPLEX API 
Variable Name            Solution Value
arcRoot_{0,1}                  1.000000
arcChild_{0,2,0}               1.000000
arcChild_{0,1,2}               1.000000
genAdj_{0,1,1}                 0.333333
genAdj_{0,2,0}                 1.000000
numNonAdj_{0,1,1}              0.666667
stopAdj_{0,0,0}                1.000000
stopAdj_{0,0,1}                1.000000
stopAdj_{0,1,0}                1.000000
stopAdj_{0,1,1}                0.666667
stopAdj_{0,2,1}                1.000000
flowRoot_{0,1}                 3.000000
flowChild_{0,2,0}              1.000000
flowChild_{0,1,2}              2.000000
numToSide_{0,1,1}              1.000000
numToSide_{0,2,0}              1.000000
All other variables matching '*' are 0.

     */
    
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

    public static RelaxedDepTreebank getLpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(new CplexFactory(), formulation);
        parser.setTempDir(new File("."));
        RelaxedDepTreebank trees = parser.getRelaxedParse(new DmvTrainCorpus(sentences), model);
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }
}
