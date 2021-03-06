package edu.jhu.pacaya.hypergraph;

import java.io.Serializable;
import java.util.Arrays;

/** A hyperedge in a hypergraph. */
// We implement serializable to allow for easy deep copies when unit testing.
public class Hyperedge implements Serializable {

    private static final long serialVersionUID = 1L;
    private Hypernode headNode;
    private Hypernode[] tailNodes;
    private String label;
    private int id;
       
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
        return getLabel();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((headNode == null) ? 0 : headNode.hashCode());
        result = prime * result + id;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
        result = prime * result + Arrays.hashCode(tailNodes);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Hyperedge other = (Hyperedge) obj;
        if (headNode == null) {
            if (other.headNode != null)
                return false;
        } else if (!headNode.equals(other.headNode))
            return false;
        if (id != other.id)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        if (!Arrays.equals(tailNodes, other.tailNodes))
            return false;
        return true;
    }
        
}
