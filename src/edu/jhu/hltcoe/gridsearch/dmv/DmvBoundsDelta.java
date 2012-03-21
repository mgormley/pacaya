package edu.jhu.hltcoe.gridsearch.dmv;

public class DmvBoundsDelta {

    private int c;
    private int m; 
    private double lbDelta; 
    private double ubDelta;
    
    public DmvBoundsDelta(int c, int m, double lbDelta, double ubDelta) {
        super();
        this.c = c;
        this.m = m;
        this.lbDelta = lbDelta;
        this.ubDelta = ubDelta;
    }

    public int getC() {
        return c;
    }

    public int getM() {
        return m;
    }

    public double getLbDelta() {
        return lbDelta;
    }

    public double getUbDelta() {
        return ubDelta;
    }
    
}
