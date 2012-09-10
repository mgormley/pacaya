package edu.jhu.hltcoe.parse.decomp;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.parse.DeltaGenerator;
import edu.jhu.hltcoe.parse.FactorDeltaGenerator;
import edu.jhu.hltcoe.parse.FixedIntervalDeltaGenerator;
import edu.jhu.hltcoe.parse.IlpFormulation;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.parse.IlpViterbiParserWithDeltasTest.MockIlpViterbiParserWithDeltas;
import edu.jhu.hltcoe.util.Prng;


public class DeltaParseBlockFileWriterTest {

    private double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
        Assert.fail("Turning these tests off for now. They are too slow to be useful.");
    }
    
    @Test
    public void testBranchPriceAndCutPC() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("the cat ate the mouse with the hat");
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda ));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight;

        DeltaGenerator deltaGen;

        expectedParseWeight = -35.353843;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        getParsesMilpBlockPc(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight);

        expectedParseWeight = -39.128243;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        getParsesMilpBlockPc(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight);
    }    
    
    @Test
    public void testBranchPriceAndCutParsePC() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("mouse ate cat");
        //sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda ));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight;

        DeltaGenerator deltaGen;

        expectedParseWeight = -35.353843;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        getParsesParsePc(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight);

        expectedParseWeight = -39.128243;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        getParsesParsePc(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight);
    }   
    
    @Test
    public void testBranchAndCutCPM() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("the cat ate");
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda ));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight;

        DeltaGenerator deltaGen;

        expectedParseWeight = -35.353843;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        getParsesCpm(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight);

        expectedParseWeight = -39.128243;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        getParsesCpm(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight);
    }    
    
    public static DepTreebank getParsesMilpBlockPc(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.DIP_MILPBLOCK_PC, 2, 128);
        factory.setBlockFileWriter(new DeltaParseBlockFileWriter(formulation));
        IlpViterbiParserWithDeltas parser = new MockIlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    } 
    
    public static DepTreebank getParsesParsePc(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.DIP_PARSE_PC, 2, 128);
        factory.setBlockFileWriter(new DeltaParseBlockFileWriter(formulation));
        IlpViterbiParserWithDeltas parser = new MockIlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    }
    
    public static DepTreebank getParsesCpm(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.DIP_MILPBLOCK_CPM, 2, 128);
        factory.setBlockFileWriter(new DeltaParseBlockFileWriter(formulation));
        IlpViterbiParserWithDeltas parser = new MockIlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    }
    
}
