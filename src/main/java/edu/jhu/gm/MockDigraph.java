package edu.jhu.gm;

import edu.jhu.gm.MockDigraph.MockEdge;
import edu.jhu.gm.MockDigraph.MockNode;

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
