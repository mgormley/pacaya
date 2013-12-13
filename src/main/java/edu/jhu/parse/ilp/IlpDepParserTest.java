package edu.jhu.parse.ilp;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.data.DepTree;
import edu.jhu.data.DepTreebank;
import edu.jhu.data.SentenceCollection;
import edu.jhu.globalopt.dmv.RelaxedDepTreebank;
import edu.jhu.ilp.IlpSolverFactory;
import edu.jhu.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.model.Model;
import edu.jhu.model.dmv.DmvModelFactory;
import edu.jhu.model.dmv.RandomDmvModelFactory;
import edu.jhu.util.Prng;
import edu.jhu.util.Timer;

public class IlpDepParserTest {

    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testProjParses() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedParseWeight = -27.274;

        // flow projective parsing
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        // multi flow projective parsing
        DepTreebank mflowTrees = getIlpParses(model, sentences, IlpFormulation.MFLOW_PROJ, expectedParseWeight);
        // explicit projective parsing
        DepTreebank expTrees = getIlpParses(model, sentences, IlpFormulation.EXPLICIT_PROJ, expectedParseWeight);
        // DP projective parsing
        DepTreebank dpTrees = getIlpParses(model, sentences, IlpFormulation.DP_PROJ, expectedParseWeight);
        
        for (int i=0; i<dpTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), dpTrees.get(i).getParents());
            assertArrayEquals(flowTrees.get(i).getParents(), dpTrees.get(i).getParents());
            assertArrayEquals(mflowTrees.get(i).getParents(), dpTrees.get(i).getParents());
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

        Timer timer;

        // explicit projective parsing
        timer = new Timer();
        timer.start();
        DepTreebank expTrees = getIlpParses(model, sentences, IlpFormulation.EXPLICIT_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.avgMs());
        
        // flow projective parsing
        timer = new Timer();
        timer.start();
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.avgMs());

        for (int i=0; i<expTrees.size(); i++) {
            int[] expTree = expTrees.get(i).getParents();
            int[] flowTree = flowTrees.get(i).getParents();
            assertArrayEquals(expTree, flowTree);
        }
        
        // This is too slow to be useful
        if (false) {
            // DP projective parsing        
            timer = new Timer();
            timer.start();
            DepTreebank dpTrees = getIlpParses(model, sentences, IlpFormulation.DP_PROJ, expectedParseWeight);
            timer.stop();
            System.out.println(timer.avgMs());
            
            for (int i=0; i<expTrees.size(); i++) {
                assertArrayEquals(expTrees.get(i).getParents(), dpTrees.get(i).getParents());
            }
            
            // multi-c flow projective parsing
            timer = new Timer();
            timer.start();
            DepTreebank mflowTrees = getIlpParses(model, sentences, IlpFormulation.MFLOW_PROJ, expectedParseWeight);
            timer.stop();
            System.out.println(timer.avgMs());
            
            for (int i=0; i<expTrees.size(); i++) {
                assertArrayEquals(expTrees.get(i).getParents(), mflowTrees.get(i).getParents());
            }
        }
        
    }
    
    @Test
    public void testNonProj() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
//        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT");
//        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD .");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        double expectedParseWeight = -26.467;

        // Single commodity flow non-projective parsing
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_NONPROJ, expectedParseWeight);
        
        // Multi-commmidity flow non-projective parsing
        DepTreebank mflowTrees = getIlpParses(model, sentences, IlpFormulation.MFLOW_NONPROJ, expectedParseWeight);

        for (int i=0; i<flowTrees.size(); i++) {
            assertArrayEquals(flowTrees.get(i).getParents(), mflowTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testLpRelaxations() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        DmvModelFactory modelFactory = new RandomDmvModelFactory(lambda);
        Model model = modelFactory.getInstance(sentences.getLabelAlphabet());
        
        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        getLpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, -26.467);
        
        getLpParses(model, sentences, IlpFormulation.FLOW_PROJ_LPRELAX, -26.525);
        
        getLpParses(model, sentences, IlpFormulation.DP_PROJ_LPRELAX, -26.467);
    }

    public static DepTreebank getIlpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        IlpDepParser parser = new IlpDepParser(formulation, factory);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }

    public static RelaxedDepTreebank getLpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        IlpDepParser parser = new IlpDepParser(formulation, factory);
        RelaxedDepTreebank trees = parser.getRelaxedParse(sentences, model);
        System.out.println("logProb: " + parser.getLastParseWeight());
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-3);
        return trees;
    }
    
}
