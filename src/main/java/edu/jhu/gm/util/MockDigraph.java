package edu.jhu.gm.util;

import edu.jhu.gm.util.MockDigraph.MockEdge;
import edu.jhu.gm.util.MockDigraph.MockNode;

public class MockDigraph extends DirectedGraph<MockNode,MockEdge> {
    public class MockEdge extends DirectedGraph<MockNode,MockEdge>.Edge {
        public MockEdge(MockNode parent, MockNode child) {
            super(parent, child);
        }
    }
    public class MockNode extends DirectedGraph<MockNode,MockEdge>.Node {
    }
    public void add(MockNode node) {
        super.add(node);
    }
    public void add(MockEdge edge) {
        super.add(edge);
    }
}
