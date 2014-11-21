package edu.jhu.nlp;

import org.apache.log4j.Logger;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.prim.util.Lambda.FnIntToVoid;
import edu.jhu.util.Threads;

public abstract class AbstractParallelAnnotator implements Annotator {

    private static final Logger log = Logger.getLogger(AbstractParallelAnnotator.class);

    @Override
    public void annotate(final AnnoSentenceCollection sents) {
        // Add the new predictions to each sentence.
        Threads.forEach(0, sents.size(), new FnIntToVoid() {            
            @Override
            public void call(int i) {
                try {
                    annotate(sents.get(i));
                } catch (Throwable t) {
                    // TODO: Maybe move this elsewhere.
                    log.error("Caught throwable: " + t.getMessage());
                    t.printStackTrace();
                }
            }
        });
    }

    public abstract void annotate(AnnoSentence sent);
    
}
