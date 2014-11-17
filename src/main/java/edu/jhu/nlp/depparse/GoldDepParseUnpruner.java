package edu.jhu.nlp.depparse;

import org.apache.log4j.Logger;

import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.Trainable;
import edu.jhu.nlp.data.DepEdgeMask;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;

public class GoldDepParseUnpruner implements Trainable, Annotator {

    private static final Logger log = Logger.getLogger(GoldDepParseUnpruner.class);

    @Override
    public void annotate(AnnoSentenceCollection sents) {
        // Do nothing. This annotator is only used to manipulate training parses.
    }

    @Override
    public void train(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold,
            AnnoSentenceCollection devInput, AnnoSentenceCollection devGold) {
        log.info("Ensuring that the gold trees in the training data are not pruned.");
        unprune(trainInput, trainGold);
    }

    protected void unprune(AnnoSentenceCollection trainInput, AnnoSentenceCollection trainGold) {
        for (int i=0; i<trainGold.size(); i++) {
            AnnoSentence gSent = trainGold.get(i);
            AnnoSentence iSent = trainInput.get(i);
            if (gSent.getParents() != null && iSent.getDepEdgeMask() != null) {
                int[] gParents = gSent.getParents();
                DepEdgeMask iMask = iSent.getDepEdgeMask();
                for (int c=0; c<gParents.length; c++) {
                    int p = gParents[c];
                    iMask.setIsPruned(p, c, false);
                }
            }
        }
    }

}
