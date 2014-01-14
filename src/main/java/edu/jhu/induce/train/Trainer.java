package edu.jhu.induce.train;

import edu.jhu.induce.model.Model;

public interface Trainer<C> {

    void train(SemiSupervisedCorpus corpus);
    
    Model getModel();
    
}
