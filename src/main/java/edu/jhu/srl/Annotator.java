package edu.jhu.srl;

import java.util.List;

import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.featurize.TemplateLanguage.AT;

/**
 * An annotator for a sentence.
 * 
 * @author mgormley
 */
public interface Annotator {

    void annotate(SimpleAnnoSentenceCollection sents);
    
}
