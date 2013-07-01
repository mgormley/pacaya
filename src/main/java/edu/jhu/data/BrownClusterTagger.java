package edu.jhu.data;

import java.util.Arrays;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.regex.Pattern;

import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.util.Alphabet;

/**
 * Reads a brown clusters file and tags a sentence, by replacing each word with
 * its corresponding cluster.
 */
public class BrownClusterTagger {

    private static final Pattern tab = Pattern.compile("\t");
    
    /** Map from words to tags. */
    private HashMap<String,String> map;
    /** Maximum length for Brown cluster tag. */
    private int maxTagLength;
    /** Alphabet to use when constructing sentences. */
    private Alphabet<Label> alphabet;
    
    public BrownClusterTagger(Alphabet<Label> alphabet, int maxTagLength) {
        this.alphabet = alphabet;
	this.maxTagLength = maxTagLength;
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
            String cutCluster = cluster.substring(0, Math.min(cluster.length(), maxTagLength));
            map.put(word, cutCluster);
        }
    }

    /** Lowercases the input word, and looks up its cluster. */
    public String getCluster(String word) {
        String cluster = map.get(word);
        if (cluster == null) {
            cluster = "NONE";
        }
        return cluster;
    }
    
    public SentenceCollection getTagged(SentenceCollection sents) {
        SentenceCollection newSents = new SentenceCollection(alphabet);
        for (Sentence s : sents) {
            newSents.add(getTagged(s));
        }
        return newSents;
    }
    
    public Sentence getTagged(Sentence sent) {
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
