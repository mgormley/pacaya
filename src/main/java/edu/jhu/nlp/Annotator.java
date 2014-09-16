package edu.jhu.nlp;

import java.util.List;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;

/**
 * An annotator for a sentence.
 * 
 * @author mgormley
 */
public interface Annotator {

    void annotate(AnnoSentenceCollection sents);
    
}
