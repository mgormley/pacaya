package edu.jhu.hypergraph;


public class BasicHypernode implements Hypernode {

    private String label;
    private int id;
    
    public BasicHypernode(String label, int id) {
        this.label = label;
        this.id = id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public int getId() {
        return id;
    }

    public String toString() {
        return label;
    }
    
}
