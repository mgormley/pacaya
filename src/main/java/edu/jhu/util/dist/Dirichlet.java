package edu.jhu.util.dist;

import java.util.Arrays;

public class Dirichlet {

    private static final double EPSILON = Double.MIN_VALUE;

    private double[] alpha;
    
    public Dirichlet(double alphaVal, int k) {
        this.alpha = new double[k];
        Arrays.fill(this.alpha, alphaVal);
    }
    
    public Dirichlet(double[] alpha) {
        this.alpha = alpha;
    }
    
    public double[] draw() {
        return staticDraw(alpha);
    }
    
    public static double[] staticDraw(double[] alpha) {
        throw new RuntimeException("Not supported in public release due to licensing restrictions.");
    }
    
}
