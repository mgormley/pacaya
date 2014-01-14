package edu.jhu.induce.model.dmv;

import edu.jhu.data.DepTreebank;
import edu.jhu.globalopt.dmv.IndexedDmvModel;
import edu.jhu.induce.model.Model;
import edu.jhu.induce.train.MStep;
import edu.jhu.induce.train.SemiSupervisedCorpus;

public class DmvMStep implements MStep<DepTreebank> {

    private double lambda;
    
    public DmvMStep(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public Model getModel(SemiSupervisedCorpus corpus, DepTreebank treebank, Model oldModel) {
        return getModel(treebank, (DmvModel) oldModel);
    }

    public Model getModel(SemiSupervisedCorpus corpus, DepTreebank treebank) {
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
