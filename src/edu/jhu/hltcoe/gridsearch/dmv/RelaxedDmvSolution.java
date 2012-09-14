package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;

public class RelaxedDmvSolution implements RelaxedSolution {

    private double score;
    private double[][] logProbs;
    private double[][] fracRoots;
    private double[][][] fracChildren;
    private RelaxStatus status;
    
    public RelaxedDmvSolution(double[][] logProbs, double[][] fracRoots, double[][][] fracChildren, double score, RelaxStatus status) {
        super();
        this.score = score;
        this.logProbs = logProbs;
        this.fracRoots = fracRoots;
        this.fracChildren = fracChildren;
        this.status = status;
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

    public RelaxStatus getStatus() {
        return status;
    }

}
