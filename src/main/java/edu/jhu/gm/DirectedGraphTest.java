package edu.jhu.gm;

import static org.junit.Assert.*;

import org.junit.Test;

import edu.jhu.gm.DirectedGraphTest.MockDigraph.MockEdge;
import edu.jhu.gm.DirectedGraphTest.MockDigraph.MockNode;

public class DirectedGraphTest {

    public static class MockDigraph extends DirectedGraph<MockNode,MockEdge> {
        public class MockEdge extends DirectedGraph<MockNode,MockEdge>.Edge {
            public MockEdge(MockNode parent, MockNode child) {
                super(parent, child);
            }
        }
        public class MockNode extends DirectedGraph<MockNode,MockEdge>.Node {
        }
    }

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
