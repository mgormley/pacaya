package edu.jhu.srl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import edu.berkeley.nlp.PCFGLA.smoothing.SrlBerkeleySignatureBuilder;
import edu.jhu.data.Label;
import edu.jhu.data.concrete.SimpleAnnoSentence;
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.prim.util.Utilities;
import edu.jhu.util.Alphabet;

/**
 * Extract corpus statistics about a CoNLL-2009 dataset.
 * 
 * @author mmitchell
 * @author mgormley
 */

public class CorpusStatistics implements Serializable {
    
    /**
     * Parameters for CorpusStatistics.
     */
    public static class CorpusStatisticsPrm implements Serializable {
        private static final long serialVersionUID = 1848012037725581753L;
        public boolean useGoldSyntax = false;
        public String language = "es";
        /**
         * Cutoff for OOV words. (This is actually used in CorpusStatistics, but
         * we'll just put it here for now.)
         */
        public int cutoff = 3;
        /**
         * Whether to normalize and clean words.
         */
        public boolean normalizeWords = false;
    }

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CorpusStatistics.class);
    
    public static final String UNKNOWN_ROLE = "argUNK";
    public static final String UNKNOWN_SENSE = "senseUNK";
    public static List<String> SENSES_FOR_UNK_PRED = Utilities.getList(UNKNOWN_SENSE); 

    public Set<String> knownWords = new HashSet<String>();
    public Set<String> knownUnks = new HashSet<String>();
    public Set<String> knownPostags = new HashSet<String>();

    public List<String> linkStateNames;
    public List<String> roleStateNames;
    private Set<String> knownRoles = new HashSet<String>();
    private Set<String> knownLinks = new HashSet<String>();
    // Mapping from predicate form to the set of predicate senses.
    public Map<String,List<String>> predSenseListMap = new HashMap<String,List<String>>();
    private Map<String,Set<String>> predSenseSetMap = new HashMap<String,Set<String>>();

    public int maxSentLength = 0;

    private Map<String, MutableInt> words = new HashMap<String, MutableInt>();
    private Map<String, MutableInt> unks = new HashMap<String, MutableInt>();

    public SrlBerkeleySignatureBuilder sig;
    public Normalizer normalize;

    public CorpusStatisticsPrm prm;
    private boolean initialized;
    
    public CorpusStatistics(CorpusStatisticsPrm prm) {
        this.prm = prm;
        this.normalize = new Normalizer(prm.normalizeWords);
        this.sig = new SrlBerkeleySignatureBuilder(new Alphabet<Label>());
        initialized = false;
    }

    public void init(Iterable<SimpleAnnoSentence> cr) {
        initialized = true;
                
        // Store the variable states we have seen before so
        // we know what our vocabulary of possible states are for
        // the Link variable. Applies to knownLinks, knownRoles.
        knownLinks.add("True");
        knownLinks.add("False");
        knownUnks.add("UNK");
        knownRoles.add(UNKNOWN_ROLE);
        // This is a hack:  '_' won't actually be in any of the defined edges.
        // However, removing this messes up what we assume as default.
        knownRoles.add("_");
        for (SimpleAnnoSentence sent : cr) {
            // Need to know max sent length because distance features
            // use these values explicitly; an unknown sentence length in
            // test data will result in an unknown feature.
            if (sent.size() > maxSentLength) {
                maxSentLength = sent.size();
            }
            for (SrlEdge edge : sent.getSrlGraph().getEdges()) {
                String role = edge.getLabel();
                knownRoles.add(role);
                int position = edge.getPred().getPosition();
                String lemma = sent.getLemma(position);
                Set<String> senses = predSenseSetMap.get(lemma);
                if (senses == null) {
                    senses = new TreeSet<String>();
                    predSenseSetMap.put(lemma, senses);
                }
                senses.add(edge.getPred().getLabel());
                
            }
            for (int position = 0; position < sent.size(); position++) {
                String wordForm = sent.getWord(position);
                String cleanWord = normalize.clean(wordForm);
                // Actually only need to do this for those words that are below
                // threshold for knownWords.  
                String unkWord = sig.getSignature(wordForm, position, prm.language);
                unkWord = normalize.escape(unkWord);
                words = addWord(words, cleanWord);
                unks = addWord(unks, unkWord);
                knownPostags.add(sent.getPosTag(position));
            }
        }
        
        // For words and unknown word classes, we only keep those above some threshold.
        knownWords = getUnigramsAboveThreshold(words, prm.cutoff);
        knownUnks = getUnigramsAboveThreshold(unks, prm.cutoff);
                    
        this.linkStateNames = new ArrayList<String>(knownLinks);
        this.roleStateNames =  new ArrayList<String>(knownRoles);
        for (Entry<String,Set<String>> entry : predSenseSetMap.entrySet()) {
            predSenseListMap.put(entry.getKey(), new ArrayList<String>(entry.getValue()));
        }
        
        log.info("Num known roles: " + roleStateNames.size());
        log.info("Known roles: " + roleStateNames);
        log.info("Num known predicates: " + predSenseListMap.size());
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


    private static Set<String> getUnigramsAboveThreshold(Map<String, MutableInt> inputHash, int cutoff) {
        Set<String> knownHash = new HashSet<String>();
        Iterator<Entry<String, MutableInt>> it = inputHash.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = it.next();
            int count = ((MutableInt) pairs.getValue()).get();
            if (count > cutoff) {
                knownHash.add((String) pairs.getKey());
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

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Gets the set of senses for the given predicate.
     */
    public List<String> getSenseStateNames(String predicate) {
        Set<String> senses = predSenseSetMap.get(predicate);
        if (senses == null) {
            return SENSES_FOR_UNK_PRED;
        } else {
            return new ArrayList<String>(senses);
        }
    }
    
}