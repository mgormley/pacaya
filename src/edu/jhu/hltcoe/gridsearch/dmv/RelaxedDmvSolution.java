package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.gridsearch.RelaxStatus;
import edu.jhu.hltcoe.gridsearch.RelaxedSolution;

public class RelaxedDmvSolution implements RelaxedSolution {

    private double score;
    private double[][] logProbs;
    private RelaxStatus status;
    private RelaxedDepTreebank treebank;
    
    public RelaxedDmvSolution(double[][] logProbs, double[][] fracRoots, double[][][] fracChildren, double score, RelaxStatus status) {
        super();
        this.score = score;
        this.logProbs = logProbs;
        this.treebank = new RelaxedDepTreebank(fracRoots, fracChildren);
        this.status = status;
    }

    @Override
    public double getScore() {
        return score;
    }

    public double[][] getLogProbs() {
        return logProbs;
    }

    public RelaxStatus getStatus() {
        return status;
    }

    public RelaxedDepTreebank getTreebank() {
        return treebank;
    }

}
