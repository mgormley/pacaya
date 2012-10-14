package edu.jhu.hltcoe.gridsearch.dmv;

public class CptBoundsDelta {

    public enum Lu { 
        LOWER, UPPER;
        public int getAsInt() {
            return (this == LOWER) ? 0 : 1;
        } 
    }
    public enum Dir { 
        ADD, SUBTRACT 
    }  
    
    private int c;
    private int m; 
    private Lu lu;
    private double delta;

    public CptBoundsDelta(int c, int m, Lu lu, double delta) {
        super();
        this.c = c;
        this.m = m;
        this.lu = lu;
        this.delta = delta;
    }

    public int getC() {
        return c;
    }

    public int getM() {
        return m;
    }

    public double getDelta() {
        return delta;
    }

    public Lu getLu() {
        return lu;
    }
    
    public static CptBoundsDelta getReverse(CptBoundsDelta delta) {
        return new CptBoundsDelta(delta.c, delta.m, delta.lu, -delta.delta);
    }
}
