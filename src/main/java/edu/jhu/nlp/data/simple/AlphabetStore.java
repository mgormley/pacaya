package edu.jhu.nlp.data.simple;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.jhu.util.Alphabet;
import edu.jhu.util.CountingAlphabet;
import edu.jhu.util.collections.Lists;

public class AlphabetStore implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(AlphabetStore.class);
    
    // Special Tokens.
    public static final int NUM_SPECIAL_TOKS = 4;
    public static final String TOK_UNK_STR = "TOK_UNK";
    public static final String TOK_START_STR = "TOK_START";
    public static final String TOK_END_STR = "TOK_END";
    public static final String TOK_WALL_STR = "TOK_WALL";
    public static final int TOK_UNK_INT = 0;
    public static final int TOK_START_INT = 1;
    public static final int TOK_END_INT = 2;
    public static final int TOK_WALL_INT = 3;
    public static String[] specialTokenStrs = new String[] { TOK_UNK_STR, TOK_START_STR, TOK_END_STR, TOK_WALL_STR};
    
    Alphabet<String> words;
    Alphabet<String> prefixes;
    Alphabet<String> lemmas;
    Alphabet<String> posTags;
    Alphabet<String> cposTags;
    Alphabet<String> clusters;
    Alphabet<String> feats;
    Alphabet<String> deprels;
    // TODO: 
    //Alphabet<String> lexAlphabet;
    //Alphabet<String> ntAlphabet;    
    private List<Alphabet<String>> as;
    
    public AlphabetStore(Iterable<AnnoSentence> sents) {
        words = getInitAlphabet("word", wordGetter, IntAnnoSentence.MAX_WORD, sents);
        prefixes = getInitAlphabet("prefix", prefixGetter, IntAnnoSentence.MAX_PREFIX, sents);
        lemmas = getInitAlphabet("lemma", lemmaGetter, IntAnnoSentence.MAX_LEMMA, sents);
        posTags = getInitAlphabet("pos", posTagGetter, IntAnnoSentence.MAX_POS, sents);
        cposTags = getInitAlphabet("cpos", cposTagGetter, IntAnnoSentence.MAX_CPOS, sents);
        clusters = getInitAlphabet("cluster", clusterGetter, IntAnnoSentence.MAX_CLUSTER, sents);
        feats = getInitAlphabet("feat", featGetter, IntAnnoSentence.MAX_FEAT, sents);
        deprels= getInitAlphabet("deprel", deprelGetter, IntAnnoSentence.MAX_DEPREL, sents);
        
        as = Lists.getList(words, prefixes, lemmas, posTags, cposTags, clusters, feats, deprels);
        this.stopGrowth();
    }

    private static Alphabet<String> getInitAlphabet(String name, StrGetter sg, int maxIdx, Iterable<AnnoSentence> sents) {
        CountingAlphabet<String> counter = new CountingAlphabet<>();
        for (AnnoSentence sent : sents) {
            List<String> strs = sg.getStrs(sent);
            if (strs != null) {
                for (String str : strs) {
                    counter.lookupIndex(str);
                }
            }
        }
        Alphabet<String> alphabet;
        for (int cutoff = 1; ; cutoff++) {
            alphabet = getInitAlphabet();
            for (int idx=0; idx<counter.size(); idx++) {
                String str = counter.lookupObject(idx);
                int count = counter.lookupObjectCount(idx);
                if (count >= cutoff) {
                    alphabet.lookupIndex(str);
                }
            }
            if (alphabet.size()-1 <= maxIdx) {
                log.info(String.format("For %s: Type count = %d Alphabet count = %d Cutoff = %d", 
                        name, counter.size(), alphabet.size(), cutoff));
                break;
            }
        }
        return alphabet;
    }
    
    private static Alphabet<String> getInitAlphabet() {
        Alphabet<String> alphabet = new Alphabet<String>();
        //for (SpecialToken tok : SpecialToken.values()) {
        for (int i=0; i<NUM_SPECIAL_TOKS; i++) {
            int idx = alphabet.lookupIndex(specialTokenStrs[i]);
            if (idx != i) {
                throw new RuntimeException("Expecting first index from alphabet to be 0");
            }
        }
        return alphabet;
    }

    public void startGrowth() {
        for (Alphabet<String> a : as) {
            a.startGrowth();
        }
    }
    
    public void stopGrowth() {
        for (Alphabet<String> a : as) {
            a.stopGrowth();
        }
    }
    
    static int safeLookup(Alphabet<String> alphabet, String tokStr) {
        int idx = alphabet.lookupIndex(tokStr);
        if (idx == -1) {
            idx = TOK_UNK_INT;
        }
        return idx;
    }
    
    public int getWordIdx(String word) {
        return safeLookup(words, word);
    }

    public int getPrefixIdx(String prefix) {
        return safeLookup(prefixes, prefix);
    }
    
    public int getLemmaIdx(String lemma) {
        return safeLookup(lemmas, lemma);
    }

    public int getPosTagIdx(String pos) {
        return safeLookup(posTags, pos);
    }

    public int getCposTagIdx(String cpos) {
        return safeLookup(cposTags, cpos);
    }

    public int getClusterIdx(String cluster) {
        return safeLookup(clusters, cluster);
    }

    public int getFeatIdx(String feat) {
        return safeLookup(feats, feat);
    }

    public int getDeprelIdx(String deprel) {
        return safeLookup(deprels, deprel);
    }
    

    private interface StrGetter extends Serializable {
        List<String> getStrs(AnnoSentence sent);
    }
    private StrGetter wordGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getWords(); }
    };
    private StrGetter prefixGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getPrefixes(); }
    };
    private StrGetter lemmaGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getLemmas(); }
    };
    private StrGetter posTagGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getPosTags(); }
    };
    private StrGetter cposTagGetter = new StrGetter() { 
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getCposTags(); }
    };
    private StrGetter clusterGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getClusters(); }
    };
    private StrGetter featGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) {
            if (sent.getFeats() == null) { return null; }
            ArrayList<String> strs = new ArrayList<>();
            for (List<String> featList : sent.getFeats()) {
                if (featList != null) {
                    strs.addAll(featList);
                }
            }
            return strs;
        }
    };
    private StrGetter deprelGetter = new StrGetter() {
        private static final long serialVersionUID = 1L;
        public List<String> getStrs(AnnoSentence sent) { return sent.getDeprels(); }
    };
    
}
