package edu.jhu.pacaya.hypergraph;


public class BasicHypernode implements Hypernode {

    private static final long serialVersionUID = 1L;
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((label == null) ? 0 : label.hashCode());
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
        BasicHypernode other = (BasicHypernode) obj;
        if (id != other.id)
            return false;
        if (label == null) {
            if (other.label != null)
                return false;
        } else if (!label.equals(other.label))
            return false;
        return true;
    }
    
}
