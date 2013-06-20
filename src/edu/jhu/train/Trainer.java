package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.model.Model;

public interface Trainer<C> {

    void train(TrainCorpus corpus);
    
    Model getModel();
    
}
