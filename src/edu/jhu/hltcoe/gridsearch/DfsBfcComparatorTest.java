package edu.jhu.hltcoe.gridsearch;

import java.util.PriorityQueue;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;


public class DfsBfcComparatorTest {

    private static class MockDmvProblemNode extends DmvProblemNode {

        public MockDmvProblemNode() {
            super();
        }
        
        public MockDmvProblemNode(double bound, DmvProblemNode parent, int side) {
            super(null, null, parent, side);
            this.optimisticBound = bound;
        }
        
    }
    
    @Test
    public void testOrdering() {
        DfsBfcComparator comparator = new DfsBfcComparator();
        PriorityQueue<DmvProblemNode> leafNodePQ = new PriorityQueue<DmvProblemNode>(11, comparator);
        DmvProblemNode root = new MockDmvProblemNode();
        DmvProblemNode node1 = new MockDmvProblemNode(-4, root, 0);
        DmvProblemNode node2 = new MockDmvProblemNode(-1, root, 1);
        DmvProblemNode node3 = new MockDmvProblemNode(-2, node1, 0);
        DmvProblemNode node4 = new MockDmvProblemNode(-3, node1, 1);

        leafNodePQ.add(root);
        leafNodePQ.add(node1);
        leafNodePQ.add(node2);
        leafNodePQ.add(node3);
        leafNodePQ.add(node4);
    
        Assert.assertTrue(node3 == leafNodePQ.remove());
        Assert.assertTrue(node4 == leafNodePQ.remove());
        Assert.assertTrue(node2 == leafNodePQ.remove());
        Assert.assertTrue(node1 == leafNodePQ.remove());
        Assert.assertTrue(root == leafNodePQ.remove());
    }
    
    
}
