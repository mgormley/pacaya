package edu.jhu.nlp;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

public interface Evaluator {
    
    /** Gets the loss on an entire corpus. */
    double evaluate(AnnoSentenceCollection predSents, AnnoSentenceCollection goldSents, String name);
    
}
