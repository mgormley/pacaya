package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
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
import edu.jhu.hltcoe.util.Prng;


public class IlpViterbiParserWithDeltasTest {

    static {
        BasicConfigurator.configure();
    }
    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testIdentityDeltaGen() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -40.56204981;

        DeltaGenerator deltaGen;

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
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight;

        DeltaGenerator deltaGen;

        expectedParseWeight = -35.35388011;
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen, expectedParseWeight);

        expectedParseWeight = -39.12828011;
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        getParses(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen, expectedParseWeight);
    }
        
    public static DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 2, 128);
        IlpViterbiParserWithDeltas parser = new MockIlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    }
    
    public static class MockIlpViterbiParserWithDeltas extends IlpViterbiParserWithDeltas {
        
        public MockIlpViterbiParserWithDeltas(IlpFormulation formulation, IlpSolverFactory ilpSolverFactory,
                DeltaGenerator deltaGen) {
            super(formulation, ilpSolverFactory, deltaGen);
        }

        protected DepTreebank decode(SentenceCollection sentences, Map<String,Double> result) {
            // Get trees from arcDelta vars
            DepTreebank arcDeltaTrees = new DepTreebank();
            
            int[][] parents = new int[sentences.size()][];
            for (int i=0; i<sentences.size(); i++) {
                parents[i] = new int[sentences.get(i).size()];
                Arrays.fill(parents[i], DepTree.EMPTY_POSITION);
            }
            
            for (Entry<String,Double> entry : result.entrySet()) {
                String zimplVar = entry.getKey();
                Double value = entry.getValue();
                String[] splits = zimplVarRegex.split(zimplVar);
                String varType = splits[0];
                if (varType.equals("arcDelta")) {
                    int sentId = Integer.parseInt(splits[1]);
                    int parent = Integer.parseInt(splits[2]);
                    int child = Integer.parseInt(splits[3]);
                    // String deltaId = splits[4];
                    long longVal = Math.round(value);
                    if (longVal == 1) {
                        assert(parents[sentId][child-1] == DepTree.EMPTY_POSITION);
                        // Must subtract one from each position
                        parents[sentId][child-1] = parent-1;
                    }
                }
            }
            
            for (int i=0; i<sentences.size(); i++) {
                DepTree tree = new DepTree(sentences.get(i), parents[i], formulation.isProjective());
                arcDeltaTrees.add(tree);
            }
            
            // Get trees from arc vars
            DepTreebank arcTrees = super.decode(sentences, result);
            
            // Assert equality of the two treebanks
            for (int i=0; i<arcTrees.size(); i++) {
                assertArrayEquals(arcTrees.get(i).getParents(), arcTrees.get(i).getParents());
            }
            
            return arcTrees;
        }
    }
    
}
