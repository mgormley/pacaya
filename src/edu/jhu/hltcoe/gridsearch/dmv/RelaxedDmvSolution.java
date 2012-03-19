package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.gridsearch.Solution;

public class RelaxedDmvSolution implements Solution {

    private double score;
    private double[][] model;
    private double[][] fracRoots;
    private double[][][] fracChildren;
    
    public RelaxedDmvSolution(double[][] model, double[][] fracRoots, double[][][] fracChildren, double score) {
        super();
        this.score = score;
        this.model = model;
        this.fracRoots = fracRoots;
        this.fracChildren = fracChildren;
    }

    @Override
    public double getScore() {
        return score;
    }

    public double[][] getDmvModel() {
        return model;
    }
    
    public double[][] getFracRoots() {
        return fracRoots;
    }
    
    public double[][][] getFracChildren() {
        return fracChildren;
    }

}
