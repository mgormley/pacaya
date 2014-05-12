package edu.jhu.srl;

import edu.jhu.data.simple.SimpleAnnoSentenceCollection;

public interface Trainable {

    void train(SimpleAnnoSentenceCollection sents);
    
}
