package edu.jhu.srl;

import org.apache.log4j.Logger;

import edu.jhu.data.DepEdgeMask;
import edu.jhu.data.LabelSequence;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.prim.matrix.DenseIntegerMatrix;
import edu.jhu.util.Alphabet;

public class PosTagDistancePruner implements Trainable, Annotator {

    private static final Logger log = Logger.getLogger(PosTagDistancePruner.class);
    private Alphabet<String> alphabet = new Alphabet<String>();
    private DenseIntegerMatrix mat;
    
    public PosTagDistancePruner() { }
    
    @Override
    public void train(SimpleAnnoSentenceCollection sents) {
        for (SimpleAnnoSentence sent : sents) {
            // Populate the alphabet
            new LabelSequence<String>(alphabet, sent.getPosTags());
        }
        
        mat = new DenseIntegerMatrix(alphabet.size(), alphabet.size());
        mat.fill(0);
        // For each sentence...
        for (SimpleAnnoSentence sent : sents) {
            LabelSequence<String> tagSeq = new LabelSequence<String>(alphabet, sent.getPosTags());        
            int[] tags = tagSeq.getLabelIds();
            int[] parents = sent.getParents();            
            // For each observed dependency edge...
            for (int c=0; c<parents.length; c++) {
                int p = parents[c];
                if (p == -1) {
                    // We don't prune edges to the wall node.
                    continue;
                }
                int dist = Math.abs(p - c);
                if (dist > mat.get(tags[p], tags[c])) {
                    // Record the max dependency length for the observed parent
                    // / child tag types.
                    mat.set(tags[p], tags[c], dist);
                }
            }
        }
    }
    
    @Override
    public void annotate(SimpleAnnoSentenceCollection sents) {
        if (mat == null) {
            throw new IllegalStateException("The train() method must be called before annotate()");
        }
        int numEdgesTot = 0;
        int numEdgesKept = 0;
        // For each sentence...
        for (SimpleAnnoSentence sent : sents) {
            // Get existing DepEdgeMask or create a new one.
            DepEdgeMask mask = sent.getDepEdgeMask();
            if (mask == null) {
                mask = new DepEdgeMask(sent.size(), true);
                sent.setDepEdgeMask(mask);
            }
            
            LabelSequence<String> tagSeq = new LabelSequence<String>(alphabet, sent.getPosTags());        
            int[] tags = tagSeq.getLabelIds();

            // For each possible dependency edge (not including edges to the wall)...
            for (int i = 0; i < tags.length; i++) {
                for (int j = 0; j < tags.length; j++) {
                    if (tags[i] >= mat.getNumRows() || tags[j] >= mat.getNumRows()) {
                        // Don't prune unknown tags.
                        continue;
                    }
                    int dist = Math.abs(i - j);
                    if (dist > mat.get(tags[i], tags[j])) {
                        // Prune any edge for which the distance is longer than
                        // the longest observed distance for the parent / child
                        // tag types.
                        mask.setIsPruned(i, j, true);
                        if (log.isTraceEnabled()) {
                            log.trace(String.format("Pruned edge: parent=%s child=%s dist=%d",
                                    alphabet.lookupObject(tags[i]), alphabet.lookupObject(tags[j]), dist));
                        }
                    } else {
                        numEdgesKept++;
                    }
                    numEdgesTot++;
                }
            }
            // Count the edges to the wall, which will never be pruned.
            numEdgesTot += sent.size();
        }
        
        int numEdgesPruned = numEdgesTot - numEdgesKept;
        log.info(String.format("Pruned %d / %d = %f edges", numEdgesPruned, numEdgesTot, (double) numEdgesPruned / numEdgesTot));        
    }
    
}