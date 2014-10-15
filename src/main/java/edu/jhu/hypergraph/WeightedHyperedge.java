package edu.jhu.hypergraph;

import java.util.List;

public class WeightedHyperedge extends Hyperedge {

    private double weight;
    
    public WeightedHyperedge(int id, String label) {
        super(id, label);
    }

    public WeightedHyperedge(int id, String label, double weight, Hypernode headNode, Hypernode... tailNodes) {
        super(id, label, headNode, tailNodes);
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

}
