package edu.jhu.nlp;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * Pipeline of Annotators each of which is optionally trainable.
 * 
 * @author mgormley
 */
public class AnnoPipeline implements Trainable, Annotator {

    private List<Annotator> pipeline = new ArrayList<Annotator>();
    
    public void add(Annotator anno) {
        pipeline.add(anno);
    }
    
    @Override
    public void train(AnnoSentenceCollection inputSents, AnnoSentenceCollection goldSents) {
        for (Annotator anno : pipeline) {
            if (anno instanceof Trainable) {
                ((Trainable) anno).train(inputSents, goldSents);
            }
            anno.annotate(inputSents);
        }
    }
    
    @Override
    public void annotate(AnnoSentenceCollection sents) {
        for (Annotator anno : pipeline) {
            anno.annotate(sents);
        }
    }
    
    

}
