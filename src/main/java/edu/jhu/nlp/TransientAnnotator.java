package edu.jhu.nlp;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

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

}
