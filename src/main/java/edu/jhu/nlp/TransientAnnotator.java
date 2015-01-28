package edu.jhu.nlp;

import java.util.Collections;
import java.util.Set;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.features.TemplateLanguage.AT;

/**
 * Annotator wrapper which skips serialization of its field.
 * @author mgormley
 */
public class TransientAnnotator implements Annotator, Trainable {

    private static final long serialVersionUID = 1L;
    private transient Annotator anno;
        
    public TransientAnnotator(Annotator wrappedAnnotator) {
        this.anno = wrappedAnnotator;
    }

    @Override
    public void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold,
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold) {
        if (anno != null && anno instanceof Trainable) {
            ((Trainable) anno).train(trainInput, trainGold, devInput, devGold);
        }
    }

    @Override
    public void annotate(AnnoSentenceCollection sents) {
        if (anno != null) {
            anno.annotate(sents);
        }
    }
    
    @Override
    public Set<AT> getAnnoTypes() {
        if (anno != null) {
            return anno.getAnnoTypes();
        }
        return Collections.emptySet();
    }

}
