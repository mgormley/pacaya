package edu.jhu.hltcoe.gridsearch.dmv;

import static junit.framework.Assert.assertEquals;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Relaxation;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.cpt.BasicCptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.CptBounds;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDeltaFactory;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.RandomVariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSelector;
import edu.jhu.hltcoe.gridsearch.cpt.VariableSplitter;
import edu.jhu.hltcoe.gridsearch.cpt.CptBoundsDelta.Type;
import edu.jhu.hltcoe.gridsearch.cpt.MidpointVarSplitter.MidpointChoice;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRltRelaxation.DmvRltRelaxPrm;
import edu.jhu.hltcoe.train.DmvTrainCorpus;
import edu.jhu.hltcoe.util.Prng;


public class DmvProblemNodeTest {

    static {
        BasicConfigurator.configure();
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }

    @Before
    public void setUp() {
        Prng.seed(1234567890);
        DmvProblemNode.resetIdCounter();
    }
    
    /**
     * This is an overly complicated test that checks the interaction between
     * DmvProblemNode and DmvRelaxation.
     */
    @Test
    public void testBranch() {
        SentenceCollection sentences = new SentenceCollection();
        sentences.addSentenceFromString("N V");
//        sentences.addSentenceFromString("N V P");
//        sentences.addSentenceFromString("N V N N N");
//        sentences.addSentenceFromString("N V P N");
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);

        VariableSelector varSelector = new RandomVariableSelector(true);
        VariableSplitter varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_PROB);
        CptBoundsDeltaFactory brancher = new BasicCptBoundsDeltaFactory(varSelector, varSplitter);
        DmvProblemNode node = new DmvProblemNode(brancher);
        DmvRelaxation relax = new DmvRltRelaxPrm().getInstance(corpus, DmvDantzigWolfeRelaxationTest.getInitFeasSol(corpus));
        RelaxedSolution relaxSol = null;
        relax.getRelaxedSolution(node);
        List<ProblemNode> children = node.branch(relax, relaxSol);
        assertEquals(2, children.size());
        DmvProblemNode c1 = (DmvProblemNode)children.get(0);
        DmvProblemNode c2 = (DmvProblemNode)children.get(1);
        
        assertEquals(0, node.getId()); 
        assertEquals(1, c1.getId()); 
        assertEquals(2, c2.getId()); 
        
        assertEquals(0, node.getDepth()); 
        assertEquals(1, c1.getDepth());
                
        IndexedDmvModel idm = new IndexedDmvModel(corpus);
        double[][][] bounds = new double[idm.getNumConds()][][];
        for (int c=0; c<idm.getNumConds(); c++) {
            bounds[c] = new double[idm.getNumParams(c)][2];
            for (int m=0; m<idm.getNumParams(c); m++) {
                CptBounds b = getBounds(relax, node);
                bounds[c][m][0] = b.getLb(Type.PARAM, c, m);
                bounds[c][m][1] = b.getUb(Type.PARAM, c, m);
            }
        }
                
        checkedSetActive(relax, c1, node);
        for (int c=0; c<idm.getNumConds(); c++) {
            CptBounds b = getBounds(relax, c1);                
             for (int m=0; m<idm.getNumParams(c); m++) {
                Assert.assertTrue(bounds[c][m][0] <= b.getLb(Type.PARAM, c, m));
                Assert.assertTrue(bounds[c][m][1] >= b.getUb(Type.PARAM, c, m));
            }
        }
        assertEquals(Math.log(0.5), getBounds(relax, c1).getLb(Type.PARAM, 2, 1), 1e-7);
        
        
        List<ProblemNode> c1Children = c1.branch(relax, relaxSol);
        DmvProblemNode c3 = (DmvProblemNode)c1Children.get(0);
        
        checkedSetActive(relax, c3, c1);
        
        checkedSetActive(relax, c2, c3);
        assertEquals(Math.log(0.5), getBounds(relax, c2).getUb(Type.PARAM, 2, 1), 1e-7);
        DmvProblemNode c4 = (DmvProblemNode)c2.branch(relax, relaxSol).get(1);
        checkedSetActive(relax, c4, c2);
        for (int c=0; c<idm.getNumConds(); c++) {
            for (int m=0; m<idm.getNumParams(c); m++) {
                CptBounds b = getBounds(relax, c4);                
                Assert.assertTrue(bounds[c][m][0] <= b.getLb(Type.PARAM, c, m));
                Assert.assertTrue(bounds[c][m][1] >= b.getUb(Type.PARAM, c, m));
            }
        }

        checkedSetActive(relax, c3, c4);
        
        DmvProblemNode prev = c3;
        for (int i=0; i<10; i++) {
            DmvProblemNode cur = (DmvProblemNode)prev.branch(relax, relaxSol).get(0);
            checkedSetActive(relax, cur, prev);
            prev = cur;
        }
        if (relax != null) {
            relax.end();
        }
    }

    private CptBounds getBounds(DmvRelaxation relax, DmvProblemNode node) {
        return relax.getBounds();
    }

    private void checkedSetActive(DmvRelaxation relax, DmvProblemNode nextNode, DmvProblemNode prevNode) {
        if (nextNode.getParent() != prevNode) {
            relax.getRelaxedSolution(nextNode);
        } else {
            double prevBound = prevNode.getLocalUb();
            relax.getRelaxedSolution(nextNode);
            double nextBound = nextNode.getLocalUb();
            System.out.println("parent: " + prevBound + " child: " + nextBound);
            Assert.assertTrue(prevBound >= nextBound);
        }
    }
    
}
