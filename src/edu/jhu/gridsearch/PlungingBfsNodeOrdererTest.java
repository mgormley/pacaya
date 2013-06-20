package edu.jhu.hltcoe.gridsearch;

import java.util.ArrayList;
import java.util.List;


import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.LazyBranchAndBoundSolver.NodeResult;
import edu.jhu.hltcoe.gridsearch.PlungingBfsNodeOrderer.PlungingBfsNodeOrdererPrm;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvRelaxedSolution;


public class PlungingBfsNodeOrdererTest {

    static {
        //Logger.getRootLogger().setLevel(Level.TRACE);
    }
    
    private static class MockDmvProblemNode extends DmvProblemNode {

        private List<ProblemNode> children = new ArrayList<ProblemNode>();
        
        public MockDmvProblemNode() {
            super(null);
        }
        
        public MockDmvProblemNode(double bound, MockDmvProblemNode parent, int side) {
            super(null, null, parent, side);
            this.optimisticBound = bound;
            parent.children.add(this);
        }

        public List<ProblemNode> getChildren() {
            return children;
        }        
        
    }
    
    private static class MockNodeResult extends NodeResult {

        public MockNodeResult(MockDmvProblemNode node1) {
            this.children = node1.getChildren();
            this.relaxSol = new DmvRelaxedSolution(null, null, node1.getLocalUb(), null, null, null, Double.NaN);
        }
        
    }
    
    private static class MockChildOrderer implements ChildOrderer {

        @Override
        public List<ProblemNode> orderChildren(RelaxedSolution relaxSol, RelaxedSolution rootSol,
                List<ProblemNode> children) {
            return children;
        }
        
    }
    
    @Test
    public void testOrdering() {
        PlungingBfsNodeOrdererPrm prm = new PlungingBfsNodeOrdererPrm();
        prm.childOrderer = new MockChildOrderer();
        prm.minPlungeDepthProp = 0.0;
        prm.maxPlungeDepthProp = 0.5;
        PlungingBfsNodeOrderer leafNodePQ = new PlungingBfsNodeOrderer(prm);
        MockDmvProblemNode root = new MockDmvProblemNode();
        MockDmvProblemNode node1 = new MockDmvProblemNode(-4, root, 0);
        MockDmvProblemNode node2 = new MockDmvProblemNode(-1, root, 1);
        MockDmvProblemNode node3 = new MockDmvProblemNode(-2, node1, 0);
        MockDmvProblemNode node4 = new MockDmvProblemNode(-3, node1, 1);

        double globalUb = -1;
        double globalLb = -11;
        
        leafNodePQ.addRoot(root);        
        Assert.assertEquals(root.getId(), leafNodePQ.remove().getId());
        leafNodePQ.addChildrenOfResult(new MockNodeResult(root), globalUb, globalLb, true);
        Assert.assertEquals(node2.getId(), leafNodePQ.remove().getId());
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node2), globalUb, globalLb, false);
        Assert.assertEquals(node1.getId(), leafNodePQ.remove().getId());
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node1), globalUb, globalLb, false);
        Assert.assertEquals(node4.getId(), leafNodePQ.remove().getId());
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node4), globalUb, globalLb, false);
        Assert.assertEquals(node3.getId(), leafNodePQ.remove().getId());
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node3), globalUb, globalLb, false);

    }
    
    
}
