package edu.jhu.tag;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import edu.jhu.data.Label;
import edu.jhu.data.Sentence;
import edu.jhu.data.SentenceCollection;
import edu.jhu.data.TaggedWord;
import edu.jhu.data.Word;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.data.simple.SimpleAnnoSentenceCollection;
import edu.jhu.util.Alphabet;

/**
 * Reads a brown clusters file and tags a sentence, by replacing each word with
 * its corresponding cluster.
 */
public class BrownClusterTagger {

    public static class BrownClusterTaggerPrm {
        /** Maximum length for Brown cluster tag. */
        public int maxTagLength = Integer.MAX_VALUE;
        /**
         * Language (2-character code). If specified, the tagger will employ
         * language specific logic for UNKs.
         */
        public String language;        
    }
    
    private static final String OOV_CLUSTER = "UNK";
    private static final Pattern tab = Pattern.compile("\t");
    
    /** Map from words to tags. */
    private HashMap<String,String> map;
    private BrownClusterTaggerPrm prm;
    // Internal counting for miss rate.
    private int numLookups = 0;
    private int numMisses = 0;
    
    public BrownClusterTagger(BrownClusterTaggerPrm prm) {
        this.prm = prm;
        map = new HashMap<String,String>();
    }
    
    public void read(File brownClusters) throws IOException {
        FileInputStream is = new FileInputStream(brownClusters);
        read(is);
    }
    
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
            numMisses++;
        }
        numLookups++;
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
        return (double) (numLookups - numMisses) / numLookups;
    }

    public void addClusters(SimpleAnnoSentenceCollection sents) {
        for (SimpleAnnoSentence s : sents) {
            addClusters(s);
        }
    }

    public void addClusters(SimpleAnnoSentence sent) {
        ArrayList<String> clusters = new ArrayList<String>();
        for (String word : sent.getWords()) {
            clusters.add(getCluster(word));
        }
        sent.setClusters(clusters);
    }
    
    @Deprecated
    public SentenceCollection getTagged(SentenceCollection sents, Alphabet<Label> alphabet) {
        SentenceCollection newSents = new SentenceCollection(alphabet);
        for (Sentence s : sents) {
            newSents.add(getTagged(s, alphabet));
        }
        return newSents;
    }
    
    @Deprecated
    public Sentence getTagged(Sentence sent, Alphabet<Label> alphabet) {
        int[] labelIds = new int[sent.size()];
        int i=0; 
        for (Label l : sent) {
            String word;
            if (l instanceof Word) {
                word = ((Word)l).getLabel();
            } else if (l instanceof TaggedWord) {
                word = ((TaggedWord)l).getWord();
            } else {
                throw new RuntimeException("Unable to tag labels of type: " + l.getClass());
            }

            String cluster = getCluster(word);
            labelIds[i] = alphabet.lookupIndex(new TaggedWord(word, cluster));
            i++;
        }
        return new Sentence(alphabet, labelIds);        
    }
    
}
