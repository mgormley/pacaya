package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.DmvModelFactory.RandomWeightGenerator;
import edu.jhu.hltcoe.util.Prng;


public class IlpViterbiParserWithDeltasTest {

    static {
        BasicConfigurator.configure();
    }
    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.setSeed(1234567890);
    }
    
    @Test
    public void testIdentityDeltaGen() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -40.56204981;

        DeltaGenerator deltaGen;

        deltaGen = new FactorDeltaGenerator(1.1, 2);
        DepTreebank treesStandard = IlpViterbiParserTest.getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, expectedParseWeight);

        deltaGen = new IdentityDeltaGenerator();
        DepTreebank treesDelta = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight);

        for (int i=0; i<treesStandard.size(); i++) {
            assertArrayEquals(treesStandard.get(i).getParents(), treesDelta.get(i).getParents());
        }
    }
    
    @Test
    public void testProjAndNonprojDeltaParsers() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight;

        DeltaGenerator deltaGen;
        
        expectedParseWeight = -140.25513195;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        DepTreebank npFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight);

        expectedParseWeight = -171.80570295;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        DepTreebank pFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight);
    }
        
    public static DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.GUROBI_CL, 2, 128);
        IlpViterbiParserWithDeltas parser = new IlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    }
    
}
