package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.Solution;

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

    public DepTreebank getDepTreebank() {
        return treebank;
    }

}
