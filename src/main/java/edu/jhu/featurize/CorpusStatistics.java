package edu.jhu.featurize;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
import edu.jhu.data.conll.CoNLL09FileReader;
import edu.jhu.data.conll.CoNLL09Sentence;
import edu.jhu.data.conll.CoNLL09Token;
import edu.jhu.featurize.SrlFgExampleBuilder.SrlFgExampleBuilderPrm;
import edu.jhu.gm.Feature;
import edu.jhu.gm.FgExample;
import edu.jhu.util.Alphabet;
import edu.jhu.util.cli.Opt;

//import com.google.common.collect.Maps;

/**
 * Extract corpus statistics about a CoNLL-2009 dataset.
 * 
 * @author mmitchell
 * @author mgormley
 */

public class CorpusStatistics {

    private static final Logger log = Logger.getLogger(CorpusStatistics.class);

    @Opt(name = "language", hasArg = true, description = "Language (en or es).")
    public static String language = "es";
    @Opt(name = "cutoff", hasArg = true, description = "Cutoff for OOV words.")
    public static int cutoff = 3;
    @Opt(name = "use-PHEAD", hasArg = false, description = "Use Predicted HEAD rather than Gold HEAD.")
    public static boolean goldHead = true;

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

    public void init(Iterable<CoNLL09Sentence> cr) {
        if (true) throw new  RuntimeException("DO SOMETHING ABOUT THE HARDCODED OPTS ABOVE");
        
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
                    String[] splitRole = word.getApreds().get(j).split("-");
                    String role = splitRole[0].toLowerCase();
                    knownRoles.add(role);
                }
                String wordForm = word.getForm();
                String cleanWord = Normalize.clean(wordForm);
                int position = word.getId() - 1;
                String unkWord = sig.getSignature(wordForm, position, language);
                unkWord = Normalize.escape(unkWord);
                words = addWord(words, cleanWord);
                unks = addWord(unks, unkWord);
                // Learn what Postags are in our vocabulary
                // Later, can then back off to NONE if we haven't seen it
                // before.
                if (!goldHead) {
                    knownPostags.add(word.getPpos());
                } else {
                    knownPostags.add(word.getPos());
                }
                // Note: These are not actually bigrams, but rather refer to
                // pairs of words seen in the same sentence.
                for (CoNLL09Token word2 : sent) {
                    String wordForm2 = word2.getForm();
                    String cleanWord2 = Normalize.clean(wordForm2);
                    String unkWord2 = sig.getSignature(wordForm2, word2.getId(), language);
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
        knownWords = getUnigramsAboveThreshold(words, cutoff);
        knownUnks = getUnigramsAboveThreshold(unks, cutoff);
        
        // TODO: Currently not actually using bigram dictionary.
        // knownBigrams = getBigramsAboveThreshold(bigrams, cutoff);
                    
        this.linkStateNames = new ArrayList<String>(knownLinks);
        this.roleStateNames =  new ArrayList<String>(knownRoles);
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

    public static class Normalize {
        private static final Pattern digits = Pattern.compile("\\d+");
        private static final Pattern punct = Pattern.compile("[^A-Za-z0-9_ÁÉÍÓÚÜÑáéíóúüñ]");
        private static Map<String, String> stringMap;

        private Normalize() {
            // Private constructor.
        }

        public static String escape(String s) {
            Iterator<Entry<String, String>> it = stringMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry entry = (Map.Entry) it.next();
                String key = (String) entry.getKey();
                String val = (String) entry.getValue();
                s.replace(key, val);
            }
            return punct.matcher(s).replaceAll("_");
        }

        public static String norm_digits(String s) {
            return digits.matcher(s).replaceAll("0");
        }

        public static String clean(String s) {
            s = escape(s);
            s = norm_digits(s.toLowerCase());
            return s;
        }

        private static void setChars() {
            // Really this would be nicer using guava...
            stringMap = new HashMap<String, String>();
            stringMap.put("1", "2");
            stringMap.put(".", "_P_");
            stringMap.put(",", "_C_");
            stringMap.put("'", "_A_");
            stringMap.put("%", "_PCT_");
            stringMap.put("-", "_DASH_");
            stringMap.put("$", "_DOL_");
            stringMap.put("&", "_AMP_");
            stringMap.put(":", "_COL_");
            stringMap.put(";", "_SCOL_");
            stringMap.put("\\/", "_BSL_");
            stringMap.put("/", "_SL_");
            stringMap.put("`", "_QT_");
            stringMap.put("?", "_Q_");
            stringMap.put("¿", "_QQ_");
            stringMap.put("=", "_EQ_");
            stringMap.put("*", "_ST_");
            stringMap.put("!", "_E_");
            stringMap.put("¡", "_EE_");
            stringMap.put("#", "_HSH_");
            stringMap.put("@", "_AT_");
            stringMap.put("(", "_LBR_");
            stringMap.put(")", "_RBR_");
            stringMap.put("\"", "_QT1_");
            stringMap.put("Á", "_A_ACNT_");
            stringMap.put("É", "_E_ACNT_");
            stringMap.put("Í", "_I_ACNT_");
            stringMap.put("Ó", "_O_ACNT_");
            stringMap.put("Ú", "_U_ACNT_");
            stringMap.put("Ü", "_U_ACNT2_");
            stringMap.put("Ñ", "_N_ACNT_");
            stringMap.put("á", "_a_ACNT_");
            stringMap.put("é", "_e_ACNT_");
            stringMap.put("í", "_i_ACNT_");
            stringMap.put("ó", "_o_ACNT_");
            stringMap.put("ú", "_u_ACNT_");
            stringMap.put("ü", "_u_ACNT2_");
            stringMap.put("ñ", "_n_ACNT_");
            stringMap.put("º", "_deg_ACNT_");

        }
    }

}