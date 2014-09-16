package edu.jhu.hypergraph;

import java.util.List;

/** A hyperedge in a hypergraph. */
public class Hyperedge {

    private Hypernode headNode;
    private Hypernode[] tailNodes;
    private String label;
    private int id;
        
    public Hyperedge(int id, String label, Hypernode headNode, List<Hypernode> tailNodes) {
        this.headNode = headNode;
        this.tailNodes = tailNodes.toArray(new Hypernode[]{});
        this.label = label;
        this.id = id;
    }
    
    public Hyperedge(int id, String label, Hypernode headNode, Hypernode... tailNodes) {
        this.headNode = headNode;
        this.tailNodes = tailNodes;
        this.label = label;
        this.id = id;
    }

    public Hyperedge(int id, String label) {
        this.headNode = null;
        this.tailNodes = null;
        this.label = label;
        this.id = id;
    }

    /** Gets the consequent node for this edge. */
    public Hypernode getHeadNode() {
        return headNode;
    }

    /** Gets the list of antecedent nodes for this edge. */
    public Hypernode[] getTailNodes() {
        return tailNodes;
    }
    
    /** Gets a name for this edge. */
    public String getLabel() {
        if (label == null) {
            // Lazily construct an edge label from the head and tail nodes.
            StringBuilder label = new StringBuilder();
            label.append(headNode.getLabel());
            label.append("<--");
            for (Hypernode tailNode : tailNodes) {
                label.append(tailNode.getLabel());
                label.append(",");
            }
            if (tailNodes.length > 0) {
                label.deleteCharAt(label.length()-1);
            }
            return label.toString();
        }
        return label;
    }
    
    /** Gets a unique identifier for this edge within the hypergraph. */
    public int getId() {
        return id;
    }
    
    public String toString() {
        return label;
    }
    
    /* ------------------- Modifiers ---------------- */
    
    public void setHeadNode(Hypernode headNode) {
        this.headNode = headNode;
    }
    
    public void setLabel(String label) {
        this.label = label;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public void setTailNodes(Hypernode... tailNodes) {
        this.tailNodes = tailNodes;
    }

    public void clear() {
        this.headNode = null;
        this.label = null;
        this.id = -1;
        this.tailNodes = null;
    }
    
}
