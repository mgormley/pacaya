package edu.jhu.hltcoe.gridsearch.dmv;

import static junit.framework.Assert.assertEquals;

import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.gridsearch.PlungingBfsNodeOrderer;
import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.Relaxation;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;
import edu.jhu.hltcoe.gridsearch.PlungingBfsNodeOrderer.PlungingBfsNodeOrdererPrm;
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


public class RelaxSolChildOrdererTest {

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
        DmvTrainCorpus corpus = new DmvTrainCorpus(sentences);

        VariableSelector varSelector = new RandomVariableSelector(true);
        VariableSplitter varSplitter = new MidpointVarSplitter(MidpointChoice.HALF_PROB);
        CptBoundsDeltaFactory brancher = new BasicCptBoundsDeltaFactory(varSelector, varSplitter);
        DmvRelaxation relax = new DmvRltRelaxPrm().getInstance(corpus, DmvDantzigWolfeRelaxationTest.getInitFeasSol(corpus));
        
        // Solve and branch on root node.
        DmvProblemNode root = new DmvProblemNode(brancher);
        RelaxedSolution rootSol = relax.getRelaxedSolution(root);
        List<ProblemNode> children = root.branch(relax, rootSol);
        assertEquals(2, children.size());
        DmvProblemNode c1 = (DmvProblemNode)children.get(0);
        DmvProblemNode c2 = (DmvProblemNode)children.get(1);
        
        // Solve and branch on child 1 (c1).
        RelaxedSolution c1Sol = relax.getRelaxedSolution(c1);
        List<ProblemNode> c1Children = c1.branch(relax, c1Sol);
        assertEquals(2, c1Children.size());
        DmvProblemNode c1c1 = (DmvProblemNode)c1Children.get(0);
        DmvProblemNode c1c2 = (DmvProblemNode)c1Children.get(1);        

        // Get the relevant log probs from the relaxed solutions.
        int c = ((DmvProblemNode)c1Children.get(0)).getDeltas().getPrimary().getC();
        int m = ((DmvProblemNode)c1Children.get(0)).getDeltas().getPrimary().getM();
        Type type = ((DmvProblemNode)c1Children.get(0)).getDeltas().getPrimary().getType();
        double rootCmLogProb = ((RelaxedDmvSolution)rootSol).getLogProbs()[c][m];
        double c1CmLogProb = ((RelaxedDmvSolution)c1Sol).getLogProbs()[c][m];
        Assert.assertEquals(c1CmLogProb, rootCmLogProb, 1e-10);

        {
            // Order the children of child 1 (c1).
            RelaxSolChildOrderer co = new RelaxSolChildOrderer();
            List<ProblemNode> order = co.orderChildren(c1Sol, rootSol, c1Children);

            // Check the ordering.
            assertEquals(2, order.size());
            Assert.assertEquals(c1c1.getId(), order.get(0).getId());
            Assert.assertEquals(c1c2.getId(), order.get(1).getId());
        }

        {
            ((RelaxedDmvSolution) c1Sol).getLogProbs()[c][m] = rootCmLogProb - 10;

            // Order the children of child 1 (c1).
            RelaxSolChildOrderer co = new RelaxSolChildOrderer();
            List<ProblemNode> order = co.orderChildren(c1Sol, rootSol, c1Children);

            // Check the ordering.
            assertEquals(2, order.size());
            Assert.assertEquals(c1c2.getId(), order.get(0).getId()); // LOWER
            Assert.assertEquals(c1c1.getId(), order.get(1).getId()); // UPPER
        }

        {
            ((RelaxedDmvSolution) c1Sol).getLogProbs()[c][m] = rootCmLogProb + 10;

            // Order the children of child 1 (c1).
            RelaxSolChildOrderer co = new RelaxSolChildOrderer();
            List<ProblemNode> order = co.orderChildren(c1Sol, rootSol, c1Children);

            // Check the ordering.
            assertEquals(2, order.size());
            Assert.assertEquals(c1c1.getId(), order.get(0).getId()); // LOWER
            Assert.assertEquals(c1c2.getId(), order.get(1).getId()); // UPPER
        }
    }

}
