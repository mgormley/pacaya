package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;
import junit.framework.Assert;

import org.apache.log4j.BasicConfigurator;
import org.jboss.dna.common.statistic.Stopwatch;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.DepTree;
import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.data.Sentence;
import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.data.Word;
import edu.jhu.hltcoe.ilp.IlpSolverFactory;
import edu.jhu.hltcoe.ilp.IlpSolverFactory.IlpSolverId;
import edu.jhu.hltcoe.model.DmvModelFactory;
import edu.jhu.hltcoe.model.DmvRandomWeightGenerator;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.ModelFactory;
import edu.jhu.hltcoe.util.Prng;

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
        sentences.add(getSentenceFromString("the cat ate the hat with the mouse"));
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -29.21248981;
        
        // flow projective parsing
        DepTreebank flowTrees = getParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        // multi flow projective parsing
        DepTreebank mflowTrees = getParses(model, sentences, IlpFormulation.MFLOW_PROJ, expectedParseWeight);
        // explicit projective parsing
        DepTreebank expTrees = getParses(model, sentences, IlpFormulation.EXPLICIT_PROJ, expectedParseWeight);
        // DP projective parsing
        DepTreebank dpTrees = getParses(model, sentences, IlpFormulation.DP_PROJ, expectedParseWeight);
        
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
        //sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD ."));
        sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT"));
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -57.26457638;

        Stopwatch timer;


        // flow projective parsing
        timer = new Stopwatch();
        timer.start();
        DepTreebank flowTrees = getParses(model, sentences, IlpFormulation.FLOW_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        // multi-c flow projective parsing
        timer = new Stopwatch();
        timer.start();
        DepTreebank mflowTrees = getParses(model, sentences, IlpFormulation.MFLOW_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        // explicit projective parsing
        timer = new Stopwatch();
        timer.start();
        DepTreebank expTrees = getParses(model, sentences, IlpFormulation.EXPLICIT_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        for (int i=0; i<expTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), mflowTrees.get(i).getParents());
            assertArrayEquals(expTrees.get(i).getParents(), flowTrees.get(i).getParents());
        }
        
        // DP projective parsing        
        timer = new Stopwatch();
        timer.start();
        DepTreebank dpTrees = getParses(model, sentences, IlpFormulation.DP_PROJ, expectedParseWeight);
        timer.stop();
        System.out.println(timer.getAverageDuration().getDurationInMilliseconds());
        
        for (int i=0; i<expTrees.size(); i++) {
            assertArrayEquals(expTrees.get(i).getParents(), dpTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testNonProj() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(getSentenceFromString("the cat ate the hat with the mouse"));
//        sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT"));
//        sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD ."));
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        double expectedParseWeight = -26.67243737;

        // Single commodity flow non-projective parsing
        DepTreebank flowTrees = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, expectedParseWeight);
        
        // Multi-commmidity flow non-projective parsing
        DepTreebank mflowTrees = getParses(model, sentences, IlpFormulation.MFLOW_NONPROJ, expectedParseWeight);

        for (int i=0; i<flowTrees.size(); i++) {
            assertArrayEquals(flowTrees.get(i).getParents(), mflowTrees.get(i).getParents());
        }
    }
    
    @Test
    public void testLpRelaxations() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(getSentenceFromString("the cat ate the hat with the mouse"));
//        sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT"));
//        sentences.add(getSentenceFromString("NNP NNP , CD NNS JJ , MD VB DT NN IN DT JJ NN NNP CD ."));
        ModelFactory modelFactory = new DmvModelFactory(new DmvRandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        
        // Single commodity flow non-projective parsing LP Relaxation
        // This is conveniently an integer solution
        getParses(model, sentences, IlpFormulation.FLOW_NONPROJ_LPRELAX, -24.879331719999996);
        
        getParses(model, sentences, IlpFormulation.DP_PROJ_LPRELAX, -24.879331719999996);
    }

    public static DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, double expectedParseWeight) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.CPLEX, 1, 128);
        IlpViterbiParser parser = new IlpViterbiParser(formulation, factory);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        Assert.assertEquals(expectedParseWeight, parser.getLastParseWeight(), 1E-13);
        return trees;
    }
    
    public static Sentence getSentenceFromString(String string) {
        return new StringSentence(string);
    }

    private static class StringSentence extends Sentence {

        private static final long serialVersionUID = 1L;

        public StringSentence(String string) {
            super();
            String[] splits = string.split("\\s");
            for (String tok : splits) {
                this.add(new Word(tok));
            }
        }

    }

}
