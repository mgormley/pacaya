package edu.jhu.nlp;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * An annotator for a sentence.
 * 
 * @author mgormley
 */
public interface Annotator {

    void annotate(AnnoSentenceCollection sents);
    
}
