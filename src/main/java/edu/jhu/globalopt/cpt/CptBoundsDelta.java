package edu.jhu.globalopt.cpt;

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
    public enum Type {
        PARAM, COUNT;
        public int getAsInt() {
            return (this == PARAM) ? 0 : 1;
        } 
    }
    
    private int c;
    private int m; 
    private Lu lu;
    private double delta;
    private Type type;

    public CptBoundsDelta(Type type, int c, int m, Lu lu, double delta) {
        super();
        this.type = type;
        this.c = c;
        this.m = m;
        this.lu = lu;
        this.delta = delta;
    }

    public Type getType() {
        return type;
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
        return new CptBoundsDelta(delta.type, delta.c, delta.m, delta.lu, -delta.delta);
    }

    @Override
    public String toString() {
        return "CptBoundsDelta [c=" + c + ", m=" + m + ", delta=" + delta + ", lu=" + lu + ", type=" + type + "]";
    }    
}
