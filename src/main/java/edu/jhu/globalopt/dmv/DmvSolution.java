package edu.jhu.globalopt.dmv;

import edu.jhu.data.DepTreebank;
import edu.jhu.globalopt.Solution;

public class DmvSolution implements Solution {

    private double score;
    private DepTreebank treebank;
    private double[][] logProbs;
    private IndexedDmvModel idm;

    public DmvSolution(double[][] logProbs, IndexedDmvModel idm, DepTreebank treebank, double score) {
        this.score = score;
        this.logProbs = logProbs;
        this.idm = idm;
        this.treebank = treebank;
    }

    @Override
    public double getScore() {
        return score;
    }
    
    public double[][] getLogProbs() {
        return logProbs;
    }

    public IndexedDmvModel getIdm() {
        return idm;
    }

    public DepTreebank getTreebank() {
        return treebank;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int[][] getFeatCounts() {
        return idm.getTotFreqCm(treebank);
    }

}
