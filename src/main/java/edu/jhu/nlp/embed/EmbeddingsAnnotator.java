package edu.jhu.nlp.embed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.embed.Embeddings.Scaling;

public class EmbeddingsAnnotator extends AbstractParallelAnnotator implements Annotator {

    public static class EmbeddingsAnnotatorPrm {
        // Path to word embeddings text file.
        public File embeddingsFile = null;
        // Method for normalization of the embeddings.
        public Scaling embNorm = Scaling.L1_NORM;
        // Amount to scale embeddings after normalization.
        public double embScalar = 15.0;
    }
    
    private static final long serialVersionUID = 1L;
    private EmbeddingsAnnotatorPrm prm;
    private Embeddings embeddings;
    // Internal counting for miss rate.
    private AtomicInteger numLookups = new AtomicInteger(0);
    private AtomicInteger numMisses = new AtomicInteger(0);
    
    public EmbeddingsAnnotator(EmbeddingsAnnotatorPrm prm) {
        this.prm = prm;
        this.embeddings = new Embeddings();
        try {
            embeddings.loadFromFile(prm.embeddingsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        embeddings.normPerWord(prm.embNorm);
        embeddings.scalePerWord(prm.embScalar);
    }
    
    public void annotate(AnnoSentence sent) {
        List<double[]> embeds = new ArrayList<>(sent.size());
        for (int i=0; i<sent.size(); i++) {
            String word = sent.getWord(i);
            embeds.add(embeddings.getEmbedding(word));
            if (embeds.get(i) == null) {
                numMisses.incrementAndGet();
            }
            numLookups.incrementAndGet();
        }
        sent.setEmbeds(embeds);
    }

    public double getHitRate() {
        return (double) (numLookups.get() - numMisses.get()) / numLookups.get();
    }

}
