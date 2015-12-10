package edu.jhu.pacaya.hypergraph.depparse;

import edu.jhu.pacaya.hypergraph.BasicHypernode;

public class PCGBasicHypernode extends BasicHypernode {
    
    private static final long serialVersionUID = 1L;
    private int p;
    private int c;
    private int g;
    
    public PCGBasicHypernode(String label, int id, int p, int c, int g) {
        super(label, id);
        this.p = p;
        this.c = c;
        this.g = g;
    }
    
    public PCGBasicHypernode(String label, int id) {
        super(label, id);
        this.p = -2;
        this.c = -2;
        this.g = -2;
    }

    public int getP() {
        return p;
    }

    public int getC() {
        return c;
    }

    public int getG() {
        return g;
    }
    
    public void setPCG(int p, int c, int g) {
        this.p = p;
        this.c = c;
        this.g = g;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + c;
        result = prime * result + g;
        result = prime * result + p;
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
        PCGBasicHypernode other = (PCGBasicHypernode) obj;
        if (c != other.c)
            return false;
        if (g != other.g)
            return false;
        if (p != other.p)
            return false;
        return true;
    }

}