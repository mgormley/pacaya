package edu.jhu.nlp.embed;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.jhu.data.simple.AnnoSentence;
import edu.jhu.data.simple.AnnoSentenceCollection;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.embed.Embeddings.Scaling;

public class EmbeddingsAnnotator implements Annotator {

    public static class EmbeddingsAnnotatorPrm {
        // Path to word embeddings text file.
        public File embeddingsFile = null;
        // Method for normalization of the embeddings.
        public Scaling embNorm = Scaling.L1_NORM;
        // Amount to scale embeddings after normalization.
        public double embScaler = 15.0;
    }
    
    private EmbeddingsAnnotatorPrm prm;
    private Embeddings embeddings;
    private int numLookups = 0;
    private int numMisses = 0;
    
    public EmbeddingsAnnotator(EmbeddingsAnnotatorPrm prm) {
        this.prm = prm;
        this.embeddings = new Embeddings();
        try {
            embeddings.loadFromFile(prm.embeddingsFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        embeddings.normPerWord(prm.embNorm);
        embeddings.scalePerWord(prm.embScaler);
    }
    
    @Override
    public void annotate(AnnoSentenceCollection sents) {
        for (AnnoSentence sent : sents) {
            List<double[]> embeds = new ArrayList<>(sent.size());
            for (int i=0; i<sent.size(); i++) {
                String word = sent.getWord(i);
                embeds.add(embeddings.getEmbedding(word));
                if (embeds.get(i) == null) {
                    numMisses++;
                }
                numLookups++;
            }
            sent.setEmbeds(embeds);
        }
    }

    public double getHitRate() {
        return (double) (numLookups - numMisses) / numLookups;
    }

}
