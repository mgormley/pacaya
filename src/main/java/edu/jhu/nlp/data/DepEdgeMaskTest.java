package edu.jhu.nlp.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DepEdgeMaskTest {

    @Test
    public void testAllowsSinglyRootedTrees() {
        DepEdgeMask mask = new DepEdgeMask(4, false);
        
        // No possible edges.
        mask.setIsKeptAll(false);
        assertTrue(!mask.allowsSinglyRootedTrees());
        
        // All possible edges.
        mask.setIsKeptAll(true);
        assertTrue(mask.allowsSinglyRootedTrees());
        
        // Only edges to root.
        mask.setIsKeptAll(false);
        mask.setIsKept(-1, 0, true);
        mask.setIsKept(-1, 1, true);
        mask.setIsKept(-1, 2, true);
        mask.setIsKept(-1, 3, true);
        assertTrue(!mask.allowsSinglyRootedTrees());
        
        // Only two cycles.
        mask.setIsKeptAll(false);
        mask.setIsKept(0, 1, true);
        mask.setIsKept(1, 0, true);
        mask.setIsKept(2, 3, true);
        mask.setIsKept(3, 2, true);
        assertTrue(!mask.allowsSinglyRootedTrees());
        
        // Two cycles with two outlets to root.
        mask.setIsKept(-1, 0, true);
        mask.setIsKept(-1, 2, true);
        assertTrue(!mask.allowsSinglyRootedTrees());
        
        // Allowing a valid tree (e.g. right-branching)
        mask.setIsKept(1, 2, true);
        assertTrue(mask.allowsSinglyRootedTrees());
    }

}
