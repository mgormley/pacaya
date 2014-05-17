package edu.jhu.srl;

import edu.jhu.data.simple.AnnoSentenceCollection;

public interface Trainable {

    void train(AnnoSentenceCollection sents);
    
}
