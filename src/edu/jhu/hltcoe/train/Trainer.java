package edu.jhu.hltcoe.train;

import edu.jhu.hltcoe.data.SentenceCollection;
import edu.jhu.hltcoe.model.Model;

public interface Trainer {

    void train(SentenceCollection sentences);
    
    Model getModel();
    
}
