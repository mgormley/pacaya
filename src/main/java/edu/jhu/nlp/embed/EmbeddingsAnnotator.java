package edu.jhu.nlp.embed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.embed.Embeddings.Scaling;
import edu.jhu.nlp.features.TemplateLanguage.AT;
import edu.jhu.util.Prm;
import edu.jhu.util.collections.Sets;

public class EmbeddingsAnnotator extends AbstractParallelAnnotator implements Annotator {

    public static class EmbeddingsAnnotatorPrm extends Prm {
        // Path to word embeddings text file.
        public File embeddingsFile = null;
        // Method for normalization of the embeddings.
        public Scaling embNorm = Scaling.L1_NORM;
        // Amount to scale embeddings after normalization.
        public double embScalar = 15.0;
    }
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(Embeddings.class);
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

    @Override
    public void annotate(AnnoSentenceCollection sents) {
        super.annotate(sents);
        log.info("Embeddings hit rate: " + getHitRate());                        
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
    
    @Override
    public Set<AT> getAnnoTypes() {
        return Sets.getSet(AT.EMBED);
    }

}
