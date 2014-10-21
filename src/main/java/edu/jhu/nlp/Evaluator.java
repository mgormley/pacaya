package edu.jhu.nlp;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

public interface Evaluator {
    void evaluate(AnnoSentenceCollection goldSents, AnnoSentenceCollection predSents, String name);
}
