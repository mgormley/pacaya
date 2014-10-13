package edu.jhu.hypergraph.depparse;

import edu.jhu.hypergraph.BasicHypernode;

public class PCBasicHypernode extends BasicHypernode {

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
    
}