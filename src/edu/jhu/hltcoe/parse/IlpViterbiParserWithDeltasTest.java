package edu.jhu.hltcoe.parse;

import static org.junit.Assert.assertArrayEquals;

import org.apache.log4j.BasicConfigurator;
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


public class IlpViterbiParserWithDeltasTest {

    static {
        BasicConfigurator.configure();
    }
    
    private final static double lambda = 0.1;

    @Test
    public void testProjParses() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        
        DeltaGenerator deltaGen;
        
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        DepTreebank npFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen);
        
        deltaGen = new FactorDeltaGenerator(1.1, 2);
        DepTreebank pFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_PROJ, deltaGen);
    }
    
    @Test
    public void testSos() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.add(IlpViterbiParserTest.getSentenceFromString("cat ate mouse"));
        sentences.add(IlpViterbiParserTest.getSentenceFromString("the cat ate the mouse with the hat"));
        ModelFactory modelFactory = new DmvModelFactory(new RandomWeightGenerator(lambda));
        Model model = modelFactory.getInstance(sentences);
        
        DeltaGenerator deltaGen;
        
        deltaGen = new FixedIntervalDeltaGenerator(0.1, 1);
        DepTreebank npFlowTrees = getParses(model, sentences, IlpFormulation.FLOW_NONPROJ, deltaGen);
    }
    
    public DepTreebank getParses(Model model, SentenceCollection sentences, IlpFormulation formulation, DeltaGenerator deltaGen) {
        IlpSolverFactory factory = new IlpSolverFactory(IlpSolverId.GUROBI_CL, 2, -1);
        IlpViterbiParserWithDeltas parser = new IlpViterbiParserWithDeltas(formulation, factory, deltaGen);
        DepTreebank trees = parser.getViterbiParse(sentences, model);
        for (DepTree depTree : trees) {
            System.out.println(depTree);
        }
        return trees;
    }
    
}
