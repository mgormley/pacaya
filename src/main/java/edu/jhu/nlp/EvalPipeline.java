package edu.jhu.nlp;

import java.util.ArrayList;
import java.util.List;

import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

/**
 * Pipeline of Annotators each of which is optionally trainable.
 * 
 * @author mgormley
 */
public class EvalPipeline implements Evaluator {

    private List<Evaluator> pipeline = new ArrayList<>();
    
    public void add(Evaluator anno) {
        pipeline.add(anno);
    }
    
    @Override
    public void evaluate(AnnoSentenceCollection goldSents, AnnoSentenceCollection predSents, String name) {
        for (Evaluator anno : pipeline) {
            anno.evaluate(goldSents, predSents, name);
        }
    }

}
