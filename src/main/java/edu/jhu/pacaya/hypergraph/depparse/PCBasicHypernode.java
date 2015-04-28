package edu.jhu.pacaya.hypergraph.depparse;

import edu.jhu.pacaya.hypergraph.BasicHypernode;

public class PCBasicHypernode extends BasicHypernode {
    
    private static final long serialVersionUID = 1L;
    private int p;
    private int c;
    
    public PCBasicHypernode(String label, int id, int p, int c) {
        super(label, id);
        this.p = p;
        this.c = c;
    }
    
    public PCBasicHypernode(String label, int id) {
        super(label, id);
        this.p = -2;
        this.c = -2;
    }

    public int getP() {
        return p;
    }

    public int getC() {
        return c;
    }
    
    public void setPC(int p, int c) {
        this.p = p;
        this.c = c;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + c;
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
        PCBasicHypernode other = (PCBasicHypernode) obj;
        if (c != other.c)
            return false;
        if (p != other.p)
            return false;
        return true;
    }    
    
}