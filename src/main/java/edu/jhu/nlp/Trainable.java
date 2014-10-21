package edu.jhu.nlp;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

public interface Trainable {

    void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold, 
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold);
    
}
