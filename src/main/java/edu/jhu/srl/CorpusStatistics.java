package edu.jhu.srl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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
import edu.jhu.data.conll.SrlGraph.SrlEdge;
import edu.jhu.data.conll.SrlGraph.SrlPred;
import edu.jhu.data.simple.SimpleAnnoSentence;
import edu.jhu.prim.tuple.ComparablePair;
import edu.jhu.prim.tuple.Pair;
import edu.jhu.util.Alphabet;
import edu.jhu.util.collections.Lists;

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
        // TODO: Remove useGoldSyntax since it's no longer used in CorpusStatistics.
        public boolean useGoldSyntax = false;
        public String language = "es";
        /** Cutoff for OOV words. */
        public int cutoff = 3;
        /** Cutoff for topN words. */ 
        public int topN = 800;
        /**
         * Whether to normalize and clean words.
         */
        public boolean normalizeWords = false;
    }

    private static final long serialVersionUID = 1L;
    private static final Logger log = Logger.getLogger(CorpusStatistics.class);
    
    public static final String UNKNOWN_ROLE = "argUNK";
    public static final String UNKNOWN_SENSE = "senseUNK";
    public static List<String> SENSES_FOR_UNK_PRED = Lists.getList(UNKNOWN_SENSE);
    public static final List<String> PRED_POSITION_STATE_NAMES = Lists.getList("_", UNKNOWN_SENSE);

    public Set<String> knownWords = new HashSet<String>();
    public Set<String> knownUnks = new HashSet<String>();
    public Set<String> knownPostags = new HashSet<String>();

    public Set<String> topNWords = new HashSet<String>();
    
    public List<String> linkStateNames;
    public List<String> roleStateNames;
    // Mapping from predicate form to the set of predicate senses.
    public Map<String,List<String>> predSenseListMap = new HashMap<String,List<String>>();

    public int maxSentLength = 0;

    public SrlBerkeleySignatureBuilder sig;
    public Normalizer normalize;

    public CorpusStatisticsPrm prm;
    private boolean initialized;
    
    public CorpusStatistics(CorpusStatisticsPrm prm) {
        this.prm = prm;
        this.normalize = new Normalizer(prm.normalizeWords);
        this.sig = new SrlBerkeleySignatureBuilder(new Alphabet<String>());
        initialized = false;
    }

    public void init(Iterable<SimpleAnnoSentence> cr) {
        Map<String,Set<String>> predSenseSetMap = new HashMap<String,Set<String>>();
        Set<String> knownRoles = new HashSet<String>();
        Set<String> knownLinks = new HashSet<String>();
        Map<String, MutableInt> words = new HashMap<String, MutableInt>();
        Map<String, MutableInt> unks = new HashMap<String, MutableInt>();
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
            
            // Word stats.
            for (int position = 0; position < sent.size(); position++) {
                String wordForm = sent.getWord(position);
                String cleanWord = normalize.clean(wordForm);
                // Actually only need to do this for those words that are below
                // threshold for knownWords.  
                String unkWord = sig.getSignature(wordForm, position, prm.language);
                unkWord = normalize.escape(unkWord);
                addWord(words, cleanWord);
                addWord(unks, unkWord);
            }
            
            // POS tag stats.
            if (sent.getPosTags() != null) {
                for (int position = 0; position < sent.size(); position++) {
                    knownPostags.add(sent.getPosTag(position));
                }
            }
            
            // SRL stats.
            if (sent.getSrlGraph() != null) {
                for (SrlEdge edge : sent.getSrlGraph().getEdges()) {
                    String role = edge.getLabel();
                    knownRoles.add(role);
                }
                for (SrlPred pred : sent.getSrlGraph().getPreds()) {
                    int position = pred.getPosition();
                    String lemma = sent.getLemma(position);
                    Set<String> senses = predSenseSetMap.get(lemma);
                    if (senses == null) {
                        senses = new TreeSet<String>();
                        predSenseSetMap.put(lemma, senses);
                    }
                    senses.add(pred.getLabel());
                }
            }
        }
        
        // For words and unknown word classes, we only keep those above some threshold.
        knownWords = getUnigramsAboveThreshold(words, prm.cutoff);
        knownUnks = getUnigramsAboveThreshold(unks, prm.cutoff);
                    
        topNWords = getTopNUnigrams(words, prm.topN, prm.cutoff);
        
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

    private static void addWord(Map<String, MutableInt> inputHash, String w) {
        MutableInt count = inputHash.get(w);
        if (count == null) {
            inputHash.put(w, new MutableInt());
        } else {
            count.increment();
        }
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

    private static Set<String> getTopNUnigrams(Map<String, MutableInt> map, int topN, int cutoff) {
        List<ComparablePair<Integer, String>> pairs = new ArrayList<ComparablePair<Integer, String>>(map.size());
        for (Entry<String, MutableInt> entry : map.entrySet()) {
            int count = entry.getValue().value;
            if (count > cutoff) {
                pairs.add(new ComparablePair<Integer, String>(count, entry.getKey()));
            }
        }
        Collections.sort(pairs, Collections.reverseOrder());
        HashSet<String> set = new HashSet<String>();
        for (Pair<Integer,String> p : pairs.subList(0, Math.min(pairs.size(), topN))) {
            set.add(p.get2());
        }
        return set;
    }
    
    @Override
    public String toString() {
        return "CorpusStatistics [" 
                + "\n     knownWords=" + knownWords 
                + ",\n     topNWords=" + topNWords
                + ",\n     knownUnks=" + knownUnks
                + ",\n     knownPostags=" + knownPostags 
                + ",\n     linkStateNames=" + linkStateNames
                + ",\n     roleStateNames=" + roleStateNames 
                + ",\n     maxSentLength=" + maxSentLength + "]";
    }

    public boolean isInitialized() {
        return initialized;
    }

    public String getLanguage() {
        return prm.language;
    }

}