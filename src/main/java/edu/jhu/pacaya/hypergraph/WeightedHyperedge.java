package edu.jhu.pacaya.hypergraph;

import java.util.List;

public class WeightedHyperedge extends Hyperedge {

    private static final long serialVersionUID = 1L;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(weight);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        WeightedHyperedge other = (WeightedHyperedge) obj;
        if (Double.doubleToLongBits(weight) != Double.doubleToLongBits(other.weight))
            return false;
        return true;
    }
    
}
