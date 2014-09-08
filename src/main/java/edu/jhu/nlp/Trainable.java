package edu.jhu.nlp;

import edu.jhu.data.simple.AnnoSentenceCollection;

public interface Trainable {

    void train(AnnoSentenceCollection goldSents);
    
}
