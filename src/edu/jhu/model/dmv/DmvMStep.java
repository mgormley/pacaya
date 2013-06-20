package edu.jhu.hltcoe.model.dmv;

import edu.jhu.hltcoe.data.DepTreebank;
import edu.jhu.hltcoe.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.hltcoe.model.Model;
import edu.jhu.hltcoe.train.MStep;
import edu.jhu.hltcoe.train.TrainCorpus;

public class DmvMStep implements MStep<DepTreebank> {

    private double lambda;
    
    public DmvMStep(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public Model getModel(TrainCorpus corpus, DepTreebank treebank) {
        return getModel(treebank);
    }
    
    public DmvModel getModel(DepTreebank treebank) {
        return IndexedDmvModel.getMleDmv(treebank, lambda);
    }

}
