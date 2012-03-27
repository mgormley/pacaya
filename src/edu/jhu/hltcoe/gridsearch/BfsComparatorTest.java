package edu.jhu.hltcoe.gridsearch;

import java.util.PriorityQueue;

import org.junit.Assert;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;


public class BfsComparatorTest {

    private static class MockDmvProblemNode extends DmvProblemNode {

        public MockDmvProblemNode(double bound) {
            super();
            this.optimisticBound = bound;
            this.isOptimisticBoundCached = true;
        }
        
    }
    
    @Test
    public void testOrdering() {
        BfsComparator comparator = new BfsComparator();
        PriorityQueue<DmvProblemNode> leafNodePQ = new PriorityQueue<DmvProblemNode>(11, comparator);
        leafNodePQ.add(new MockDmvProblemNode(-2));
        leafNodePQ.add(new MockDmvProblemNode(-1));
        leafNodePQ.add(new MockDmvProblemNode(-3));
        leafNodePQ.add(new MockDmvProblemNode(-4));

        Assert.assertEquals(-1.0, leafNodePQ.remove().getOptimisticBound(), 1e-13);
    }
    
    
}
