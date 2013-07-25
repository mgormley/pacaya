package edu.jhu.srl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import edu.berkeley.nlp.PCFGLA.smoothing.BerkeleySignatureBuilder;
import edu.jhu.data.Label;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SentFeatureExtractor.SentFeatureExtractorPrm;
import edu.jhu.util.Alphabet;

/**
 * Extract corpus statistics about a CoNLL-2009 dataset.
 * 
 * @author mmitchell
 * @author mgormley
 */

public class CorpusStatistics {

    private static final Logger log = Logger.getLogger(CorpusStatistics.class);

    public Set<String> knownWords = new HashSet<String>();
    public Set<String> knownUnks = new HashSet<String>();
    public Set<String> knownPostags = new HashSet<String>();

    public List<String> linkStateNames;
    public List<String> roleStateNames;
    public Set<String> knownRoles = new HashSet<String>();
    public Set<String> knownLinks = new HashSet<String>();

    public Integer maxSentLength = 0;

    private Map<String, MutableInt> words = new HashMap<String, MutableInt>();
    private Map<String, MutableInt> unks = new HashMap<String, MutableInt>();

    public BerkeleySignatureBuilder sig = new BerkeleySignatureBuilder(new Alphabet<Label>());
    public Normalizer normalize;

    private SentFeatureExtractorPrm prm;
    
    public static final Pattern dash = Pattern.compile("-");

    public CorpusStatistics(SentFeatureExtractorPrm prm) {
        this.prm = prm;
        this.normalize = new Normalizer(prm.normalizeWords); 
    }

    public void init(Iterable<CoNLL09Sentence> cr) {
        // TODO: Currently, we build but just discard the bigrams map.
        Map<Set<String>, MutableInt> bigrams = new HashMap<Set<String>, MutableInt>();
        
        // Store the variable states we have seen before so
        // we know what our vocabulary of possible states are for
        // the Link variable. Applies to knownLinks, knownRoles.
        knownLinks.add("True");
        knownLinks.add("False");
        knownUnks.add("UNK");
        for (CoNLL09Sentence sent : cr) {
            // Need to know max sent length because distance features
            // use these values explicitly; an unknown sentence length in
            // test data will result in an unknown feature.
            if (sent.size() > maxSentLength) {
                maxSentLength = sent.size();
            }
            for (int i = 0; i < sent.size(); i++) {
                CoNLL09Token word = sent.get(i);
                for (int j = 0; j < word.getApreds().size(); j++) {  
                    String role = word.getApreds().get(j);
                    knownRoles.add(role);
                }
                String wordForm = word.getForm();
                String cleanWord = normalize.clean(wordForm);
                int position = word.getId() - 1;
                String unkWord = sig.getSignature(wordForm, position, prm.language);
                unkWord = normalize.escape(unkWord);
                words = addWord(words, cleanWord);
                unks = addWord(unks, unkWord);
                // Learn what Postags are in our vocabulary
                // Later, can then back off to NONE if we haven't seen it
                // before.
                if (!prm.useGoldSyntax) {
                    knownPostags.add(word.getPpos());
                } else {
                    knownPostags.add(word.getPos());
                }
                // Note: These are not actually bigrams, but rather refer to
                // pairs of words seen in the same sentence.
                for (CoNLL09Token word2 : sent) {
                    String wordForm2 = word2.getForm();
                    String cleanWord2 = normalize.clean(wordForm2);
                    String unkWord2 = sig.getSignature(wordForm2, word2.getId(), prm.language);
                    unkWord2 = normalize.escape(unkWord2);

                    // TBD: Actually use the seen/unseen bigrams to shrink the
                    // feature space.
                    addBigram(bigrams, cleanWord, cleanWord2);
                    addBigram(bigrams, unkWord, unkWord2);
                    addBigram(bigrams, cleanWord, unkWord2);
                    addBigram(bigrams, unkWord, cleanWord2);
                }
            }
        }
        
        // For words and unknown word classes, we only keep those above some threshold.
        knownWords = getUnigramsAboveThreshold(words, prm.cutoff);
        knownUnks = getUnigramsAboveThreshold(unks, prm.cutoff);
        
        // TODO: Currently not actually using bigram dictionary.
        // knownBigrams = getBigramsAboveThreshold(bigrams, cutoff);
                    
        this.linkStateNames = new ArrayList<String>(knownLinks);
        this.roleStateNames =  new ArrayList<String>(knownRoles);
        
        log.info("Num known roles: " + roleStateNames.size());
        log.info("Known roles: " + roleStateNames);
    }

    // ------------------- Data Munging ------------------- //

    // TODO: These methods should move elsewhere.
    public static void normalizeRoleNames(List<CoNLL09Sentence> sents) {
        for (CoNLL09Sentence sent : sents) {
            for (CoNLL09Token tok : sent) {
                ArrayList<String> apreds = new ArrayList<String>();
                for (String apred : tok.getApreds()) {
                    if ("_".equals(apred)) {
                        apreds.add(apred);
                    } else { 
                        apreds.add(normalizeRoleName(apred));
                    }
                }
                tok.setApreds(apreds);
            }
        }
    }
    
    private static String normalizeRoleName(String role) {
        String[] splitRole = dash.split(role);
        return splitRole[0].toLowerCase();
    }

    // ------------------- private ------------------- //

    private static Map<String, MutableInt> addWord(Map<String, MutableInt> inputHash, String w) {
        MutableInt count = inputHash.get(w);
        if (count == null) {
            inputHash.put(w, new MutableInt());
        } else {
            count.increment();
        }
        return inputHash;
    }

    private static void addBigram(Map<Set<String>, MutableInt> bigrams, String w1, String w2) {
        HashSet<String> pair = new HashSet<String>(Arrays.asList(w1, w2));
        MutableInt count = bigrams.get(pair);
        if (count == null) {
            bigrams.put(pair, new MutableInt());
        } else {
            count.increment();
        }
    }

    private static Set<String> getUnigramsAboveThreshold(Map<String, MutableInt> inputHash, int cutoff) {
        Set<String> knownHash = new HashSet<String>();
        Iterator<Entry<String, MutableInt>> it = inputHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            int count = ((MutableInt) pairs.getValue()).get();
            if (count > cutoff) {
                knownHash.add((String) pairs.getKey());
            }
        }
        return knownHash;
    }

    private static Set<Set<String>> getBigramsAboveThreshold(Map<Set<String>, MutableInt> inputHash, int cutoff) {
        // Version of below, for bigrams.
        Set<Set<String>> knownHash = new HashSet<Set<String>>();
        Iterator<Entry<Set<String>, MutableInt>> it = inputHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            int count = ((MutableInt) pairs.getValue()).get();
            if (count > cutoff) {
                knownHash.add((Set<String>) pairs.getKey());
            }
        }
        return knownHash;
    }

    @Override
    public String toString() {
        return "CorpusStatistics [\n     knownWords=" + knownWords + ",\n     knownUnks=" + knownUnks
                + ",\n     knownPostags=" + knownPostags + ",\n     linkStateNames=" + linkStateNames
                + ",\n     roleStateNames=" + roleStateNames + ",\n     knownRoles=" + knownRoles
                + ",\n     knownLinks=" + knownLinks + ",\n     maxSentLength=" + maxSentLength + ",\n     words="
                + words + ",\n     unks=" + unks + "]";
    }
    
}