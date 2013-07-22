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
    public Model getModel(TrainCorpus corpus, DepTreebank treebank, Model oldModel) {
        return getModel(treebank, (DmvModel) oldModel);
    }

    public Model getModel(TrainCorpus corpus, DepTreebank treebank) {
        return getModel(treebank);
    }
    
    /**
     * Updates and returns the old model.
     */
    public DmvModel getModel(DepTreebank treebank, DmvModel oldModel) {
        IndexedDmvModel.getMleDmv(treebank, lambda, oldModel);
        return oldModel;
    }

    public DmvModel getModel(DepTreebank treebank) {
        return IndexedDmvModel.getMleDmv(treebank, lambda);
    }

}
