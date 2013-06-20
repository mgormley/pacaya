package edu.jhu.model.dmv;

import edu.jhu.data.DepTreebank;
import edu.jhu.gridsearch.dmv.IndexedDmvModel;
import edu.jhu.model.Model;
import edu.jhu.train.MStep;
import edu.jhu.train.TrainCorpus;

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
