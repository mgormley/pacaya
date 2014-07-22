package edu.jhu.nlp;

import java.util.List;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.featurize.TemplateLanguage.AT;

/**
 * An annotator for a sentence.
 * 
 * @author mgormley
 */
public interface Annotator {

    void annotate(AnnoSentenceCollection sents);
    
}
