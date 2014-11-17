package edu.jhu.nlp;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.prim.util.Lambda.FnIntToVoid;
import edu.jhu.util.Threads;

public abstract class AbstractParallelAnnotator implements Annotator {

    @Override
    public void annotate(final AnnoSentenceCollection sents) {
        // Add the new predictions to each sentence.
        Threads.forEach(0, sents.size(), new FnIntToVoid() {            
            @Override
            public void call(int i) {
                annotate(sents.get(i));
            }
        });
    }

    public abstract void annotate(AnnoSentence sent);
    
}
