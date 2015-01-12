package edu.jhu.nlp;

import java.io.Serializable;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * An annotator for a sentence.
 * 
 * @author mgormley
 */
public interface Annotator extends Serializable {

    void annotate(AnnoSentenceCollection sents);
    
}
