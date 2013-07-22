package edu.jhu.gridsearch;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.gridsearch.DfsNodeOrderer.DfsNodeOrdererPrm;
import edu.jhu.gridsearch.LazyBranchAndBoundSolver.NodeResult;
import edu.jhu.gridsearch.dmv.DmvProblemNode;


public class DfsNodeOrdererTest {

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
        DfsNodeOrdererPrm prm = new DfsNodeOrdererPrm();
        prm.childOrderer = new MockChildOrderer();
        DfsNodeOrderer leafNodePQ = new DfsNodeOrderer(prm);
        MockDmvProblemNode root = new MockDmvProblemNode();
        MockDmvProblemNode node1 = new MockDmvProblemNode(-4, root, 0);
        MockDmvProblemNode node2 = new MockDmvProblemNode(-1, root, 1);
        MockDmvProblemNode node3 = new MockDmvProblemNode(-2, node1, 0);
        MockDmvProblemNode node4 = new MockDmvProblemNode(-3, node1, 1);

        double globalUb = -1;
        double globalLb = -11;
        
        leafNodePQ.addRoot(root);        
        leafNodePQ.addChildrenOfResult(new MockNodeResult(root), globalUb, globalLb, true);
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node1), globalUb, globalLb, false);
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node2), globalUb, globalLb, false);
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node3), globalUb, globalLb, false);
        leafNodePQ.addChildrenOfResult(new MockNodeResult(node4), globalUb, globalLb, false);

        Assert.assertEquals(node4.getId(), leafNodePQ.remove().getId());
        Assert.assertEquals(node3.getId(), leafNodePQ.remove().getId());
        Assert.assertEquals(node2.getId(), leafNodePQ.remove().getId());
        Assert.assertEquals(node1.getId(), leafNodePQ.remove().getId());
        Assert.assertEquals(root.getId(), leafNodePQ.remove().getId());

    }
    
    
}
