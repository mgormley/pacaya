package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.jboss.dna.common.statistic.Stopwatch;
import org.junit.Before;
import org.junit.Test;

import util.Alphabet;
import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Label;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.Word;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvModelFactory;
import edu.jhu.hltcoe.model.dmv.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.util.Prng;
import edu.jhu.hltcoe.util.Utilities;

public class IlpViterbiParserTest {

    static {
        BasicConfigurator.configure();
    }
    
    private final static double lambda = 0.1;

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }
    
    @Test
    public void testProjParses() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -27.33002417937424;
        
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
        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT");
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -51.94204629750775;

        Stopwatch timer;


        // flow projective parsing
        timer = new Stopwatch();
        timer.start();
        DepTreebank flowTrees = getIlpParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        // multi-c flow projective parsing
        timer = new Stopwatch();
        timer.start();
        DepTreebank mflowTrees = getIlpParses(model, sentences, IlpFormulation.MFLOW_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        // explicit projective parsing
        timer = new Stopwatch();
        timer.start();
        DepTreebank expTrees = getIlpParses(model, sentences, IlpFormulation.EXPLICIT_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        for (int i=0; i<expTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), mflowTrees.get(i).getParents());
            assertArrayEquals(expTrees.get(i).getParents(), flowTrees.get(i).getParents());
        }
        
        // DP projective parsing        
        timer = new Stopwatch();
        timer.start();
        DepTreebank dpTrees = getIlpParses(model, sentences, IlpFormulation.DP_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        for (int i=0; i<expTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testNonProj() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("the cat ate the hat with the mouse");
//        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT");
//        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD .");
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -24.78997246377081;

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
//        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT");
//        sentences.addSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD .");
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        
        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        getIlpParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, -24.879331719999996);
        
        getIlpParses(model, sentences, IlpFormulation.DP_PROJ_LPRELAX, -24.879331719999996);
    }

    public static DepTreebank getIlpParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        IlpViterbiParser parser = new IlpViterbiParser(formulation, factory);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        System.out.println("prob: " + Utilities.exp(parser.getLastParseWeight()));
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    }
    
}
