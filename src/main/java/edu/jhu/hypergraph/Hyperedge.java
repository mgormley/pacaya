package edu.jhu.hypergraph;

import java.util.List;

/** A hyperedge in a hypergraph. */
public class Hyperedge {

    private Hypernode headNode;
    private List<Hypernode> tailNodes;
    private String label;
    private int id;
        
    public Hyperedge(Hypernode headNode, List<Hypernode> tailNodes, String label, int id) {
        this.headNode = headNode;
        this.tailNodes = tailNodes;
        this.label = label;
        this.id = id;
    }

    /** Gets the consequent node for this edge. */
    public Hypernode getHeadNode() {
        return headNode;
    }

    /** Gets the list of antecedent nodes for this edge. */
    public List<Hypernode> getTailNodes() {
        return tailNodes;
    }
    
    /** Gets a name for this edge. */
    public String getLabel() {
        return label;
    }
    
    /** Gets a unique identifier for this edge within the hypergraph. */
    public int getId() {
        return id;
    }
    
}
