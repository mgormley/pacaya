package edu.jhu.nlp.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DepEdgeMaskTest {

    @Test
    public void testKeepEdgesFromTree() {
        int[] parents = new int[]{ 2, 0, -1, 2, 5, 3 };
        DepEdgeMask mask = new DepEdgeMask(parents.length, false);
        assertEquals(0, mask.getCount());
        mask.keepEdgesFromTree(parents);
        assertEquals(parents.length, mask.getCount());
        assertTrue(mask.isKept(2, 0));
        assertTrue(mask.isKept(0, 1));
        assertTrue(mask.isKept(-1, 2));
        assertTrue(mask.isKept(2, 3));
        assertTrue(mask.isKept(5, 4));
        assertTrue(mask.isKept(3, 5));
    }
    
    @Test
    public void testAllowsSingleRootTrees() {
        DepEdgeMask mask = new DepEdgeMask(4, false);
        
        // No possible edges.
        mask.setIsKeptAll(false);
        assertTrue(!mask.allowsSingleRootTrees());
        
        // All possible edges.
        mask.setIsKeptAll(true);
        assertTrue(mask.allowsSingleRootTrees());
        
        // Only edges to root.
        mask.setIsKeptAll(false);
        mask.setIsKept(-1, 0, true);
        mask.setIsKept(-1, 1, true);
        mask.setIsKept(-1, 2, true);
        mask.setIsKept(-1, 3, true);
        assertTrue(!mask.allowsSingleRootTrees());
        
        // Only two cycles.
        mask.setIsKeptAll(false);
        mask.setIsKept(0, 1, true);
        mask.setIsKept(1, 0, true);
        mask.setIsKept(2, 3, true);
        mask.setIsKept(3, 2, true);
        assertTrue(!mask.allowsSingleRootTrees());
        
        // Two cycles with two outlets to root.
        mask.setIsKept(-1, 0, true);
        mask.setIsKept(-1, 2, true);
        assertTrue(!mask.allowsSingleRootTrees());
        
        // Allowing a valid tree (e.g. right-branching)
        mask.setIsKept(1, 2, true);
        assertTrue(mask.allowsSingleRootTrees());
    }
    
    @Test
    public void testAllowsMultiRootTrees() {
        DepEdgeMask mask = new DepEdgeMask(4, false);
        
        // No possible edges.
        mask.setIsKeptAll(false);
        assertTrue(!mask.allowsMultiRootTrees());
        
        // All possible edges.
        mask.setIsKeptAll(true);
        assertTrue(mask.allowsMultiRootTrees());
        
        // Only edges to root.
        mask.setIsKeptAll(false);
        mask.setIsKept(-1, 0, true);
        mask.setIsKept(-1, 1, true);
        mask.setIsKept(-1, 2, true);
        mask.setIsKept(-1, 3, true);
        assertTrue(mask.allowsMultiRootTrees());
        
        // Only two cycles.
        mask.setIsKeptAll(false);
        mask.setIsKept(0, 1, true);
        mask.setIsKept(1, 0, true);
        mask.setIsKept(2, 3, true);
        mask.setIsKept(3, 2, true);
        assertTrue(!mask.allowsMultiRootTrees());
        
        // Two cycles with two outlets to root.
        mask.setIsKept(-1, 0, true);
        mask.setIsKept(-1, 2, true);
        assertTrue(mask.allowsMultiRootTrees());
        
        // Allowing a single root tree (e.g. right-branching)
        mask.setIsKept(1, 2, true);
        assertTrue(mask.allowsMultiRootTrees());
    }

}
