package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.RandomDmvModelFactory;
import edu.jhu.hltcoe.util.Files;
import edu.jhu.hltcoe.util.Prng;


public class InitializedIlpViterbiParserWithDeltasTest {

    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testIdentityDeltaGen() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("the cat ate the mouse with the hat");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedParseWeight = -37.087;

        DeltaGenerator deltaGen;

        DepTreebank treesStandard = IlpViterbiParserTest.getIlpParses(model, sentences, IlpFormulation.FLOW_NONPROJ, expectedParseWeight);

        deltaGen = new IdentityDeltaGenerator();
        DepTreebank treesDelta = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight, expectedParseWeight);

        for (int i=0; i<treesStandard.size(); i++) {
            assertArrayEquals(treesStandard.get(i).getParents(), treesDelta.get(i).getParents());
        }
    }
    
    @Test
    public void testNonprojDeltaParsers() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("the cat ate the mouse with the hat");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        
        double expectedInitParseWeight;
        double expectedParseWeight;

        DeltaGenerator deltaGen;
        expectedInitParseWeight = -37.087;
        expectedParseWeight = -32.271;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight, expectedInitParseWeight);
    }
    
    @Test
    public void testProjDeltaParsers() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("cat ate mouse");
        sentences.addSentenceFromString("the cat ate the mouse with the hat");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        
        double expectedInitParseWeight;
        double expectedParseWeight;

        DeltaGenerator deltaGen;
        expectedInitParseWeight = -38.439;
        expectedParseWeight = -36.342;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        getParses(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight, expectedInitParseWeight);
    }
        
    public static DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight, double expectedInitParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 2, 128);
        InitializedIlpViterbiParserWithDeltas parser = new InitializedIlpViterbiParserWithDeltas(formulation, factory, deltaGen, factory);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        
        File initCplexLog = new File(new File(parser.getInitWorkspace(), "ilp_parse_000"), "cplex.log");
        Matcher initMatch = Files.getFirstMatch(initCplexLog, Pattern.compile("Solution value = (.+)"));
        Assert.assertEquals(expectedInitParseWeight, Double.parseDouble(initMatch.group(1)), 1E-3);
        
        File cplexLog = new File(new File(parser.getWorkspace(), "ilp_parse_000"), "cplex.log");
        Matcher match = Files.getFirstMatch(cplexLog, Pattern.compile("defined initial solution with objective (.+)\\."));
        Assert.assertEquals(expectedInitParseWeight, Double.parseDouble(match.group(1)), 1E-3);
        
        return trees;
    }
        
}
