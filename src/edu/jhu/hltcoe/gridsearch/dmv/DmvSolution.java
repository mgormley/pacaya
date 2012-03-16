package edu.jhu.hltcoe.gridsearch.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.Solution;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.model.dmv.DmvModel;

public class DmvSolution implements Solution {

    private double score;
    private DmvModel model;
    private DepTreebank treebank;
    
    public DmvSolution(DmvModel model, DepTreebank treebank, double score) {
        super();
        this.score = score;
        this.model = model;
        this.treebank = treebank;
    }

    @Override
    public double getScore() {
        return score;
    }

    public Model getDmvModel() {
        return model;
    }
    
    public DepTreebank getDepTreebank() {
        return treebank;
    }

}
