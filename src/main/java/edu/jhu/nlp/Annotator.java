package edu.jhu.nlp;

import java.io.Serializable;
import java.util.Set;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;

/**
 * An annotator for a sentence.
 * 
 * @author mgormley
 */
public interface Annotator extends Serializable {

    /** Annotates each sentence in the collection. */
    void annotate(AnnoSentenceCollection sents);
    
    /** Gets the types of annotations that will be added to each sentence. */
    Set<AT> getAnnoTypes();
    
}
