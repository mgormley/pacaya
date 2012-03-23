package edu.jhu.hltcoe.gridsearch.dmv;

public class DmvBoundsDelta {

    public enum Lu { LOWER, UPPER }
    public enum Dir { ADD, SUBTRACT }  
    
    private int c;
    private int m; 
    private Lu lu;
    private Dir dir;
    private double delta;

    public DmvBoundsDelta(int c, int m, Lu lu, Dir dir, double delta) {
        super();
        this.c = c;
        this.m = m;
        this.lu = lu;
        this.dir = dir;
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

    public Dir getDir() {
        return dir;
    }
    
    public static DmvBoundsDelta getReverse(DmvBoundsDelta delta) {
        Dir dir = (delta.dir == Dir.ADD) ? Dir.SUBTRACT : Dir.ADD;
        return new DmvBoundsDelta(delta.c, delta.m, delta.lu, dir, delta.delta);
    }
}
