package edu.jhu.hltcoe.gridsearch.randwalk;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import edu.jhu.hltcoe.gridsearch.ProblemNode;
import edu.jhu.hltcoe.gridsearch.dmv.DmvProblemNode;
import edu.jhu.hltcoe.util.Prng;


public class DfsRandChildNodeOrdererTest {

    @Before
    public void setUp() {
        Prng.seed(1234567890);
    }

    private static class MockDmvProblemNode extends DmvProblemNode {

        public MockDmvProblemNode(int depth) {
            super();
            this.depth = depth;
            this.optimisticBound = depth;
        }
        
    }
    
    
    @Test
    public void testDepthOrder() {
        ProblemNode n0a = new MockDmvProblemNode(0);
        ProblemNode n1a = new MockDmvProblemNode(1);
        ProblemNode n1b = new MockDmvProblemNode(1);
        ProblemNode n2a = new MockDmvProblemNode(2);
        ProblemNode n2b = new MockDmvProblemNode(2);
        ProblemNode n2c = new MockDmvProblemNode(2);
        ProblemNode n3a = new MockDmvProblemNode(3);
        
        DfsRandChildAtDepthNodeOrderer pq = new DfsRandChildAtDepthNodeOrderer(3);
        pq.add(n1a);
        pq.add(n2a);
        pq.add(n3a);
        pq.add(n2b);
        pq.add(n1b);
        pq.add(n2c);
        pq.add(n0a);
        
        Assert.assertEquals(7, pq.size());
                
        Assert.assertEquals(n0a, pq.remove());
        Assert.assertEquals(n1b, pq.remove());
        Assert.assertEquals(n2a, pq.remove());
        Assert.assertEquals(n1a, pq.remove());
        Assert.assertEquals(n2b, pq.remove());
        Assert.assertEquals(n2c, pq.remove());
        Assert.assertEquals(n3a, pq.remove());
        
        Assert.assertEquals(true, pq.isEmpty());
    }
    
    //@Test
    public void testIterator() {
        //TODO: write this test and uncomment the annotation.
    }
    
}
