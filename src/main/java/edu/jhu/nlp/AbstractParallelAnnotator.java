package edu.jhu.nlp;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.prim.util.Lambda.FnIntToVoid;
import edu.jhu.util.Threads;

public abstract class AbstractParallelAnnotator implements Annotator {

    private static final Logger log = LoggerFactory.getLogger(AbstractParallelAnnotator.class);
    private static final long serialVersionUID = 1L;

    @Override
    public void annotate(final AnnoSentenceCollection sents) {
        // Add the new predictions to each sentence.
        Threads.forEach(0, sents.size(), new FnIntToVoid() {            
            @Override
            public void call(int i) {
                try {
                    annotate(sents.get(i));
                } catch (Throwable t) {
                    AbstractParallelAnnotator.logThrowable(log, t);
                }
            }
        });
    }

    public abstract void annotate(AnnoSentence sent);
    
    public static void logThrowable(Logger log, Throwable t) {
        String msg = (t.getMessage() == null) ? "" : ": " + t.getMessage();
        log.error("Failed to annotate sentence. Caught throwable: " + t.getClass() + msg);
        log.trace("Stacktrace from previous ERROR:\n", t);
    }
}
