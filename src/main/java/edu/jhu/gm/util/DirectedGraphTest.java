package edu.jhu.gm.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.jhu.gm.util.MockDigraph.MockEdge;
import edu.jhu.gm.util.MockDigraph.MockNode;

public class DirectedGraphTest {

    @Test
    public void testIsTree() {
        MockDigraph g = new MockDigraph();
        MockNode n1 = g.new MockNode();
        MockNode n2 = g.new MockNode();
        MockNode n3 = g.new MockNode();
        
        MockEdge e1 = g.new MockEdge(n1, n2);
        MockEdge e2 = g.new MockEdge(n2, n3);
                
        g.add(e1);
        g.add(e2);

        assertTrue(g.isTree(n1));
        assertTrue(g.isTree(n2));
        assertTrue(g.isTree(n3));
        
        // Add two parents for n3.
        MockEdge e3 = g.new MockEdge(n1, n3);
        g.add(e3);        

        assertFalse(g.isTree(n1));
        assertFalse(g.isTree(n2));
        assertFalse(g.isTree(n3));

        // Remove the second parent.
        g.remove(e3);
        assertTrue(g.isTree(n1));

        // Add a cycle.
        MockEdge e4 = g.new MockEdge(n3, n1);
        g.add(e4);        

        assertFalse(g.isTree(n1));
        assertFalse(g.isTree(n2));
        assertFalse(g.isTree(n3));        
    }

}
