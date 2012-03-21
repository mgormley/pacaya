package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.gridsearch.Solution;

public class RelaxedDmvSolution implements Solution {

    private double score;
    private double[][] logProbs;
    private double[][] fracRoots;
    private double[][][] fracChildren;
    
    public RelaxedDmvSolution(double[][] logProbs, double[][] fracRoots, double[][][] fracChildren, double score) {
        super();
        this.score = score;
        this.logProbs = logProbs;
        this.fracRoots = fracRoots;
        this.fracChildren = fracChildren;
    }

    @Override
    public double getScore() {
        return score;
    }

    public double[][] getLogProbs() {
        return logProbs;
    }
    
    public double[][] getFracRoots() {
        return fracRoots;
    }
    
    public double[][][] getFracChildren() {
        return fracChildren;
    }

}
