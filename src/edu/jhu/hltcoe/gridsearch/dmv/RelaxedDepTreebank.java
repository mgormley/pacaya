/**
 * 
 */
package edu.jhu.hltcoe.gridsearch.dmv;

public class RelaxedDepTreebank {
    private double[][] fracRoots;
    private double[][][] fracChildren;

    public RelaxedDepTreebank(double[][] fracRoots, double[][][] fracChildren) {
        this.fracRoots = fracRoots;
        this.fracChildren = fracChildren;
    }
    
    public double[][] getFracRoots() {
        return fracRoots;
    }
    
    public double[][][] getFracChildren() {
        return fracChildren;
    }
}