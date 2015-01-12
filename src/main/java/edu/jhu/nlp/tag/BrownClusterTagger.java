package edu.jhu.nlp.tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.nlp.AbstractParallelAnnotator;
import edu.jhu.nlp.Annotator;
import edu.jhu.nlp.data.Sentence;
import edu.jhu.nlp.data.SentenceCollection;
import edu.jhu.nlp.data.simple.AnnoSentence;
import edu.jhu.nlp.data.simple.AnnoSentenceCollection;
import edu.jhu.util.Alphabet;
import edu.jhu.util.Prm;

/**
 * Reads a brown clusters file and tags a sentence, by replacing each word with
 * its corresponding cluster.
 */
public class BrownClusterTagger extends AbstractParallelAnnotator implements Annotator {

    public static class BrownClusterTaggerPrm extends Prm {
        /** Maximum length for Brown cluster tag. */
        public int maxTagLength = Integer.MAX_VALUE;
        /**
         * Language (2-character code). If specified, the tagger will employ
         * language specific logic for UNKs.
         */
        public String language;
    }
    
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(BrownClusterTagger.class);
    private static final String OOV_CLUSTER = "UNK";
    private static final Pattern tab = Pattern.compile("\t");
    
    /** Map from words to tags. */
    private HashMap<String,String> map;
    private BrownClusterTaggerPrm prm;
    // Internal counting for miss rate.
    private AtomicInteger numLookups = new AtomicInteger(0);
    private AtomicInteger numMisses = new AtomicInteger(0);
    
    public BrownClusterTagger(BrownClusterTaggerPrm prm) {
        this.prm = prm;
        map = new HashMap<String,String>();
    }
    
    /** Constructor which also reads the brown clusters from the file. */
    public BrownClusterTagger(BrownClusterTaggerPrm prm, File brownClusters) {
        this(prm);
        read(brownClusters);
    }
    
    /** Read the Brown clusters from a file. */
    public void read(File brownClusters) {
        log.info("Reading Brown clusters from file: " + brownClusters);
        FileInputStream is;
        try {
            is = new FileInputStream(brownClusters);
            read(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    /** Read the Brown clusters from an input stream. */
    public void read(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            String[] splits = tab.split(line);
            String cluster = splits[0];
            String word = splits[1];
            String cutCluster = cluster.substring(0, Math.min(cluster.length(), prm.maxTagLength));
            map.put(word.intern(), cutCluster.intern());
        }
    }

    /** Looks up the Brown cluster for this word. */
    public String getCluster(String word) {
        String cluster = map.get(word);
        if (cluster == null && prm.language != null) {
            cluster = tryGetSubwordCluster(word);
        }
        if (cluster == null) {
            cluster = OOV_CLUSTER;
            numMisses.incrementAndGet();
        }
        numLookups.incrementAndGet();
        return cluster;
    }

    private String tryGetSubwordCluster(String word) {
        String[] subwords = null;
        if ("es".equals(prm.language) || "ca".equals(prm.language)) {
            if (word.contains("_")) {
                subwords = word.split("_");
            }
        } else if ("zh".equals(prm.language)) {
            if (word.length() > 1) {
                subwords = new String[word.length()];
                for (int i=0; i<subwords.length; i++) {
                    subwords[i] = Character.toString(word.charAt(i));
                }
            }
        }

        if (subwords != null) {
            for (String subword : subwords) {
                String cluster = map.get(subword);
                if (cluster != null) {
                    // Note: by adding "SW" as a prefix, we coarsen the cutoff
                    // version of these clusters.
                    return "SW" + cluster;
                }
            }
        }
        return null;
    }
    
    public double getHitRate() {
        return (double) (numLookups.get() - numMisses.get()) / numLookups.get();
    }
    
    @Override
    public void annotate(AnnoSentenceCollection sents) {
        super.annotate(sents);
        log.info("Brown cluster hit rate: " + getHitRate());                        
    }
    
    public void annotate(AnnoSentence sent) {
        ArrayList<String> clusters = new ArrayList<String>();
        for (String word : sent.getWords()) {
            clusters.add(getCluster(word));
        }
        sent.setClusters(clusters);
    }
    
    @Deprecated
    public SentenceCollection getTagged(SentenceCollection sents, Alphabet<String> alphabet) {
        SentenceCollection newSents = new SentenceCollection(alphabet);
        for (Sentence s : sents) {
            newSents.add(getTagged(s, alphabet));
        }
        return newSents;
    }
    
    @Deprecated
    public Sentence getTagged(Sentence sent, Alphabet<String> alphabet) {
        int[] labelIds = new int[sent.size()];
        int i=0; 
        for (String word : sent) {
            String cluster = getCluster(word);
            labelIds[i] = alphabet.lookupIndex(cluster);
            i++;
        }
        return new Sentence(alphabet, labelIds);        
    }
    
}
