package edu.jhu.train;

import edu.jhu.model.Model;

public interface Trainer<C> {

    void train(TrainCorpus corpus);
    
    Model getModel();
    
}
