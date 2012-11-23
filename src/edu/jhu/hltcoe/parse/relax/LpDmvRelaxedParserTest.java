package edu.jhu.hltcoe.parse.relax;


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
    
    @Test
    public void testCplexLpParser() {
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
    public void testCplexLpParserMultipleSentences() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("cat ate hat");
        sentences.addSentenceFromString("mouse cat ate");
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

    public static RelaxedDepTreebank getLpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        LpDmvRelaxedParser parser = new LpDmvRelaxedParser(new CplexFactory(), IlpFormulation.FLOW_NONPROJ_LPRELAX);
        RelaxedDepTreebank trees = parser.getRelaxedParse(new DmvTrainCorpus(sentences), model);
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }
}
